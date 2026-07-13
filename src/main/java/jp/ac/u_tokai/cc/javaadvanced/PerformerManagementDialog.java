package jp.ac.u_tokai.cc.javaadvanced;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;

/** 選択中の公演に登録された演者を一覧で追加・名前変更・削除します。 */
final class PerformerManagementDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final SetlistEntryTableModel tableModel;
    private final DefaultListModel<String> performerListModel;
    private final JList<String> performerList;
    private final JTextField performerNameField;
    private final JLabel feedbackLabel;
    private final JButton renameButton;
    private final JButton removeButton;

    PerformerManagementDialog(
            Window owner,
            String sessionName,
            SetlistEntryTableModel tableModel) {
        super(owner, "演者を編集", ModalityType.APPLICATION_MODAL);
        this.tableModel = Objects.requireNonNull(tableModel, "tableModel must not be null");
        this.performerListModel = new DefaultListModel<>();
        this.performerList = new JList<>(performerListModel);
        this.performerNameField = new JTextField(24);
        this.feedbackLabel = AppTheme.body("演者を選ぶと、名前変更と削除ができます。");
        this.renameButton = AppTheme.secondaryButton("名前を変更");
        this.removeButton = AppTheme.quietButton("削除");
        setupWindow(owner, normalizeSessionName(sessionName));
        refreshPerformerList(null);
    }

    private void setupWindow(Window owner, String sessionName) {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setIconImage(AppIcon.create(64));
        setResizable(false);

        JPanel root = AppTheme.page(new BorderLayout(0, 18));
        root.setBorder(BorderFactory.createEmptyBorder(24, 26, 22, 26));
        setContentPane(root);

        JPanel introduction = transparentBoxPanel(BoxLayout.Y_AXIS);
        introduction.add(AppTheme.heading(sessionName + "の演者"));
        introduction.add(Box.createVerticalStrut(5));
        introduction.add(AppTheme.body("この公演の演者を一覧で確認し、追加・名前変更・削除できます。"));
        root.add(introduction, BorderLayout.NORTH);

        JPanel content = AppTheme.card(new BorderLayout(16, 0));
        content.setBorder(BorderFactory.createEmptyBorder(17, 18, 15, 18));
        content.add(createPerformerListPanel(), BorderLayout.WEST);
        content.add(createEditPanel(), BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);

        JButton doneButton = AppTheme.secondaryButton("完了");
        doneButton.addActionListener(event -> dispose());
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        footer.setOpaque(false);
        footer.add(doneButton);
        root.add(footer, BorderLayout.SOUTH);

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close-dialog");
        getRootPane().getActionMap().put("close-dialog", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                dispose();
            }
        });

        pack();
        setMinimumSize(new Dimension(690, 410));
        setLocationRelativeTo(owner);
    }

    private JPanel createPerformerListPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        panel.add(AppTheme.fieldLabel("登録済みの演者"), BorderLayout.NORTH);

        performerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        performerList.setVisibleRowCount(8);
        performerList.setFixedCellWidth(220);
        performerList.getAccessibleContext().setAccessibleName("登録済みの演者一覧");
        performerList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                updateSelection();
            }
        });
        JScrollPane scroll = AppTheme.scroll(performerList);
        scroll.setPreferredSize(new Dimension(240, 235));
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createEditPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JPanel form = transparentBoxPanel(BoxLayout.Y_AXIS);
        JLabel fieldLabel = AppTheme.fieldLabel("演者名");
        fieldLabel.setAlignmentX(LEFT_ALIGNMENT);
        form.add(fieldLabel);
        form.add(Box.createVerticalStrut(8));

        performerNameField.setPreferredSize(new Dimension(300, 40));
        performerNameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        performerNameField.setAlignmentX(LEFT_ALIGNMENT);
        performerNameField.getAccessibleContext().setAccessibleName("編集する演者名");
        performerNameField.getAccessibleContext().setAccessibleDescription(
                "新しい演者を追加するか、選択した演者の名前を変更します。");
        form.add(performerNameField);
        form.add(Box.createVerticalStrut(12));

        JButton addButton = AppTheme.primaryButton("追加");
        addButton.addActionListener(event -> addPerformer());
        renameButton.addActionListener(event -> renamePerformer());
        removeButton.addActionListener(event -> removePerformer());
        AppTheme.styleDanger(removeButton);
        renameButton.setEnabled(false);
        removeButton.setEnabled(false);

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 0));
        actionRow.setOpaque(false);
        actionRow.add(addButton);
        actionRow.add(renameButton);
        actionRow.add(removeButton);

        actionRow.setAlignmentX(LEFT_ALIGNMENT);
        form.add(actionRow);
        form.add(Box.createVerticalStrut(10));
        feedbackLabel.setAlignmentX(LEFT_ALIGNMENT);
        feedbackLabel.getAccessibleContext().setAccessibleName("演者編集の結果");
        form.add(feedbackLabel);
        panel.add(form, BorderLayout.NORTH);
        return panel;
    }

    private void addPerformer() {
        String performerName = performerNameField.getText().strip();
        try {
            tableModel.addPerformer(performerName);
            refreshPerformerList(performerName);
            showSuccess("「" + performerName + "」を追加しました。");
        } catch (IllegalArgumentException exception) {
            showError(exception);
        }
    }

    private void renamePerformer() {
        String currentName = performerList.getSelectedValue();
        if (currentName == null) {
            showError("名前を変更する演者を選んでください。");
            return;
        }
        String newName = performerNameField.getText().strip();
        try {
            tableModel.renamePerformer(currentName, newName);
            refreshPerformerList(newName);
            showSuccess("演者名を変更しました。");
        } catch (IllegalArgumentException exception) {
            showError(exception);
        }
    }

    private void removePerformer() {
        String performerName = performerList.getSelectedValue();
        if (performerName == null) {
            showError("削除する演者を選んでください。");
            return;
        }
        try {
            tableModel.removePerformer(performerName);
            refreshPerformerList(null);
            performerNameField.setText("");
            showSuccess("「" + performerName + "」を削除しました。");
        } catch (IllegalArgumentException exception) {
            showError(exception);
        }
    }

    private void refreshPerformerList(String selectedName) {
        performerListModel.clear();
        tableModel.performerNames().forEach(performerListModel::addElement);
        if (selectedName != null) {
            performerList.setSelectedValue(selectedName, true);
        } else {
            performerList.clearSelection();
            updateSelection();
        }
    }

    private void updateSelection() {
        String selectedName = performerList.getSelectedValue();
        boolean hasSelection = selectedName != null;
        renameButton.setEnabled(hasSelection);
        removeButton.setEnabled(hasSelection);
        if (hasSelection) {
            performerNameField.setText(selectedName);
            performerNameField.selectAll();
        }
    }

    private void showSuccess(String message) {
        feedbackLabel.setText(message);
        feedbackLabel.setForeground(AppTheme.SUCCESS);
        performerNameField.requestFocusInWindow();
    }

    private void showError(IllegalArgumentException exception) {
        String message = exception.getMessage();
        showError(message == null || message.isBlank() ? "演者名を確認してください。" : message);
    }

    private void showError(String message) {
        feedbackLabel.setText(message);
        feedbackLabel.setForeground(AppTheme.DANGER);
        performerNameField.requestFocusInWindow();
        performerNameField.selectAll();
    }

    private String normalizeSessionName(String sessionName) {
        return sessionName == null || sessionName.isBlank() ? "選択中の公演" : sessionName.strip();
    }

    private JPanel transparentBoxPanel(int axis) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, axis));
        panel.setOpaque(false);
        return panel;
    }
}
