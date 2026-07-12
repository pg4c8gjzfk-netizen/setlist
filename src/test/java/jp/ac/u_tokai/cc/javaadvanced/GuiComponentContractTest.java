package jp.ac.u_tokai.cc.javaadvanced;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

/** 実際のSwing部品を検査するGUI契約テストです。 */
public class GuiComponentContractTest {

    @BeforeClass
    public static void installApplicationTheme() {
        AppTheme.install();
    }

    @Test
    public void mainFrameEnablesEditOnlyAfterGeneratedProjectIsDisplayed() throws Exception {
        Assume.assumeFalse("画面表示できない環境ではSwing契約テストを実行しません。", GraphicsEnvironment.isHeadless());
        runOnEventDispatchThread(() -> {
            SetlistFrame frame = new SetlistFrame();
            try {
                assertEquals("Setlist Studio", frame.getTitle());
                JButton editButton = findButton(frame.getContentPane(), "編集");
                JButton startEditingButton = findButton(frame.getContentPane(), "編集を開始");
                JButton generateButton = findButton(frame.getContentPane(), "生成");
                JButton chooseInputButton = findButton(frame.getContentPane(), "XLSXを選択");
                JButton saveButton = findButton(frame.getContentPane(), "編集状態を保存");
                JButton exportButton = findButton(frame.getContentPane(), "配布用XLSX出力");
                assertNotNull(findButton(frame.getContentPane(), themeToggleLabel()));
                assertNotNull(editButton);
                assertNotNull(startEditingButton);
                assertNotNull(exportButton);
                assertNotNull(findComponent(
                        frame.getContentPane(), JLabel.class,
                        candidate -> "入力XLSX".equals(candidate.getText())));
                JTextField inputFileField = findComponent(frame.getContentPane(), JTextField.class);
                assertNotNull(inputFileField);
                assertFalse(inputFileField.isEditable());
                assertEquals("XLSXファイルが選択されていません", inputFileField.getText());
                assertNull(findComponent(frame.getContentPane(), JComboBox.class));
                assertFalse(editButton.isEnabled());
                assertFalse(startEditingButton.isEnabled());
                assertFalse(generateButton.isEnabled());
                assertFalse(saveButton.isEnabled());
                assertFalse(exportButton.isEnabled());
                assertTrue(chooseInputButton.isEnabled());
                assertEquals(JFrame.DO_NOTHING_ON_CLOSE, frame.getDefaultCloseOperation());
                assertNotNull(findComponent(
                        frame.getContentPane(), JLabel.class,
                        candidate -> "未作成".equals(candidate.getText())));
                assertNotNull(startEditingButton.getClientProperty(
                        com.formdev.flatlaf.FlatClientProperties.STYLE));
                assertShortcut(frame, KeyEvent.VK_N, "new-project");
                assertShortcut(frame, KeyEvent.VK_O, "open-project");
                assertShortcut(frame, KeyEvent.VK_S, "save-project");

                File inputFile = Files.createTempFile("selected-input-", ".xlsx").toFile();
                inputFile.deleteOnExit();
                frame.setSelectedInputFile(inputFile);

                assertEquals(inputFile.getName(), inputFileField.getText());
                assertEquals(inputFile.getAbsolutePath(), inputFileField.getToolTipText());
                assertTrue(startEditingButton.isEnabled());
                assertTrue(generateButton.isEnabled());

                frame.displayGeneratedProject(projectWithOneEntry());

                assertTrue(editButton.isEnabled());
                assertTrue(saveButton.isEnabled());
                assertTrue(exportButton.isEnabled());
                assertTrue(frame.hasUnsavedChanges());
                assertEquals("Setlist Studio *", frame.getTitle());
                assertNotNull(findComponent(
                        frame.getContentPane(), JLabel.class,
                        candidate -> "未保存の変更".equals(candidate.getText())));
            } finally {
                frame.dispose();
            }
        });
    }

