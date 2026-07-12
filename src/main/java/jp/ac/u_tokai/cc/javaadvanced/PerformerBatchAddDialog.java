package jp.ac.u_tokai.cc.javaadvanced;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

/** 同じ入力画面を閉じずに演者を続けて追加するダイアログです。 */
final class PerformerBatchAddDialog extends JDialog {

    private static final long serialVersionUID = 1L;
    private static final Color SUCCESS = new Color(0x248A3D);

    private final Consumer<String> performerAdder;
    private final JTextField performerNameField;
    private final JLabel feedbackLabel;
    private int addedCount;

    PerformerBatchAddDialog(Window owner, Consumer<String> performerAdder) {
        super(owner, "演者を追加", ModalityType.APPLICATION_MODAL);
        this.performerAdder = Objects.requireNonNull(performerAdder, "performerAdder must not be null");
        this.performerNameField = new JTextField(24);
        this.feedbackLabel = AppTheme.body("演者名を入力し、「追加」または Enter を押してください。");
        setupWindow(owner);
    }

    private void setupWindow(Window owner) {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setIconImage(AppIcon.create(64));
        setResizable(false);

        JPanel root = AppTheme.page(new BorderLayout(0, 18));
        root.setBorder(BorderFactory.createEmptyBorder(24, 26, 22, 26));
        setContentPane(root);

        JPanel introduction = transparentBoxPanel(BoxLayout.Y_AXIS);
        introduction.add(AppTheme.heading("演者を連続追加"));
        introduction.add(Box.createVerticalStrut(5));
        introduction.add(AppTheme.body("追加後も入力欄を閉じず、そのまま次の演者を登録できます。"));
        root.add(introduction, BorderLayout.NORTH);

        JPanel form = AppTheme.card(new BorderLayout(0, 10));
        form.setBorder(BorderFactory.createEmptyBorder(17, 18, 15, 18));
        form.add(AppTheme.fieldLabel("演者名"), BorderLayout.NORTH);

        performerNameField.setPreferredSize(new Dimension(300, 40));
        performerNameField.getAccessibleContext().setAccessibleName("追加する演者名");
        performerNameField.getAccessibleContext().setAccessibleDescription(
                "演者名を入力し、Enterで続けて追加できます。");

        JButton addButton = AppTheme.primaryButton("追加");
        addButton.addActionListener(event -> addCurrentPerformer());

        JPanel inputRow = new JPanel(new BorderLayout(10, 0));
        inputRow.setOpaque(false);
        inputRow.add(performerNameField, BorderLayout.CENTER);
        inputRow.add(addButton, BorderLayout.EAST);
        form.add(inputRow, BorderLayout.CENTER);

        feedbackLabel.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
        feedbackLabel.getAccessibleContext().setAccessibleName("演者追加の結果");
        form.add(feedbackLabel, BorderLayout.SOUTH);
        root.add(form, BorderLayout.CENTER);

        JButton doneButton = AppTheme.secondaryButton("完了");
        doneButton.addActionListener(event -> dispose());
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        footer.setOpaque(false);
        footer.add(doneButton);
        root.add(footer, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(addButton);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close-dialog");
        getRootPane().getActionMap().put("close-dialog", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                dispose();
            }
        });

        pack();
        setMinimumSize(new Dimension(520, getHeight()));
        setLocationRelativeTo(owner);
    }

    private void addCurrentPerformer() {
        String performerName = performerNameField.getText().strip();
        try {
            performerAdder.accept(performerName);
            addedCount++;
            feedbackLabel.setText(
                    "追加済み " + addedCount + "人 — 「" + performerName + "」を追加しました。");
            feedbackLabel.setForeground(SUCCESS);
            performerNameField.setText("");
        } catch (IllegalArgumentException exception) {
            String message = exception.getMessage();
            feedbackLabel.setText(message == null || message.isBlank()
                    ? "演者名を確認してください。"
                    : message);
            feedbackLabel.setForeground(AppTheme.DANGER);
            performerNameField.selectAll();
        }
        performerNameField.requestFocusInWindow();
    }

    private JPanel transparentBoxPanel(int axis) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, axis));
        panel.setOpaque(false);
        return panel;
    }
}
