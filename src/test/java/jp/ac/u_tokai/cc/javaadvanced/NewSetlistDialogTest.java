package jp.ac.u_tokai.cc.javaadvanced;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

/** 新規作成設定ダイアログのGUI契約テストです。 */
public class NewSetlistDialogTest {

    @BeforeClass
    public static void installApplicationTheme() {
        AppTheme.install();
    }

    @Test
    public void requiresFileNameAndReturnsNormalizedSettings() throws Exception {
        Assume.assumeFalse(
                "画面表示できない環境ではSwing契約テストを実行しません。",
                GraphicsEnvironment.isHeadless());

        runOnEventDispatchThread(() -> {
            NewSetlistDialog dialog = new NewSetlistDialog(null);
            try {
                JTextField fileNameField = findComponent(dialog.getContentPane(), JTextField.class);
                JSpinner performanceCountSpinner = findComponent(dialog.getContentPane(), JSpinner.class);
                JButton createButton = findButton(dialog.getContentPane(), "作成して編集");
                JButton cancelButton = findButton(dialog.getContentPane(), "キャンセル");

                assertNotNull(fileNameField);
                assertNotNull(performanceCountSpinner);
                assertNotNull(cancelButton);
                assertEquals("", fileNameField.getText());
                assertEquals(1, performanceCountSpinner.getValue());

                createButton.doClick();
                assertTrue(dialog.isDisplayable());
                assertTrue(dialog.selectedSettings().isEmpty());
                assertNotNull(findComponent(
                        dialog.getContentPane(), JLabel.class,
                        label -> label.getText().contains("ファイル名を入力")));

                fileNameField.setText("夏公演");
                assertNotNull(findComponent(
                        dialog.getContentPane(), JLabel.class,
                        label -> "ファイル名と公演数は必須です。".equals(label.getText())));
                ((JSpinner.DefaultEditor) performanceCountSpinner.getEditor())
                        .getTextField().setText("0");
                createButton.doClick();
                assertTrue(dialog.isDisplayable());
                assertTrue(dialog.selectedSettings().isEmpty());
                assertNotNull(findComponent(
                        dialog.getContentPane(), JLabel.class,
                        label -> label.getText().contains("公演数は1〜30")));

                performanceCountSpinner.setValue(4);
                assertNotNull(findComponent(
                        dialog.getContentPane(), JLabel.class,
                        label -> "第1公演〜第4公演の4公演を作成します。".equals(label.getText())));
                createButton.doClick();

                assertFalse(dialog.isDisplayable());
                assertEquals(
                        new NewSetlistSettings("夏公演.xlsx", 4),
                        dialog.selectedSettings().orElseThrow());
            } finally {
                dialog.dispose();
            }
        });
    }

    private JButton findButton(Container root, String text) {
        JButton button = findComponent(root, JButton.class, candidate -> text.equals(candidate.getText()));
        assertNotNull("ボタンが見つかりません: " + text, button);
        return button;
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