    @Test
    public void mainFrameGuardsUnsavedChangesAndClearsDirtyStateAfterEditableSave() throws Exception {
        Assume.assumeFalse("画面表示できない環境ではSwing契約テストを実行しません。", GraphicsEnvironment.isHeadless());
        AtomicReference<UnsavedChangesPrompt.Decision> decision =
                new AtomicReference<>(UnsavedChangesPrompt.Decision.CANCEL);
        AtomicInteger promptCount = new AtomicInteger();
        runOnEventDispatchThread(() -> {
            SetlistFrame frame = new SetlistFrame((parent, actionName) -> {
                assertEquals("新しい香盤表を作成する", actionName);
                promptCount.incrementAndGet();
                return decision.get();
            });
            File output = Files.createTempFile("saved-editable-setlist-", ".xlsx").toFile();
            output.deleteOnExit();
            try {
                frame.displayGeneratedProject(projectWithOneEntry());

                assertFalse(frame.confirmProjectReplacement("新しい香盤表を作成する"));
                assertTrue(frame.hasUnsavedChanges());
                decision.set(UnsavedChangesPrompt.Decision.DISCARD);
                assertTrue(frame.confirmProjectReplacement("新しい香盤表を作成する"));
                assertEquals(2, promptCount.get());

                frame.writeEditableProject(output);

                assertFalse(frame.hasUnsavedChanges());
                assertEquals("Setlist Studio", frame.getTitle());
                assertNotNull(findComponent(
                        frame.getContentPane(), JLabel.class,
                        candidate -> "保存済み".equals(candidate.getText())));
                assertTrue(frame.confirmProjectReplacement("新しい香盤表を作成する"));
                assertEquals("保存済み状態では確認を表示しません。", 2, promptCount.get());
            } finally {
                frame.dispose();
            }
        });
    }

    @Test
    public void mainFrameExportsTheMostRecentlyDisplayedProject() throws Exception {
        Assume.assumeFalse("画面表示できない環境ではSwing契約テストを実行しません。", GraphicsEnvironment.isHeadless());
        runOnEventDispatchThread(() -> {
            SetlistFrame frame = new SetlistFrame();
            File output = Files.createTempFile("latest-setlist-project-", ".xlsx").toFile();
            output.deleteOnExit();
            try {
                frame.displayGeneratedProject(projectWithEntry("生成直後の演目", List.of("出演者A")));
                frame.displayGeneratedProject(projectWithEntry("編集後の演目", List.of("代理出演者")));

                frame.writeCurrentProject(output);

                try (Workbook workbook = new XSSFWorkbook(output)) {
                    Sheet sheet = workbook.getSheetAt(0);
                    assertEquals("編集後の演目", sheet.getRow(1).getCell(1).getStringCellValue());
                    int proxyPerformerColumn = findColumn(sheet, "代理出演者");
                    assertTrue(proxyPerformerColumn >= 3);
                    assertEquals("○", sheet.getRow(1).getCell(proxyPerformerColumn).getStringCellValue());
                }
            } finally {
                frame.dispose();
            }
        });
    }

