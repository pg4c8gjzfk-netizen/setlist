package jp.ac.u_tokai.cc.javaadvanced;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.ParseException;
import java.util.Optional;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** ファイル名と公演数を必ず確認してから、新規香盤表を作成します。 */
final class NewSetlistDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final JTextField fileNameField;
    private final JSpinner performanceCountSpinner;
    private final JLabel sessionSummaryLabel;
    private final JLabel feedbackLabel;
    private NewSetlistSettings result;

    NewSetlistDialog(Window owner) {
        super(owner, "新しい香盤表を作成", ModalityType.APPLICATION_MODAL);
        this.fileNameField = new JTextField(26);
        this.performanceCountSpinner = new JSpinner(new SpinnerNumberModel(
                NewSetlistSettings.MIN_PERFORMANCE_COUNT,
                NewSetlistSettings.MIN_PERFORMANCE_COUNT,
                NewSetlistSettings.MAX_PERFORMANCE_COUNT,
                1));
        this.sessionSummaryLabel = AppTheme.body("");
        this.feedbackLabel = AppTheme.body("ファイル名と公演数は必須です。");
        setupWindow(owner);
        updateSessionSummary();
    }

    Optional<NewSetlistSettings> showDialog() {
        setVisible(true);
        return selectedSettings();
    }

    Optional<NewSetlistSettings> selectedSettings() {
        return Optional.ofNullable(result);
    }

    private void setupWindow(Window owner) {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setIconImage(AppIcon.create(64));
        setResizable(false);

        JPanel root = AppTheme.page(new BorderLayout(0, 18));
        root.setBorder(BorderFactory.createEmptyBorder(24, 26, 22, 26));
        setContentPane(root);

        JPanel introduction = transparentBoxPanel(BoxLayout.Y_AXIS);
        introduction.add(AppTheme.heading("新しい香盤表の設定"));
        introduction.add(Box.createVerticalStrut(5));
        introduction.add(AppTheme.body("作成後の保存名と、必要な公演数を先に決めます。"));
        root.add(introduction, BorderLayout.NORTH);

        JPanel form = AppTheme.card(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(18, 20, 16, 20));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 8, 0);
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        form.add(AppTheme.fieldLabel("ファイル名（必須）"), constraints);

        constraints.gridy++;
        constraints.insets = new Insets(0, 0, 16, 0);
        fileNameField.setPreferredSize(new Dimension(430, 40));
        fileNameField.putClientProperty(
                FlatClientProperties.PLACEHOLDER_TEXT, "例: 2026年夏公演.xlsx");
        fileNameField.getAccessibleContext().setAccessibleName("新規香盤表のファイル名");
        fileNameField.getAccessibleContext().setAccessibleDescription(
                "拡張子を省略した場合は.xlsxを自動で追加します。");
        fileNameField.getDocument().addDocumentListener(inputChangeListener());
        form.add(fileNameField, constraints);

        constraints.gridy++;
        constraints.insets = new Insets(0, 0, 8, 0);
        form.add(AppTheme.fieldLabel("公演数（必須・1〜30）"), constraints);

        constraints.gridy++;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 8, 0);
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(performanceCountSpinner, "0");
        performanceCountSpinner.setEditor(editor);
        performanceCountSpinner.setPreferredSize(new Dimension(120, 40));
        performanceCountSpinner.getAccessibleContext().setAccessibleName("新規香盤表の公演数");
        performanceCountSpinner.addChangeListener(event -> {
            updateSessionSummary();
            resetFeedback();
        });
        editor.getTextField().getDocument().addDocumentListener(inputChangeListener());
        form.add(performanceCountSpinner, constraints);

        constraints.gridy++;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 8, 0);
        form.add(sessionSummaryLabel, constraints);

        constraints.gridy++;
        constraints.insets = new Insets(0, 0, 0, 0);
        feedbackLabel.getAccessibleContext().setAccessibleName("新規作成設定の入力結果");
        form.add(feedbackLabel, constraints);
        root.add(form, BorderLayout.CENTER);

        JButton cancelButton = AppTheme.quietButton("キャンセル");
        JButton createButton = AppTheme.primaryButton("作成して編集");
        cancelButton.addActionListener(event -> dispose());
        createButton.addActionListener(event -> createProject());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(cancelButton);
        actions.add(createButton);
        root.add(actions, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(createButton);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel-new-setlist");
        getRootPane().getActionMap().put("cancel-new-setlist", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                dispose();
            }
        });
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent event) {
                fileNameField.requestFocusInWindow();
            }
        });

        pack();
        setMinimumSize(new Dimension(570, 390));
        if (getWidth() < 570 || getHeight() < 390) {
            setSize(Math.max(getWidth(), 570), Math.max(getHeight(), 390));
        }
        setLocationRelativeTo(owner);
    }

    private void createProject() {
        try {
            performanceCountSpinner.commitEdit();
        } catch (ParseException | IllegalArgumentException exception) {
            feedbackLabel.setText("公演数は1〜30の整数で入力してください。");
            feedbackLabel.setForeground(AppTheme.DANGER);
            performanceCountSpinner.requestFocusInWindow();
            return;
        }
        try {
            result = new NewSetlistSettings(
                    fileNameField.getText(),
                    ((Number) performanceCountSpinner.getValue()).intValue());
            dispose();
        } catch (IllegalArgumentException exception) {
            feedbackLabel.setText(exception.getMessage());
            feedbackLabel.setForeground(AppTheme.DANGER);
            fileNameField.requestFocusInWindow();
            fileNameField.selectAll();
        }
    }

    private void updateSessionSummary() {
        int count = ((Number) performanceCountSpinner.getValue()).intValue();
        if (count == 1) {
            sessionSummaryLabel.setText("第1公演を作成します。");
            return;
        }
        sessionSummaryLabel.setText(
                "第1公演〜第" + count + "公演の" + count + "公演を作成します。");
    }

    private DocumentListener inputChangeListener() {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                resetFeedback();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                resetFeedback();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                resetFeedback();
            }
        };
    }

    private void resetFeedback() {
        feedbackLabel.setText("ファイル名と公演数は必須です。");
        feedbackLabel.setForeground(AppTheme.TEXT_SECONDARY);
    }

    private JPanel transparentBoxPanel(int axis) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, axis));
        panel.setOpaque(false);
        return panel;
    }
}
