package jp.ac.u_tokai.cc.javaadvanced;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

/** 演者を連続追加する入力フローのGUI契約テストです。 */
public class PerformerBatchAddDialogTest {

    @BeforeClass
    public static void installApplicationTheme() {
        AppTheme.install();
    }

    @Test
    public void keepsDialogOpenWhileAddingPerformersConsecutively() throws Exception {
        Assume.assumeFalse(
                "画面表示できない環境ではSwing契約テストを実行しません。",
                GraphicsEnvironment.isHeadless());

        runOnEventDispatchThread(() -> {
            SetlistEntryTableModel model = new SetlistEntryTableModel(List.of(), List.of("出演者A"));
            PerformerBatchAddDialog dialog = new PerformerBatchAddDialog(null, model::addPerformer);
            try {
                JTextField input = findComponent(dialog.getContentPane(), JTextField.class);
                JButton addButton = findButton(dialog.getContentPane(), "追加");
                JButton doneButton = findButton(dialog.getContentPane(), "完了");

                assertNotNull(input);
                assertSame(addButton, dialog.getRootPane().getDefaultButton());
                assertTrue(dialog.isDisplayable());

                input.setText("出演者B");
                addButton.doClick();
                assertEquals(List.of("出演者A", "出演者B"), model.performerNames());
                assertEquals("", input.getText());
                assertTrue(dialog.isDisplayable());

                input.setText("出演者C");
                dialog.getRootPane().getDefaultButton().doClick();
                assertEquals(List.of("出演者A", "出演者B", "出演者C"), model.performerNames());
                assertEquals("", input.getText());
                assertNotNull(findComponent(
                        dialog.getContentPane(), JLabel.class,
                        label -> label.getText().contains("追加済み 2人")));

                input.setText("出演者C");
                addButton.doClick();
                assertEquals(List.of("出演者A", "出演者B", "出演者C"), model.performerNames());
                assertEquals("出演者C", input.getText());
                assertNotNull(findComponent(
                        dialog.getContentPane(), JLabel.class,
                        label -> label.getText().contains("すでに登録されています")));

                doneButton.doClick();
                assertFalse(dialog.isDisplayable());
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