    @Test
    public void editorShowsPerformerParticipationAsSeparateCircleColumns() throws Exception {
        Assume.assumeFalse("画面表示できない環境ではSwing契約テストを実行しません。", GraphicsEnvironment.isHeadless());
        AtomicReference<SetlistProject> changedProject = new AtomicReference<>();
        runOnEventDispatchThread(() -> {
            SetlistEditorFrame frame = new SetlistEditorFrame(projectWithOneEntry(), changedProject::set);
            File output = Files.createTempFile("editor-save-state-", ".xlsx").toFile();
            output.deleteOnExit();
            try {
                frame.setVisible(true);
                assertEquals(JFrame.DO_NOTHING_ON_CLOSE, frame.getDefaultCloseOperation());
                assertNull("画面を開いただけでは変更扱いにしません。", changedProject.get());
                assertFalse(frame.hasUnsavedChanges());
                JLabel saveStatus = findComponent(
                        frame.getContentPane(), JLabel.class,
                        candidate -> "変更なし".equals(candidate.getText()));
                assertNotNull(saveStatus);
                JTable table = findComponent(frame.getContentPane(), JTable.class);
                assertNotNull(table);
                assertEquals(6, table.getColumnCount());
                assertEquals("出演者A", table.getColumnName(3));
                assertEquals("出演者B", table.getColumnName(4));
                assertEquals("固定", table.getColumnName(5));
                assertFalse(hasColumnNamed(table, "出演者"));
                assertEquals(Boolean.TRUE, table.getValueAt(0, 3));
                assertEquals(Boolean.FALSE, table.getValueAt(0, 4));
                Component markedCell = table.getCellRenderer(0, 3)
                        .getTableCellRendererComponent(table, Boolean.TRUE, false, false, 0, 3);
                Component emptyCell = table.getCellRenderer(0, 4)
                        .getTableCellRendererComponent(table, Boolean.FALSE, false, false, 0, 4);
                assertEquals("◯", ((JLabel) markedCell).getText());
                assertEquals("", ((JLabel) emptyCell).getText());
                Component editingCell = table.getColumnModel().getColumn(4).getCellEditor()
                        .getTableCellEditorComponent(table, Boolean.FALSE, true, 0, 4);
                assertEquals("◯", ((JLabel) editingCell).getText());
                assertEquals(Boolean.TRUE,
                        table.getColumnModel().getColumn(4).getCellEditor().getCellEditorValue());
                assertNotNull(findButton(frame.getContentPane(), "演者を追加"));
                assertNotNull(findButton(frame.getContentPane(), "演者を削除"));
                assertNotNull(findButton(frame.getContentPane(), "先頭へ"));
                assertNotNull(findButton(frame.getContentPane(), "末尾へ"));
                assertNotNull(findButton(frame.getContentPane(), "指定順へ"));
                assertTrue(findButton(frame.getContentPane(), "別の公演へ").isEnabled());
                assertNotNull(findButton(frame.getContentPane(), "再生成"));
                assertNotNull(findButton(frame.getContentPane(), themeToggleLabel()));
                assertTrue(frame.getWidth() >= 1000);
                assertShortcut(frame, KeyEvent.VK_S, "save-project");
                assertShortcut(frame, KeyEvent.VK_R, "regenerate-project");
                for (String buttonText : List.of("演目を削除", "再生成", "XLSX保存", "閉じる")) {
                    assertButtonFullyVisible(frame, buttonText);
                }

                table.setValueAt("変更後の演目", 0, 1);
                assertTrue(frame.hasUnsavedChanges());
                assertNotNull(changedProject.get());
                assertEquals("香盤表を編集 *", frame.getTitle());
                assertEquals("未保存の変更", saveStatus.getText());

                frame.writeEditableProject(output);

                assertFalse(frame.hasUnsavedChanges());
                assertEquals("香盤表を編集", frame.getTitle());
                assertEquals("変更なし", saveStatus.getText());
            } finally {
                frame.dispose();
            }
        });
    }

    @Test
    public void editorPreventsImportedXlsxSheetBoundaryChanges() throws Exception {
        Assume.assumeFalse("画面表示できない環境ではSwing契約テストを実行しません。", GraphicsEnvironment.isHeadless());
        runOnEventDispatchThread(() -> {
            SetlistProject unlocked = projectWithOneEntry();
            SetlistProject locked = new SetlistProject(unlocked.sessions(), true);
            SetlistEditorFrame frame = new SetlistEditorFrame(locked, project -> {
            });
            try {
                frame.setVisible(true);
                assertFalse(findButton(frame.getContentPane(), "公演を追加").isEnabled());
                assertFalse(findButton(frame.getContentPane(), "公演を削除").isEnabled());
                assertFalse(findButton(frame.getContentPane(), "別の公演へ").isEnabled());
                JLabel status = findComponent(
                        frame.getContentPane(), JLabel.class,
                        candidate -> "シート境界を保持".equals(candidate.getText()));
                assertNotNull(status);
                assertTrue(status.isVisible());
                assertTrue(frame.currentProject().sheetBoundariesLocked());
            } finally {
                frame.dispose();
            }
        });
    }

