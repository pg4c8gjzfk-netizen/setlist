package jp.ac.u_tokai.cc.javaadvanced;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;

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
        setLayout(new BorderLayout(8, 8));
        add(new JLabel("曲名・時間・出演者・固定条件を編集してから「再生成」を押してください。"), BorderLayout.NORTH);
        add(sessionTabs, BorderLayout.CENTER);
        add(createActionPanel(), BorderLayout.SOUTH);
        setSize(920, 560);
        setLocationByPlatform(true);
    }

    private JPanel createActionPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton addSessionButton = new JButton("公演を追加");
        JButton removeSessionButton = new JButton("公演を削除");
        JButton moveUpButton = new JButton("上へ");
        JButton moveDownButton = new JButton("下へ");
        JButton addEntryButton = new JButton("演目を追加");
        JButton removeEntryButton = new JButton("演目を削除");
        JButton regenerateButton = new JButton("再生成");
        JButton closeButton = new JButton("閉じる");

        addSessionButton.addActionListener(event -> addSession());
        removeSessionButton.addActionListener(event -> removeSelectedSession());
        moveUpButton.addActionListener(event -> moveSelectedEntry(-1));
        moveDownButton.addActionListener(event -> moveSelectedEntry(1));
        addEntryButton.addActionListener(event -> addEntry());
        removeEntryButton.addActionListener(event -> removeSelectedEntry());
        regenerateButton.addActionListener(event -> regenerate());
        closeButton.addActionListener(event -> dispose());

        panel.add(addSessionButton);
        panel.add(removeSessionButton);
        panel.add(moveUpButton);
        panel.add(moveDownButton);
        panel.add(addEntryButton);
        panel.add(removeEntryButton);
        panel.add(regenerateButton);
        panel.add(closeButton);
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
        addSessionTab("第" + (sessionTabs.getTabCount() + 1) + "公演", List.of());
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
        table.getColumnModel().getColumn(5).setCellEditor(new javax.swing.DefaultCellEditor(
                new JComboBox<>(FixedPosition.values())));
        tableModels.add(model);
        tables.add(table);
        sessionTabs.addTab(name, new JScrollPane(table));
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
        try {
            SetlistProject regenerated = new SetlistRegenerator().regenerate(currentProject());
            setProject(regenerated);
            JOptionPane.showMessageDialog(this, "固定演目を保持して未固定演目を再生成しました。");
        } catch (IllegalArgumentException exception) {
            showValidationError(exception.getMessage());
        }
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

    private void publishProject() {
        if (!rebuilding) {
            projectChangedHandler.accept(currentProject());
        }
    }

    private void showValidationError(String message) {
        JOptionPane.showMessageDialog(this, message, "入力エラー", JOptionPane.ERROR_MESSAGE);
    }
}
