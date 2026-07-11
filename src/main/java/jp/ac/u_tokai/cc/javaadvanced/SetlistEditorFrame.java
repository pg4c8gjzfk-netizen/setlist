package jp.ac.u_tokai.cc.javaadvanced;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileNameExtensionFilter;

/** 公演ごとの演目を直接編集し、再生成できる画面です。 */
public final class SetlistEditorFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private final JTabbedPane sessionTabs;
    private final List<SetlistEntryTableModel> tableModels;
    private final List<JTable> tables;
    private final Consumer<SetlistProject> projectChangedHandler;
    private boolean rebuilding;

    public SetlistEditorFrame(SetlistProject project, Consumer<SetlistProject> projectChangedHandler) {
        super("香盤表を編集");
        this.sessionTabs = new JTabbedPane();
        this.tableModels = new ArrayList<>();
        this.tables = new ArrayList<>();
        this.projectChangedHandler = Objects.requireNonNull(projectChangedHandler, "projectChangedHandler must not be null");
        setupWindow();
        setProject(Objects.requireNonNull(project, "project must not be null"));
    }

    /** 現在の編集内容を取得します。 */
    public SetlistProject currentProject() {
        List<SetlistSession> sessions = new ArrayList<>();
        for (int index = 0; index < tableModels.size(); index++) {
            sessions.add(new SetlistSession(sessionTabs.getTitleAt(index), tableModels.get(index).entries()));
        }
        return new SetlistProject(sessions);
    }

    private void setupWindow() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setIconImage(AppIcon.create(64));
        JPanel root = AppTheme.page(new BorderLayout(0, 16));
        root.setBorder(AppTheme.pagePadding());
        setContentPane(root);

        sessionTabs.putClientProperty(
                com.formdev.flatlaf.FlatClientProperties.STYLE,
                "tabArc: 12; tabHeight: 40; selectedBackground: #FFFFFF; hoverColor: #ECECF0");

        root.add(createHeaderPanel(), BorderLayout.NORTH);
        JPanel editorCard = AppTheme.card(new BorderLayout());
        editorCard.add(sessionTabs, BorderLayout.CENTER);
        root.add(editorCard, BorderLayout.CENTER);
        root.add(createActionPanel(), BorderLayout.SOUTH);

        AppTheme.bindMenuShortcut(getRootPane(), "save-project", KeyEvent.VK_S, this::saveProject);
        AppTheme.bindMenuShortcut(getRootPane(), "regenerate-project", KeyEvent.VK_R, this::regenerate);
        AppTheme.bindMenuShortcut(getRootPane(), "close-editor", KeyEvent.VK_W, this::dispose);

        setMinimumSize(new Dimension(1000, 650));
        setSize(1180, 760);
        setLocationRelativeTo(null);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = AppTheme.page(new BorderLayout(20, 0));
        JPanel titleStack = transparentBoxPanel(BoxLayout.Y_AXIS);
        titleStack.add(AppTheme.title("香盤表を編集"));
        titleStack.add(Box.createVerticalStrut(5));
        titleStack.add(AppTheme.body("曲順・時間・出演者を整え、動かしたくない演目だけを固定します。"));

        JButton regenerateButton = AppTheme.primaryButton("再生成");
        JButton saveButton = AppTheme.secondaryButton("XLSX保存");
        JButton closeButton = AppTheme.quietButton("閉じる");
        regenerateButton.setToolTipText(
                "固定した演目を残し、各公演内の未固定演目だけを並べ直します（Ctrl/Command+R）");
        saveButton.setToolTipText("編集状態と固定情報を保持したXLSXを保存します（Ctrl/Command+S）");

        regenerateButton.addActionListener(event -> regenerate());
        saveButton.addActionListener(event -> saveProject());
        closeButton.addActionListener(event -> dispose());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(closeButton);
        actions.add(saveButton);
        actions.add(regenerateButton);

        panel.add(titleStack, BorderLayout.WEST);
        panel.add(actions, BorderLayout.EAST);
        return panel;
    }

    private JPanel createActionPanel() {
        JPanel panel = AppTheme.card(new BorderLayout());

        JButton addSessionButton = AppTheme.quietButton("公演を追加");
        JButton removeSessionButton = AppTheme.quietButton("公演を削除");
        JButton moveUpButton = AppTheme.quietButton("上へ");
        JButton moveDownButton = AppTheme.quietButton("下へ");
        JButton moveFirstButton = AppTheme.quietButton("先頭へ");
        JButton moveLastButton = AppTheme.quietButton("末尾へ");
        JButton moveToPositionButton = AppTheme.quietButton("指定順へ");
        JButton moveToAnotherSessionButton = AppTheme.quietButton("別の公演へ");
        JButton addEntryButton = AppTheme.quietButton("演目を追加");
        JButton removeEntryButton = AppTheme.quietButton("演目を削除");
        AppTheme.styleDanger(removeSessionButton);
        AppTheme.styleDanger(removeEntryButton);

        addSessionButton.addActionListener(event -> addSession());
        removeSessionButton.addActionListener(event -> removeSelectedSession());
        moveUpButton.addActionListener(event -> moveSelectedEntry(-1));
        moveDownButton.addActionListener(event -> moveSelectedEntry(1));
        moveFirstButton.addActionListener(event -> moveSelectedEntryTo(0));
        moveLastButton.addActionListener(event -> moveSelectedEntryToLast());
        moveToPositionButton.addActionListener(event -> moveSelectedEntryToSpecifiedPosition());
        moveToAnotherSessionButton.addActionListener(event -> moveSelectedEntryToAnotherSession());
        addEntryButton.addActionListener(event -> addEntry());
        removeEntryButton.addActionListener(event -> removeSelectedEntry());

        JPanel groups = transparentBoxPanel(BoxLayout.X_AXIS);
        groups.add(createToolbarGroup("公演", addSessionButton, removeSessionButton, moveToAnotherSessionButton));
        groups.add(Box.createHorizontalStrut(16));
        groups.add(createSeparator());
        groups.add(Box.createHorizontalStrut(16));
        groups.add(createToolbarGroup(
                "曲順", moveUpButton, moveDownButton, moveFirstButton, moveLastButton, moveToPositionButton));
        groups.add(Box.createHorizontalGlue());
        groups.add(createSeparator());
        groups.add(Box.createHorizontalStrut(16));
        groups.add(createToolbarGroup("演目", addEntryButton, removeEntryButton));

        panel.add(groups, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createToolbarGroup(String title, JButton... buttons) {
        JPanel group = transparentBoxPanel(BoxLayout.Y_AXIS);
        JLabel label = AppTheme.fieldLabel(title);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        group.add(label);
        group.add(Box.createVerticalStrut(7));

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        buttonRow.setOpaque(false);
        buttonRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (JButton button : buttons) {
            buttonRow.add(button);
        }
        group.add(buttonRow);
        return group;
    }

    private JSeparator createSeparator() {
        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
        separator.setForeground(AppTheme.BORDER);
        separator.setMaximumSize(new Dimension(1, 54));
        separator.setPreferredSize(new Dimension(1, 54));
        return separator;
    }

    private JPanel transparentBoxPanel(int axis) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, axis));
        return panel;
    }

    private void setProject(SetlistProject project) {
        rebuilding = true;
        sessionTabs.removeAll();
        tableModels.clear();
        tables.clear();
        for (SetlistSession session : project.sessions()) {
            addSessionTab(session.name(), session.entries());
        }
        rebuilding = false;
        publishProject();
    }

    private void addSession() {
        addSessionTab(nextSessionName(), List.of());
        sessionTabs.setSelectedIndex(sessionTabs.getTabCount() - 1);
        publishProject();
    }

    private void addSessionTab(String name, List<SetlistEntry> entries) {
        SetlistEntryTableModel model = new SetlistEntryTableModel(entries);
        model.setValidationErrorHandler(this::showValidationError);
        model.setChangedHandler(this::publishProject);
        model.setPerformerChangeCallbacks(this::choosePerformerChangeScope, this::updatePerformersInAllSessions);

        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(false);
        AppTheme.styleTable(table);
        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(0).setMaxWidth(76);
        table.getColumnModel().getColumn(1).setPreferredWidth(280);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(2).setMaxWidth(130);
        table.getColumnModel().getColumn(3).setPreferredWidth(360);
        table.getColumnModel().getColumn(4).setPreferredWidth(72);
        table.getColumnModel().getColumn(4).setMaxWidth(86);
        tableModels.add(model);
        tables.add(table);
        JScrollPane scrollPane = AppTheme.scroll(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        sessionTabs.addTab(name, scrollPane);
    }

    private SetlistEntryTableModel.PerformerChangeScope choosePerformerChangeScope(
            SetlistEntryTableModel.PerformerEditRequest request) {
        Object[] options = {"この公演だけ変更", "同じ演目を全公演変更", "キャンセル"};
        int choice = JOptionPane.showOptionDialog(
                this,
                "「" + request.entry().title() + "」の出演者変更をどこへ反映しますか？",
                "出演者の変更範囲",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);
        return switch (choice) {
            case 0 -> SetlistEntryTableModel.PerformerChangeScope.CURRENT_SESSION;
            case 1 -> SetlistEntryTableModel.PerformerChangeScope.ALL_SESSIONS;
            default -> SetlistEntryTableModel.PerformerChangeScope.CANCEL;
        };
    }

    private void updatePerformersInAllSessions(UUID sourcePerformanceId, List<String> performers) {
        for (SetlistEntryTableModel model : tableModels) {
            model.updatePerformersBySourceId(sourcePerformanceId, performers);
        }
    }

    private void addEntry() {
        selectedModel().ifPresent(SetlistEntryTableModel::addEntry);
    }

    private void removeSelectedEntry() {
        selectedModel().ifPresent(model -> {
            JTable table = selectedTable();
            int row = table.getSelectedRow();
            if (row < 0) {
                showValidationError("削除する演目を選択してください。");
                return;
            }
            model.removeEntry(row);
        });
    }

    private void moveSelectedEntry(int direction) {
        selectedModel().ifPresent(model -> {
            JTable table = selectedTable();
            int row = table.getSelectedRow();
            if (row < 0) {
                showValidationError("移動する演目を選択してください。");
                return;
            }
            int destination = row + direction;
            model.moveEntry(row, destination);
            if (destination >= 0 && destination < model.getRowCount()) {
                table.setRowSelectionInterval(destination, destination);
            }
        });
    }

    private void moveSelectedEntryTo(int destination) {
        selectedModel().ifPresent(model -> {
            JTable table = selectedTable();
            int row = table.getSelectedRow();
            if (row < 0) {
                showValidationError("移動する演目を選択してください。");
                return;
            }
            model.moveEntry(row, destination);
            table.setRowSelectionInterval(destination, destination);
        });
    }

    private void moveSelectedEntryToLast() {
        selectedModel().ifPresent(model -> moveSelectedEntryTo(model.getRowCount() - 1));
    }

    private void moveSelectedEntryToSpecifiedPosition() {
        selectedModel().ifPresent(model -> {
            JTable table = selectedTable();
            int row = table.getSelectedRow();
            if (row < 0) {
                showValidationError("移動する演目を選択してください。");
                return;
            }
            String input = JOptionPane.showInputDialog(
                    this,
                    "移動先の順番を1から" + model.getRowCount() + "の範囲で入力してください。",
                    row + 1);
            if (input == null) {
                return;
            }
            try {
                int destination = Integer.parseInt(input.trim()) - 1;
                if (destination < 0 || destination >= model.getRowCount()) {
                    throw new NumberFormatException();
                }
                model.moveEntry(row, destination);
                table.setRowSelectionInterval(destination, destination);
            } catch (NumberFormatException exception) {
                showValidationError("移動先は1から" + model.getRowCount() + "の整数で入力してください。");
            }
        });
    }

    private void moveSelectedEntryToAnotherSession() {
        int sourceSessionIndex = sessionTabs.getSelectedIndex();
        if (sourceSessionIndex < 0) {
            showValidationError("公演タブを選択してください。");
            return;
        }
        JTable sourceTable = selectedTable();
        int sourceRowIndex = sourceTable.getSelectedRow();
        if (sourceRowIndex < 0) {
            showValidationError("移動する演目を選択してください。");
            return;
        }
        List<SessionTarget> targets = new ArrayList<>();
        for (int sessionIndex = 0; sessionIndex < sessionTabs.getTabCount(); sessionIndex++) {
            if (sessionIndex != sourceSessionIndex) {
                targets.add(new SessionTarget(sessionIndex, sessionTabs.getTitleAt(sessionIndex)));
            }
        }
        if (targets.isEmpty()) {
            showValidationError("移動先の公演がありません。先に「公演を追加」を押してください。");
            return;
        }
        SessionTarget selectedTarget = (SessionTarget) JOptionPane.showInputDialog(
                this,
                "移動先の公演を選択してください。",
                "別の公演へ移動",
                JOptionPane.QUESTION_MESSAGE,
                null,
                targets.toArray(),
                targets.getFirst());
        if (selectedTarget == null) {
            return;
        }

        SetlistEntry movedEntry = tableModels.get(sourceSessionIndex).removeEntryAndReturn(sourceRowIndex);
        SetlistEntryTableModel targetModel = tableModels.get(selectedTarget.sessionIndex());
        targetModel.appendEntry(movedEntry);
        sessionTabs.setSelectedIndex(selectedTarget.sessionIndex());
        JTable targetTable = tables.get(selectedTarget.sessionIndex());
        int targetRowIndex = targetModel.getRowCount() - 1;
        targetTable.setRowSelectionInterval(targetRowIndex, targetRowIndex);
    }

    private void removeSelectedSession() {
        int selectedIndex = sessionTabs.getSelectedIndex();
        if (selectedIndex < 0) {
            return;
        }
        if (sessionTabs.getTabCount() == 1) {
            showValidationError("少なくとも1つの公演を残してください。");
            return;
        }
        sessionTabs.removeTabAt(selectedIndex);
        tableModels.remove(selectedIndex);
        tables.remove(selectedIndex);
        publishProject();
    }

    private void regenerate() {
        // チェックボックスなど、編集中セルの値を確定してから現在のモデルを取得する。
        stopTableEditing();
        try {
            SetlistProject regenerated = new SetlistRegenerator().regenerate(currentProject());
            setProject(regenerated);
            JOptionPane.showMessageDialog(this, "固定演目を保持して未固定演目を再生成しました。");
        } catch (IllegalArgumentException exception) {
            showValidationError(exception.getMessage());
        }
    }

    private void saveProject() {
        stopTableEditing();
        JFileChooser chooser = new JFileChooser(new File("Data/output"));
        chooser.setDialogTitle("編集可能な香盤表を保存");
        chooser.setFileFilter(new FileNameExtensionFilter("Excelファイル (*.xlsx)", "xlsx"));
        chooser.setSelectedFile(new File("Data/output", "setlist-project.xlsx"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File outputFile = ensureXlsxExtension(chooser.getSelectedFile());
        if (outputFile.exists() && JOptionPane.showConfirmDialog(
                this,
                outputFile.getName() + " を上書きしますか？",
                "上書き確認",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            new XlsxSetlistProjectWriter().write(currentProject(), outputFile);
            JOptionPane.showMessageDialog(this, "編集可能な香盤表を保存しました: " + outputFile.getAbsolutePath());
        } catch (IOException | IllegalArgumentException exception) {
            showValidationError("XLSXの保存に失敗しました: " + exception.getMessage());
        }
    }

    private void stopTableEditing() {
        for (JTable table : tables) {
            if (table.isEditing()) {
                table.getCellEditor().stopCellEditing();
            }
        }
    }

    private File ensureXlsxExtension(File file) {
        return file.getName().toLowerCase().endsWith(".xlsx")
                ? file
                : new File(file.getParentFile(), file.getName() + ".xlsx");
    }

    private java.util.Optional<SetlistEntryTableModel> selectedModel() {
        int selectedIndex = sessionTabs.getSelectedIndex();
        if (selectedIndex < 0) {
            showValidationError("公演タブを選択してください。");
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(tableModels.get(selectedIndex));
    }

    private JTable selectedTable() {
        return tables.get(sessionTabs.getSelectedIndex());
    }

    private String nextSessionName() {
        Set<String> names = new HashSet<>();
        for (int index = 0; index < sessionTabs.getTabCount(); index++) {
            names.add(sessionTabs.getTitleAt(index));
        }
        int number = 1;
        while (names.contains("第" + number + "公演")) {
            number++;
        }
        return "第" + number + "公演";
    }

    private void publishProject() {
        if (!rebuilding) {
            projectChangedHandler.accept(currentProject());
        }
    }

    private void showValidationError(String message) {
        JOptionPane.showMessageDialog(this, message, "入力エラー", JOptionPane.ERROR_MESSAGE);
    }

    private record SessionTarget(int sessionIndex, String sessionName) {

        @Override
        public String toString() {
            return sessionName;
        }
    }
}