    @Test
    public void editorCommitsTheActiveCellBeforeClosing() throws Exception {
        Assume.assumeFalse("画面表示できない環境ではSwing契約テストを実行しません。", GraphicsEnvironment.isHeadless());
        AtomicReference<SetlistProject> changedProject = new AtomicReference<>();
        runOnEventDispatchThread(() -> {
            SetlistEditorFrame frame = new SetlistEditorFrame(projectWithOneEntry(), changedProject::set);
            try {
                frame.setVisible(true);
                JTable table = findComponent(frame.getContentPane(), JTable.class);
                assertNotNull(table);
                assertTrue(table.editCellAt(0, 1));
                JTextField editor = (JTextField) table.getEditorComponent();
                editor.setText("閉じる直前の変更");

                frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));

                assertNotNull(changedProject.get());
                assertEquals(
                        "閉じる直前の変更",
                        changedProject.get().sessions().getFirst().entries().getFirst().title());
                assertFalse(frame.isDisplayable());
            } finally {
                frame.dispose();
            }
        });
    }

    private SetlistProject projectWithOneEntry() {
        return projectWithEntry("確認用演目", List.of("出演者A"));
    }

    private SetlistProject projectWithEntry(String title, List<String> performers) {
        SetlistEntry entry = new SetlistEntry(
                UUID.randomUUID(), UUID.randomUUID(), title, 180,
                performers, false, FixedPosition.NONE, -1);
        return new SetlistProject(List.of(new SetlistSession(
                "第1公演", List.of(entry), List.of("出演者A", "出演者B"))));
    }

    private boolean hasColumnNamed(JTable table, String columnName) {
        for (int column = 0; column < table.getColumnCount(); column++) {
            if (columnName.equals(table.getColumnName(column))) {
                return true;
            }
        }
        return false;
    }

    private String themeToggleLabel() {
        return AppTheme.isDark() ? "ライト表示" : "ダーク表示";
    }

    private int findColumn(Sheet sheet, String headerText) {
        for (int column = 0; column < sheet.getRow(0).getLastCellNum(); column++) {
            if (headerText.equals(sheet.getRow(0).getCell(column).getStringCellValue())) {
                return column;
            }
        }
        return -1;
    }

    private JButton findButton(Container root, String text) {
        JButton button = findComponent(root, JButton.class, candidate -> text.equals(candidate.getText()));
        assertNotNull("ボタンが見つかりません: " + text, button);
        return button;
    }

    private void assertButtonFullyVisible(SetlistEditorFrame frame, String text) {
        JButton button = findButton(frame.getContentPane(), text);
        Rectangle bounds = SwingUtilities.convertRectangle(
                button.getParent(), button.getBounds(), frame.getContentPane());
        Rectangle visibleArea = new Rectangle(
                0, 0, frame.getContentPane().getWidth(), frame.getContentPane().getHeight());
        assertTrue("ボタンが画面内に収まっていません: " + text, visibleArea.contains(bounds));
    }

    private void assertShortcut(JFrame frame, int keyCode, String expectedActionName) {
        int menuMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        Object actionName = frame.getRootPane()
                .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .get(KeyStroke.getKeyStroke(keyCode, menuMask));
        assertEquals(expectedActionName, actionName);
    }

    private <T extends Component> T findComponent(Container root, Class<T> type) {
        return findComponent(root, type, candidate -> true);
    }

    private <T extends Component> T findComponent(
            Container root, Class<T> type, java.util.function.Predicate<T> condition) {
        for (Component component : root.getComponents()) {
            if (type.isInstance(component) && condition.test(type.cast(component))) {
                return type.cast(component);
            }
            if (component instanceof Container container) {
                T found = findComponent(container, type, condition);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private void runOnEventDispatchThread(ThrowingRunnable runnable) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
            return;
        }
        AtomicReference<Throwable> failure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                runnable.run();
            } catch (Throwable throwable) {
                failure.set(throwable);
            }
        });
        if (failure.get() != null) {
            if (failure.get() instanceof Exception exception) {
                throw exception;
            }
            throw new AssertionError(failure.get());
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
