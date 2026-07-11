package jp.ac.u_tokai.cc.javaadvanced;

import java.awt.BorderLayout;
import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.AbstractCellEditor;
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
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;

/** 公演ごとの演目を直接編集し、再生成できる画面です。 */
public final class SetlistEditorFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private final JTabbedPane sessionTabs;
    private final List<SetlistEntryTableModel> tableModels;
    private final List<JTable> tables;
    private final Consumer<SetlistProject> projectChangedHandler;
    private final BiConsumer<SetlistProject, File> projectSavedHandler;
    private final JButton addSessionButton;
    private final JButton removeSessionButton;
    private final JButton moveToAnotherSessionButton;
    private final JLabel boundaryStatusLabel;
    private final JLabel saveStatusLabel;
    private boolean rebuilding;
    private boolean sheetBoundariesLocked;
    private boolean unsavedChanges;

    public SetlistEditorFrame(SetlistProject project, Consumer<SetlistProject> projectChangedHandler) {
        this(project, projectChangedHandler, (savedProject, outputFile) -> {
        }, false);
    }

    SetlistEditorFrame(
            SetlistProject project,
            Consumer<SetlistProject> projectChangedHandler,
            BiConsumer<SetlistProject, File> projectSavedHandler,
            boolean initiallyUnsaved) {
        super("香盤表を編集");
        this.sessionTabs = new JTabbedPane();
        this.tableModels = new ArrayList<>();
        this.tables = new ArrayList<>();
        this.projectChangedHandler = Objects.requireNonNull(projectChangedHandler, "projectChangedHandler must not be null");
        this.projectSavedHandler = Objects.requireNonNull(projectSavedHandler, "projectSavedHandler must not be null");
        this.addSessionButton = AppTheme.quietButton("公演を追加");
        this.removeSessionButton = AppTheme.quietButton("公演を削除");
        this.moveToAnotherSessionButton = AppTheme.quietButton("別の公演へ");
        this.boundaryStatusLabel = AppTheme.statusPill("シート境界を保持", new java.awt.Color(0x248A3D));
        this.boundaryStatusLabel.setToolTipText("XLSXの各シートを独立した公演として保持します。");
        this.boundaryStatusLabel.setVisible(false);
        this.saveStatusLabel = AppTheme.statusPill("変更なし", AppTheme.TEXT_SECONDARY);
        this.unsavedChanges = initiallyUnsaved;
        setupWindow();
        setProject(Objects.requireNonNull(project, "project must not be null"));
        updateSaveStatus();
    }

    /** 現在の編集内容を取得します。 */
    public SetlistProject currentProject() {
        List<SetlistSession> sessions = new ArrayList<>();
        for (int index = 0; index < tableModels.size(); index++) {
            SetlistEntryTableModel model = tableModels.get(index);
            sessions.add(new SetlistSession(
                    sessionTabs.getTitleAt(index), model.entries(), model.performerNames()));
        }
        return new SetlistProject(sessions, sheetBoundariesLocked);
    }

    private void setupWindow() {
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setIconImage(AppIcon.create(64));
        JPanel root = AppTheme.page(new BorderLayout(0, 16));
        root.setBorder(AppTheme.pagePadding());
        root.setFocusable(true);
        setContentPane(root);

        sessionTabs.putClientProperty(
                com.formdev.flatlaf.FlatClientProperties.STYLE,
                "tabArc: 12; tabHeight: 40; selectedBackground: #FFFFFF; hoverColor: #ECECF0");

        root.add(createHeaderPanel(), BorderLayout.NORTH);
        JPanel editorCard = AppTheme.card(new BorderLayout());
        editorCard.add(createPerformerPanel(), BorderLayout.NORTH);
        editorCard.add(sessionTabs, BorderLayout.CENTER);
        root.add(editorCard, BorderLayout.CENTER);
        root.add(createActionPanel(), BorderLayout.SOUTH);

        AppTheme.bindMenuShortcut(getRootPane(), "save-project", KeyEvent.VK_S, this::saveProject);
        AppTheme.bindMenuShortcut(getRootPane(), "regenerate-project", KeyEvent.VK_R, this::regenerate);
        AppTheme.bindMenuShortcut(getRootPane(), "close-editor", KeyEvent.VK_W, this::requestClose);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent event) {
                root.requestFocusInWindow();
            }

            @Override
            public void windowClosing(WindowEvent event) {
                requestClose();
            }
        });

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
        closeButton.addActionListener(event -> requestClose());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(saveStatusLabel);
        actions.add(boundaryStatusLabel);
        actions.add(closeButton);
        actions.add(saveButton);
        actions.add(regenerateButton);

        panel.add(titleStack, BorderLayout.WEST);
        panel.add(actions, BorderLayout.EAST);
        return panel;
    }

    private JPanel createActionPanel() {
        JPanel panel = AppTheme.card(new BorderLayout());

        JButton moveUpButton = AppTheme.quietButton("上へ");
        JButton moveDownButton = AppTheme.quietButton("下へ");
        JButton moveFirstButton = AppTheme.quietButton("先頭へ");
        JButton moveLastButton = AppTheme.quietButton("末尾へ");
        JButton moveToPositionButton = AppTheme.quietButton("指定順へ");
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

    private JPanel createPerformerPanel() {
        JPanel panel = new JPanel(new BorderLayout(16, 0));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        JPanel explanation = transparentBoxPanel(BoxLayout.Y_AXIS);
        explanation.add(AppTheme.fieldLabel("演者別の出演欄"));
        explanation.add(Box.createVerticalStrut(4));
        explanation.add(AppTheme.body("出演する演目のセルを選ぶと◯が付き、空欄は出演なしを表します。"));

        JButton addPerformerButton = AppTheme.quietButton("演者を追加");
        JButton removePerformerButton = AppTheme.quietButton("演者を削除");
        AppTheme.styleDanger(removePerformerButton);
        addPerformerButton.addActionListener(event -> addPerformer());
        removePerformerButton.addActionListener(event -> removePerformer());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actions.setOpaque(false);
        actions.add(addPerformerButton);
        actions.add(removePerformerButton);

        panel.add(explanation, BorderLayout.WEST);
        panel.add(actions, BorderLayout.EAST);
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
        sheetBoundariesLocked = project.sheetBoundariesLocked();
        updateSheetBoundaryControls();
        sessionTabs.removeAll();
        tableModels.clear();
        tables.clear();
        for (SetlistSession session : project.sessions()) {
            addSessionTab(session.name(), session.entries(), session.performerNames());
        }
        rebuilding = false;
    }

    private void updateSheetBoundaryControls() {
        boolean canChangeSessions = !sheetBoundariesLocked;
        addSessionButton.setEnabled(canChangeSessions);
        removeSessionButton.setEnabled(canChangeSessions);
        moveToAnotherSessionButton.setEnabled(canChangeSessions);
        boundaryStatusLabel.setVisible(sheetBoundariesLocked);

        if (sheetBoundariesLocked) {
            String explanation = "XLSXでは元シートの追加・削除・公演間移動はできません。";
            addSessionButton.setToolTipText(explanation);
            removeSessionButton.setToolTipText(explanation);
            moveToAnotherSessionButton.setToolTipText(explanation);
        } else {
            addSessionButton.setToolTipText("新しい公演タブを追加します。");
            removeSessionButton.setToolTipText("選択中の公演タブを削除します。");
            moveToAnotherSessionButton.setToolTipText("選択した演目を別の公演へ移動します。");
        }
    }

    private void addSession() {
        if (sheetBoundariesLocked) {
            showValidationError("XLSXのシート構成は変更できません。");
            return;
        }
        addSessionTab(nextSessionName(), List.of(), List.of());
        sessionTabs.setSelectedIndex(sessionTabs.getTabCount() - 1);
        publishProject();
    }

    private void addSessionTab(
            String name, List<SetlistEntry> entries, List<String> performerNames) {
        SetlistEntryTableModel model = new SetlistEntryTableModel(entries, performerNames);
        model.setValidationErrorHandler(this::showValidationError);
        model.setChangedHandler(this::publishProject);
        model.setPerformerChangeCallbacks(this::choosePerformerChangeScope, this::updatePerformersInAllSessions);

        JTable table = new EmptyStateTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(false);
        AppTheme.styleTable(table);
        configureTableColumns(table, model);
        model.addTableModelListener(event -> {
            if (event.getFirstRow() == TableModelEvent.HEADER_ROW) {
                SwingUtilities.invokeLater(() -> configureTableColumns(table, model));
            }
        });
        table.getAccessibleContext().setAccessibleName(name + "の演目一覧");
        table.getAccessibleContext().setAccessibleDescription(
                "演者ごとの列では、◯が出演、空欄が出演なしを表します。");
        tableModels.add(model);
        tables.add(table);
        JScrollPane scrollPane = AppTheme.scroll(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        sessionTabs.addTab(name, scrollPane);
    }

    private void configureTableColumns(JTable table, SetlistEntryTableModel model) {
        if (table.getColumnCount() != model.getColumnCount()) {
            return;
        }
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getTableHeader().setReorderingAllowed(false);

        TableColumn orderColumn = table.getColumnModel().getColumn(0);
        orderColumn.setPreferredWidth(60);
        orderColumn.setMaxWidth(76);
        table.getColumnModel().getColumn(1).setPreferredWidth(280);
        TableColumn durationColumn = table.getColumnModel().getColumn(2);
        durationColumn.setPreferredWidth(100);
        durationColumn.setMaxWidth(130);

        for (String performerName : model.performerNames()) {
            int columnIndex = model.performerColumnIndex(performerName);
            TableColumn performerColumn = table.getColumnModel().getColumn(columnIndex);
            performerColumn.setMinWidth(72);
            performerColumn.setPreferredWidth(Math.min(132, Math.max(82, performerName.length() * 18)));
            performerColumn.setCellRenderer(new ParticipationMarkRenderer());
            performerColumn.setCellEditor(new ParticipationMarkEditor());
        }

        TableColumn fixedColumn = table.getColumnModel().getColumn(model.fixedColumnIndex());
        fixedColumn.setPreferredWidth(72);
        fixedColumn.setMaxWidth(86);
    }

    private SetlistEntryTableModel.PerformerChangeScope choosePerformerChangeScope(
            SetlistEntryTableModel.PerformerEditRequest request) {
        long matchingEntries = tableModels.stream()
                .flatMap(model -> model.entries().stream())
                .filter(entry -> entry.sourcePerformanceId().equals(request.entry().sourcePerformanceId()))
                .count();
        if (matchingEntries <= 1) {
            return SetlistEntryTableModel.PerformerChangeScope.CURRENT_SESSION;
        }
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

    private void addPerformer() {
        selectedModel().ifPresent(model -> {
            String performerName = JOptionPane.showInputDialog(
                    this, "追加する演者名を入力してください。", "演者を追加", JOptionPane.PLAIN_MESSAGE);
            if (performerName == null) {
                return;
            }
            try {
                model.addPerformer(performerName);
            } catch (IllegalArgumentException exception) {
                showValidationError(exception.getMessage());
            }
        });
    }

    private void removePerformer() {
        selectedModel().ifPresent(model -> {
            if (model.performerNames().isEmpty()) {
                showValidationError("削除できる演者がいません。");
                return;
            }
            String performerName = (String) JOptionPane.showInputDialog(
                    this,
                    "削除する演者を選んでください。出演中の演者は削除できません。",
                    "演者を削除",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    model.performerNames().toArray(),
                    model.performerNames().getFirst());
            if (performerName == null) {
                return;
            }
            try {
                model.removePerformer(performerName);
            } catch (IllegalArgumentException exception) {
                showValidationError(exception.getMessage());
            }
        });
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
        if (sheetBoundariesLocked) {
            showValidationError("XLSXの演目は元シート以外の公演へ移動できません。");
            return;
        }
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
        if (sheetBoundariesLocked) {
            showValidationError("XLSXのシート構成は変更できません。");
            return;
        }
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
            publishProject();
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
            File savedFile = writeEditableProject(outputFile);
            JOptionPane.showMessageDialog(this, "編集可能な香盤表を保存しました: " + savedFile.getAbsolutePath());
        } catch (IOException | IllegalArgumentException exception) {
            showValidationError("XLSXの保存に失敗しました: " + exception.getMessage());
        }
    }

    /** 現在の編集状態を保存し、未保存表示を解除します。 */
    File writeEditableProject(File outputFile) throws IOException {
        stopTableEditing();
        SetlistProject project = currentProject();
        File absoluteFile = outputFile.getAbsoluteFile();
        new XlsxSetlistProjectWriter().write(project, absoluteFile);
        setUnsavedChanges(false);
        projectSavedHandler.accept(project, absoluteFile);
        return absoluteFile;
    }

    /** メイン画面側の保存・終了前に、編集中セルの値を確定します。 */
    void commitPendingEdits() {
        stopTableEditing();
    }

    private void requestClose() {
        commitPendingEdits();
        dispose();
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
            setUnsavedChanges(true);
            projectChangedHandler.accept(currentProject());
        }
    }

    boolean hasUnsavedChanges() {
        return unsavedChanges;
    }

    private void setUnsavedChanges(boolean value) {
        unsavedChanges = value;
        updateSaveStatus();
    }

    private void updateSaveStatus() {
        if (unsavedChanges) {
            AppTheme.updateStatusPill(
                    saveStatusLabel, "未保存の変更", new java.awt.Color(0xB54708));
            saveStatusLabel.setToolTipText("編集状態をXLSX保存すると保護できます。");
            setTitle("香盤表を編集 *");
        } else {
            AppTheme.updateStatusPill(saveStatusLabel, "変更なし", new java.awt.Color(0x248A3D));
            saveStatusLabel.setToolTipText("現在の編集内容は保存済み、または変更されていません。");
            setTitle("香盤表を編集");
        }
    }

    private void showValidationError(String message) {
        JOptionPane.showMessageDialog(this, message, "入力エラー", JOptionPane.ERROR_MESSAGE);
    }

    /** 演目がない場合にも、次の操作を画面内で案内する表です。 */
    private static final class EmptyStateTable extends JTable {

        private static final long serialVersionUID = 1L;

        private EmptyStateTable(SetlistEntryTableModel model) {
            super(model);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (getRowCount() != 0) {
                return;
            }

            Rectangle visibleArea = getVisibleRect();
            if (visibleArea.width < 200 || visibleArea.height < 120) {
                return;
            }

            Graphics2D graphics2D = (Graphics2D) graphics.create();
            try {
                graphics2D.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics2D.setRenderingHint(
                        RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int centerX = visibleArea.x + visibleArea.width / 2;
                int centerY = visibleArea.y + visibleArea.height / 2 - 12;
                int iconSize = 38;
                int iconX = centerX - iconSize / 2;
                int iconY = centerY - 62;

                graphics2D.setColor(AppTheme.SURFACE_SUBTLE);
                graphics2D.fillOval(iconX, iconY, iconSize, iconSize);
                graphics2D.setColor(AppTheme.BORDER);
                graphics2D.drawOval(iconX, iconY, iconSize, iconSize);
                graphics2D.setColor(AppTheme.ACCENT);
                graphics2D.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                graphics2D.drawLine(centerX - 7, iconY + iconSize / 2, centerX + 7, iconY + iconSize / 2);
                graphics2D.drawLine(centerX, iconY + iconSize / 2 - 7, centerX, iconY + iconSize / 2 + 7);

                graphics2D.setColor(AppTheme.TEXT_PRIMARY);
                graphics2D.setFont(getFont().deriveFont(Font.BOLD, 16f));
                drawCentered(graphics2D, "演目がありません", centerX, centerY + 4);

                graphics2D.setColor(AppTheme.TEXT_SECONDARY);
                graphics2D.setFont(getFont().deriveFont(Font.PLAIN, 13f));
                drawCentered(graphics2D, "下の「演目を追加」から始められます。", centerX, centerY + 28);
            } finally {
                graphics2D.dispose();
            }
        }

        private static void drawCentered(Graphics2D graphics, String text, int centerX, int baseline) {
            FontMetrics metrics = graphics.getFontMetrics();
            graphics.drawString(text, centerX - metrics.stringWidth(text) / 2, baseline);
        }
    }

    /** 出演状態をExcelと同じ◯／空欄で表示します。 */
    private static final class ParticipationMarkRenderer extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;

        private ParticipationMarkRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setText(Boolean.TRUE.equals(value) ? "◯" : "");
            setFont(getFont().deriveFont(Font.BOLD, 16f));
            return this;
        }
    }

    /** クリック時も標準チェック記号を出さず、◯／空欄だけで状態を切り替えます。 */
    private static final class ParticipationMarkEditor extends AbstractCellEditor implements TableCellEditor {

        private static final long serialVersionUID = 1L;
        private final JLabel editorLabel;
        private boolean editedValue;

        private ParticipationMarkEditor() {
            editorLabel = new JLabel("", SwingConstants.CENTER);
            editorLabel.setOpaque(true);
        }

        @Override
        public Object getCellEditorValue() {
            return editedValue;
        }

        @Override
        public Component getTableCellEditorComponent(
                JTable table,
                Object value,
                boolean isSelected,
                int row,
                int column) {
            editedValue = !Boolean.TRUE.equals(value);
            editorLabel.setText(editedValue ? "◯" : "");
            editorLabel.setFont(table.getFont().deriveFont(Font.BOLD, 16f));
            editorLabel.setBackground(table.getSelectionBackground());
            editorLabel.setForeground(table.getSelectionForeground());
            SwingUtilities.invokeLater(this::stopCellEditing);
            return editorLabel;
        }
    }

    private record SessionTarget(int sessionIndex, String sessionName) {

        @Override
        public String toString() {
            return sessionName;
        }
    }
}
