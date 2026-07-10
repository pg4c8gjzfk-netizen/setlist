package jp.ac.u_tokai.cc.javaadvanced;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import org.junit.Assume;
import org.junit.Test;

/** 実際のSwing部品を検査するGUI契約テストです。 */
public class GuiComponentContractTest {

    @Test
    public void mainFrameEnablesEditOnlyAfterGeneratedProjectIsDisplayed() throws Exception {
        Assume.assumeFalse("画面表示できない環境ではSwing契約テストを実行しません。", GraphicsEnvironment.isHeadless());
        runOnEventDispatchThread(() -> {
            SetlistFrame frame = new SetlistFrame();
            try {
                JButton editButton = findButton(frame.getContentPane(), "編集");
                JButton startEditingButton = findButton(frame.getContentPane(), "編集を開始");
                assertNotNull(editButton);
                assertNotNull(startEditingButton);
                assertFalse(editButton.isEnabled());

                frame.displayGeneratedProject(projectWithOneEntry());

                assertTrue(editButton.isEnabled());
            } finally {
                frame.dispose();
            }
        });
    }

    @Test
    public void editorUsesCheckboxBasedFixedColumnAndShowsMoveControls() throws Exception {
        Assume.assumeFalse("画面表示できない環境ではSwing契約テストを実行しません。", GraphicsEnvironment.isHeadless());
        runOnEventDispatchThread(() -> {
            SetlistEditorFrame frame = new SetlistEditorFrame(projectWithOneEntry(), project -> {
            });
            try {
                frame.setVisible(true);
                JTable table = findComponent(frame.getContentPane(), JTable.class);
                assertNotNull(table);
                assertEquals(5, table.getColumnCount());
                assertEquals("固定", table.getColumnName(4));
                assertNotNull(findButton(frame.getContentPane(), "先頭へ"));
                assertNotNull(findButton(frame.getContentPane(), "末尾へ"));
                assertNotNull(findButton(frame.getContentPane(), "指定順へ"));
                assertNotNull(findButton(frame.getContentPane(), "別の公演へ"));
                assertNotNull(findButton(frame.getContentPane(), "再生成"));
                for (String buttonText : List.of("演目を削除", "再生成", "XLSX保存", "閉じる")) {
                    assertButtonFullyVisible(frame, buttonText);
                }
            } finally {
                frame.dispose();
            }
        });
    }

    private SetlistProject projectWithOneEntry() {
        SetlistEntry entry = new SetlistEntry(
                UUID.randomUUID(), UUID.randomUUID(), "確認用演目", 180,
                List.of("出演者"), false, FixedPosition.NONE, -1);
        return new SetlistProject(List.of(new SetlistSession("第1公演", List.of(entry))));
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
