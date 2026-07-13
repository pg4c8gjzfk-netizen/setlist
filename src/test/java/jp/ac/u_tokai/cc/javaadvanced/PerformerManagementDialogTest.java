package jp.ac.u_tokai.cc.javaadvanced;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

/** 演者一覧で追加・名前変更・削除できる統合画面のGUI契約テストです。 */
public class PerformerManagementDialogTest {

    @BeforeClass
    public static void installApplicationTheme() {
        AppTheme.install();
    }

    @Test
    public void managesPerformersWithoutLeavingTheDialog() throws Exception {
        Assume.assumeFalse(
                "画面表示できない環境ではSwing契約テストを実行しません。",
                GraphicsEnvironment.isHeadless());

        runOnEventDispatchThread(() -> {
            SetlistEntry entry = new SetlistEntry(
                    UUID.randomUUID(), UUID.randomUUID(), "確認用演目", 180,
                    List.of("出演者A"), false, FixedPosition.NONE, -1);
            SetlistEntryTableModel model = new SetlistEntryTableModel(
                    List.of(entry), List.of("出演者A", "出演者B"));
            PerformerManagementDialog dialog = new PerformerManagementDialog(null, "第1公演", model);
            try {
                JList<?> performerList = findComponent(dialog.getContentPane(), JList.class);
                JTextField input = findComponent(dialog.getContentPane(), JTextField.class);
                JButton addButton = findButton(dialog.getContentPane(), "追加");
                JButton renameButton = findButton(dialog.getContentPane(), "名前を変更");
                JButton removeButton = findButton(dialog.getContentPane(), "削除");
                JButton doneButton = findButton(dialog.getContentPane(), "完了");

                assertNotNull(performerList);
                assertNotNull(input);
                assertEquals(2, performerList.getModel().getSize());
                assertFalse(renameButton.isEnabled());
                assertFalse(removeButton.isEnabled());

                input.setText("出演者C");
                addButton.doClick();
                assertEquals(List.of("出演者A", "出演者B", "出演者C"), model.performerNames());
                assertTrue(dialog.isDisplayable());

                performerList.setSelectedValue("出演者A", true);
                assertTrue(renameButton.isEnabled());
                assertTrue(removeButton.isEnabled());
                input.setText("出演者A 改名後");
                renameButton.doClick();
                assertEquals(List.of("出演者A 改名後", "出演者B", "出演者C"), model.performerNames());
                assertEquals(List.of("出演者A 改名後"), model.entries().getFirst().performers());

                performerList.setSelectedValue("出演者C", true);
                removeButton.doClick();
                assertEquals(List.of("出演者A 改名後", "出演者B"), model.performerNames());

                performerList.setSelectedValue("出演者A 改名後", true);
                removeButton.doClick();
                assertEquals(List.of("出演者A 改名後", "出演者B"), model.performerNames());
                assertNotNull(findComponent(
                        dialog.getContentPane(), JLabel.class,
                        label -> label.getText().contains("出演中")));

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
