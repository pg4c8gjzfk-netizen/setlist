package jp.ac.u_tokai.cc.javaadvanced;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.filechooser.FileNameExtensionFilter;

/** 自動生成、編集画面への遷移、既存形式での出力を行うメイン画面です。 */
public class SetlistFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final String DATA_DIRECTORY = "Data";

    private final JComboBox<File> dataFileBox;
    private final JSpinner sessionSpinner;
    private final JTextField capacityField;
    private final JTextField openerField;
    private final JTextField closerField;
    private final JTextArea resultArea;
    private final JLabel modeHintLabel;
    private final JButton editButton;
    private final JButton generateButton;
    private final JButton startEditingButton;
    private final JButton newButton;
    private final JButton openProjectButton;
    private final JButton excelButton;
    private final JButton csvButton;
    private List<List<Performance>> generatedSessions;
    private SetlistProject currentProject;

    /** メイン画面を作成します。 */
    public SetlistFrame() {
        super("セットリスト自動生成");
        this.dataFileBox = new JComboBox<>();
        this.sessionSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 20, 1));
        this.capacityField = new JTextField(6);
        this.openerField = new JTextField(12);
        this.closerField = new JTextField(12);
        this.resultArea = new JTextArea(22, 70);
        this.modeHintLabel = AppTheme.body("データファイルを選択してください。");
        this.editButton = AppTheme.secondaryButton("編集");
        this.generateButton = AppTheme.secondaryButton("生成");
        this.startEditingButton = AppTheme.primaryButton("編集を開始");
        this.newButton = AppTheme.quietButton("新規作成");
        this.openProjectButton = AppTheme.quietButton("編集済みXLSXを開く");
        this.excelButton = AppTheme.quietButton("Excel出力");
        this.csvButton = AppTheme.quietButton("CSV出力");
        this.generatedSessions = List.of();
        this.currentProject = SetlistProjectFactory.newEmptyProject();

        setupWindow();
        dataFileBox.addActionListener(event -> updateGenerationInputMode());
        loadDataFiles();
    }

    private void setupWindow() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setIconImage(AppIcon.create(64));
        JPanel root = AppTheme.page(new BorderLayout(0, 18));
        root.setBorder(AppTheme.pagePadding());
        setContentPane(root);

        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        resultArea.setText("まだプレビューはありません。\nデータを選び、「編集を開始」または「生成」を選択してください。");
        AppTheme.stylePreview(resultArea);
        editButton.setEnabled(false);

        root.add(createHeaderPanel(), BorderLayout.NORTH);

        JPanel workspace = AppTheme.page(new BorderLayout(0, 16));
        workspace.add(createInputPanel(), BorderLayout.NORTH);
        workspace.add(createPreviewPanel(), BorderLayout.CENTER);
        root.add(workspace, BorderLayout.CENTER);
        root.add(createActionPanel(), BorderLayout.SOUTH);

        getRootPane().setDefaultButton(startEditingButton);
        AppTheme.bindMenuShortcut(getRootPane(), "new-project", KeyEvent.VK_N, newButton::doClick);
        AppTheme.bindMenuShortcut(getRootPane(), "open-project", KeyEvent.VK_O, openProjectButton::doClick);
        AppTheme.bindMenuShortcut(getRootPane(), "generate-setlist", KeyEvent.VK_G, generateButton::doClick);
        setMinimumSize(new Dimension(980, 660));
        setSize(1120, 760);
        setLocationRelativeTo(null);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = AppTheme.page(new BorderLayout(20, 0));
        JPanel titleStack = transparentBoxPanel(BoxLayout.Y_AXIS);
        titleStack.add(AppTheme.title("Setlist Studio"));
        titleStack.add(Box.createVerticalStrut(5));
        titleStack.add(AppTheme.body("出演者の流れを整え、迷いなく本番へ進める香盤表ワークスペース"));

        JLabel localBadge = AppTheme.statusPill("ローカル編集", new java.awt.Color(0x248A3D));

        panel.add(titleStack, BorderLayout.WEST);
        panel.add(localBadge, BorderLayout.EAST);
        return panel;
    }

    private JPanel createInputPanel() {
        JPanel panel = AppTheme.card(new BorderLayout(0, 16));

        JPanel heading = transparentBoxPanel(BoxLayout.Y_AXIS);
        heading.add(AppTheme.heading("入力と生成条件"));
        heading.add(Box.createVerticalStrut(4));
        heading.add(AppTheme.body("XLSXは元シートを公演として保持し、CSVは指定した公演数へ配分します。"));
        panel.add(heading, BorderLayout.NORTH);

        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 0, 12);

        constraints.gridx = 0;
        constraints.weightx = 1.0;
        fields.add(createFieldGroup("データファイル", dataFileBox, 340), constraints);

        JButton reloadButton = AppTheme.quietButton("再読込");
        reloadButton.setToolTipText("Dataフォルダの入力ファイル一覧を更新します");
        reloadButton.addActionListener(event -> loadDataFiles());
        constraints.gridx = 1;
        constraints.weightx = 0;
        constraints.anchor = GridBagConstraints.SOUTHWEST;
        fields.add(reloadButton, constraints);

        constraints.gridx = 2;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        fields.add(createFieldGroup("公演数", sessionSpinner, 84), constraints);
        constraints.gridx = 3;
        fields.add(createFieldGroup("各公演の上限", capacityField, 110), constraints);
        constraints.gridx = 4;
        fields.add(createFieldGroup("オープニング", openerField, 145), constraints);
        constraints.gridx = 5;
        constraints.insets = new Insets(0, 0, 0, 0);
        fields.add(createFieldGroup("トリ", closerField, 145), constraints);
        panel.add(fields, BorderLayout.CENTER);

        modeHintLabel.setForeground(AppTheme.ACCENT);
        panel.add(modeHintLabel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createPreviewPanel() {
        JPanel panel = AppTheme.card(new BorderLayout(0, 14));
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);

        JPanel text = transparentBoxPanel(BoxLayout.Y_AXIS);
        text.add(AppTheme.heading("香盤表プレビュー"));
        text.add(Box.createVerticalStrut(3));
        text.add(AppTheme.body("生成結果と現在の編集状態を、保存前に確認できます。"));
        header.add(text, BorderLayout.WEST);

        editButton.setToolTipText("現在の香盤表を公演タブで編集します");
        header.add(editButton, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        JScrollPane previewScroll = AppTheme.scroll(resultArea);
        previewScroll.setBorder(BorderFactory.createLineBorder(new java.awt.Color(0xE7E7EB)));
        panel.add(previewScroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createActionPanel() {
        JPanel panel = AppTheme.page(new BorderLayout(16, 0));
        generateButton.addActionListener(event -> generateSetlist());
        startEditingButton.addActionListener(event -> startEditingSelectedData());
        newButton.addActionListener(event -> openEditor(SetlistProjectFactory.newEmptyProject()));
        openProjectButton.addActionListener(event -> openSavedProject());
        editButton.addActionListener(event -> openEditor(currentProject));
        excelButton.addActionListener(event -> exportGeneratedSetlist(new XlsxSetlistExporter(), "output_setlist.xlsx"));
        csvButton.addActionListener(event -> exportGeneratedSetlist(new CsvSetlistExporter(), "output_setlist.csv"));

        newButton.setToolTipText("空の香盤表から作成します（Ctrl/Command+N）");
        openProjectButton.setToolTipText("編集状態を保持したXLSXを開きます（Ctrl/Command+O）");
        generateButton.setToolTipText("入力データから曲順を自動生成します（Ctrl/Command+G）");
        startEditingButton.setToolTipText("入力データをそのまま公演タブで開きます");

        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftActions.setOpaque(false);
        leftActions.add(newButton);
        leftActions.add(openProjectButton);

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightActions.setOpaque(false);
        rightActions.add(csvButton);
        rightActions.add(excelButton);
        rightActions.add(generateButton);
        rightActions.add(startEditingButton);

        panel.add(leftActions, BorderLayout.WEST);
        panel.add(rightActions, BorderLayout.EAST);
        return panel;
    }

    private JPanel createFieldGroup(String labelText, Component component, int width) {
        JPanel group = transparentBoxPanel(BoxLayout.Y_AXIS);
        JLabel label = AppTheme.fieldLabel(labelText);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        component.setPreferredSize(new Dimension(width, 38));
        component.setMinimumSize(new Dimension(Math.min(width, 80), 38));
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        group.add(label);
        group.add(Box.createVerticalStrut(6));
        group.add(component);
        return group;
    }

    private JPanel transparentBoxPanel(int axis) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, axis));
        return panel;
    }

    private void loadDataFiles() {
        dataFileBox.removeAllItems();
        File dataDir = new File(DATA_DIRECTORY);
        File[] files = dataDir.listFiles((directory, name) -> name.endsWith(".csv") || name.endsWith(".xlsx"));
        if (files == null || files.length == 0) {
            modeHintLabel.setText("Dataフォルダに .csv または .xlsx ファイルがありません。");
            resultArea.setText("入力データが見つかりません。\nDataフォルダへCSVまたはXLSXを追加し、「再読込」を押してください。");
            return;
        }
        for (File file : files) {
            dataFileBox.addItem(file);
        }
        updateGenerationInputMode();
    }

    private void generateSetlist() {
        File selectedFile = (File) dataFileBox.getSelectedItem();
        if (selectedFile == null) {
            showError("読み込むデータファイルを選択してください。");
            return;
        }

        if (isXlsxFile(selectedFile)) {
            generateSetlistWithinSheets(selectedFile);
            return;
        }

        Map<String, Performance> performances = loadPerformances(selectedFile);
        if (performances.isEmpty()) {
            showError("演目データを読み込めませんでした。");
            return;
        }

        int numberOfSessions = (Integer) sessionSpinner.getValue();
        int[] capacities;
        try {
            capacities = createCapacities(numberOfSessions);
        } catch (NumberFormatException exception) {
            showError("各公演の上限数には1以上の整数を入力してください。");
            return;
        }

        generatedSessions = new SetlistGenerator().generate(
                performances,
                numberOfSessions,
                capacities,
                createRepeatedValues(openerField.getText(), numberOfSessions),
                createRepeatedValues(closerField.getText(), numberOfSessions));
        displayGeneratedProject(SetlistProjectFactory.fromGeneratedSessions(generatedSessions));
    }

    /** XLSXの各シートを独立した公演として、シート内の曲順だけを生成します。 */
    private void generateSetlistWithinSheets(File selectedFile) {
        List<PerformanceSheet> sourceSheets = loadPerformanceSheets(selectedFile);
        if (sourceSheets.isEmpty()) {
            showError("演目データを読み込めませんでした。");
            return;
        }

        List<PerformanceSheet> generatedSheets = new SetlistGenerator().generateWithinSheets(
                sourceSheets, openerField.getText().trim(), closerField.getText().trim());
        generatedSessions = generatedSheets.stream()
                .map(PerformanceSheet::performances)
                .toList();
        displayGeneratedProject(SetlistProjectFactory.fromImportedSheets(generatedSheets));
    }

    private Map<String, Performance> loadPerformances(File selectedFile) {
        PerformanceDataReader loader = PerformanceReaderFactory.create(selectedFile);
        return loader.load(selectedFile);
    }

    private List<PerformanceSheet> loadPerformanceSheets(File selectedFile) {
        PerformanceDataReader loader = PerformanceReaderFactory.create(selectedFile);
        return loader.loadSheets(selectedFile);
    }

    private void startEditingSelectedData() {
        File selectedFile = (File) dataFileBox.getSelectedItem();
        if (selectedFile == null) {
            showError("読み込むデータファイルを選択してください。");
            return;
        }
        List<PerformanceSheet> performanceSheets = loadPerformanceSheets(selectedFile);
        if (performanceSheets.isEmpty()) {
            showError("演目データを読み込めませんでした。");
            return;
        }
        generatedSessions = List.of();
        openEditor(SetlistProjectFactory.fromImportedSheets(performanceSheets));
    }

    /** XLSXでは元シートが公演数を決めるため、シートをまたぐ配分設定を無効にします。 */
    private void updateGenerationInputMode() {
        File selectedFile = (File) dataFileBox.getSelectedItem();
        boolean sheetScoped = selectedFile != null && isXlsxFile(selectedFile);
        sessionSpinner.setEnabled(!sheetScoped);
        capacityField.setEnabled(!sheetScoped);

        if (selectedFile == null) {
            return;
        }
        if (sheetScoped) {
            modeHintLabel.setText(
                    "● XLSXモード：各シートを独立した公演として保持します。シート間の再配分は行いません。");
        } else {
            modeHintLabel.setText("● CSVモード：公演数と上限を使って演目を配分します。");
        }
    }

    private boolean isXlsxFile(File file) {
        return file.getName().toLowerCase(Locale.ROOT).endsWith(".xlsx");
    }

    /**
     * 自動生成済みのプロジェクトを画面へ反映します。
     *
     * <p>編集ボタンは、生成結果を表示した後だけ有効になります。</p>
     *
     * @param generatedProject 自動生成したプロジェクト
     */
    void displayGeneratedProject(SetlistProject generatedProject) {
        currentProject = generatedProject;
        editButton.setEnabled(!currentProject.sessions().isEmpty());
        resultArea.setText(formatProject(currentProject));
    }

    private int[] createCapacities(int numberOfSessions) {
        String capacityText = capacityField.getText().trim();
        if (capacityText.isEmpty()) {
            return null;
        }
        int capacity = Integer.parseInt(capacityText);
        if (capacity <= 0) {
            throw new NumberFormatException("capacity must be positive");
        }
        int[] capacities = new int[numberOfSessions];
        for (int i = 0; i < numberOfSessions; i++) {
            capacities[i] = capacity;
        }
        return capacities;
    }

    private String[] createRepeatedValues(String value, int numberOfSessions) {
        String[] values = new String[numberOfSessions];
        for (int i = 0; i < numberOfSessions; i++) {
            values[i] = value == null ? "" : value.trim();
        }
        return values;
    }

    private void openEditor(SetlistProject project) {
        currentProject = project;
        editButton.setEnabled(true);
        SetlistEditorFrame editor = new SetlistEditorFrame(project, updatedProject -> {
            currentProject = updatedProject;
            resultArea.setText(formatProject(updatedProject));
        });
        editor.setVisible(true);
    }

    private void openSavedProject() {
        JFileChooser chooser = new JFileChooser(new File("Data/output"));
        chooser.setDialogTitle("編集可能な香盤表XLSXを開く");
        chooser.setFileFilter(new FileNameExtensionFilter("Excelファイル (*.xlsx)", "xlsx"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            SetlistProject loadedProject = new XlsxSetlistProjectReader().read(chooser.getSelectedFile());
            openEditor(loadedProject);
        } catch (IOException | IllegalArgumentException exception) {
            showError("編集可能な香盤表XLSXを開けませんでした: " + exception.getMessage());
        }
    }

    private void exportGeneratedSetlist(SetlistExporter exporter, String fileName) {
        if (generatedSessions.isEmpty()) {
            showError("先にセットリストを生成してください。編集画面の保存機能は次の更新で利用できます。");
            return;
        }
        exporter.export(generatedSessions, fileName);
        JOptionPane.showMessageDialog(this, "Dataフォルダに出力しました: " + fileName);
    }

    private String formatProject(SetlistProject project) {
        StringBuilder builder = new StringBuilder();
        for (SetlistSession session : project.sessions()) {
            builder.append("【").append(session.name()).append("】\n");
            for (int index = 0; index < session.entries().size(); index++) {
                SetlistEntry entry = session.entries().get(index);
                builder.append(index + 1)
                        .append(". ")
                        .append(entry.title())
                        .append(" / ")
                        .append(String.join(", ", entry.performers()));
                if (entry.fixed()) {
                    builder.append(" [固定: ").append(entry.fixedPosition()).append("]");
                }
                builder.append('\n');
            }
            builder.append('\n');
        }
        return builder.toString();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "エラー", JOptionPane.ERROR_MESSAGE);
    }

    /** メイン画面を表示します。 */
    public void showScreen() {
        setVisible(true);
    }
}
