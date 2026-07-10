package jp.ac.u_tokai.cc.javaadvanced;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    private final JButton editButton;
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
        this.editButton = new JButton("編集");
        this.generatedSessions = List.of();
        this.currentProject = SetlistProjectFactory.newEmptyProject();

        setupWindow();
        dataFileBox.addActionListener(event -> updateGenerationInputMode());
        loadDataFiles();
    }

    private void setupWindow() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));
        resultArea.setEditable(false);
        resultArea.setLineWrap(false);
        editButton.setEnabled(false);

        add(createInputPanel(), BorderLayout.NORTH);
        add(new JScrollPane(resultArea), BorderLayout.CENTER);
        add(createActionPanel(), BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(new JLabel("データファイル"));
        panel.add(dataFileBox);
        panel.add(new JLabel("公演数"));
        panel.add(sessionSpinner);
        panel.add(new JLabel("各公演の上限数"));
        panel.add(capacityField);
        panel.add(new JLabel("オープニング"));
        panel.add(openerField);
        panel.add(new JLabel("トリ"));
        panel.add(closerField);
        return panel;
    }

    private JPanel createActionPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton reloadButton = new JButton("再読込");
        JButton generateButton = new JButton("生成");
        JButton startEditingButton = new JButton("編集を開始");
        JButton newButton = new JButton("新規作成");
        JButton openProjectButton = new JButton("編集済みXLSXを開く");
        JButton excelButton = new JButton("Excel出力");
        JButton csvButton = new JButton("CSV出力");

        reloadButton.addActionListener(event -> loadDataFiles());
        generateButton.addActionListener(event -> generateSetlist());
        startEditingButton.addActionListener(event -> startEditingSelectedData());
        newButton.addActionListener(event -> openEditor(SetlistProjectFactory.newEmptyProject()));
        openProjectButton.addActionListener(event -> openSavedProject());
        editButton.addActionListener(event -> openEditor(currentProject));
        excelButton.addActionListener(event -> exportGeneratedSetlist(new XlsxSetlistExporter(), "output_setlist.xlsx"));
        csvButton.addActionListener(event -> exportGeneratedSetlist(new CsvSetlistExporter(), "output_setlist.csv"));

        panel.add(reloadButton);
        panel.add(generateButton);
        panel.add(startEditingButton);
        panel.add(newButton);
        panel.add(openProjectButton);
        panel.add(editButton);
        panel.add(excelButton);
        panel.add(csvButton);
        return panel;
    }

    private void loadDataFiles() {
        dataFileBox.removeAllItems();
        File dataDir = new File(DATA_DIRECTORY);
        File[] files = dataDir.listFiles((directory, name) -> name.endsWith(".csv") || name.endsWith(".xlsx"));
        if (files == null || files.length == 0) {
            resultArea.setText("Dataフォルダに .csv または .xlsx ファイルがありません。");
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
            resultArea.setText(
                    "XLSXは各シートを独立した公演として生成します。公演数・上限数によるシート間配分は行いません。");
        } else {
            resultArea.setText("データファイルを選択して、「生成」または「編集を開始」を押してください。");
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
