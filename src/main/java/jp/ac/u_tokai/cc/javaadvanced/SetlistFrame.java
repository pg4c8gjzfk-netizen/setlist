package jp.ac.u_tokai.cc.javaadvanced;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

/**
 * セットリスト自動生成アプリを画面で操作するためのクラス。
 * Dataフォルダ内のファイル選択、生成条件の入力、生成結果の表示、Excel/CSV保存を画面から実行します。
 */
public class SetlistFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final String DATA_DIRECTORY = "Data";

    private final JComboBox<File> dataFileBox;
    private final JSpinner sessionSpinner;
    private final JTextField capacityField;
    private final JTextField openerField;
    private final JTextField closerField;
    private final JTextArea resultArea;
    private List<List<Performance>> generatedSessions;

    /**
     * セットリスト生成画面を初期化します。
     */
    public SetlistFrame() {
        super("セットリスト自動生成");
        this.dataFileBox = new JComboBox<>();
        this.sessionSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 20, 1));
        this.capacityField = new JTextField(6);
        this.openerField = new JTextField(12);
        this.closerField = new JTextField(12);
        this.resultArea = new JTextArea(22, 70);
        this.generatedSessions = List.of();

        setupWindow();
        loadDataFiles();
    }

    /**
     * 画面部品を配置し、ウィンドウの基本設定を行います。
     */
    private void setupWindow() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        this.resultArea.setEditable(false);
        this.resultArea.setLineWrap(false);

        add(createInputPanel(), BorderLayout.NORTH);
        add(new JScrollPane(this.resultArea), BorderLayout.CENTER);
        add(createActionPanel(), BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
    }

    /**
     * ファイル選択や生成条件を入力するパネルを作成します。
     *
     * @return 入力用パネル
     */
    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(new JLabel("データファイル"));
        panel.add(this.dataFileBox);
        panel.add(new JLabel("公演数"));
        panel.add(this.sessionSpinner);
        panel.add(new JLabel("各公演の曲数"));
        panel.add(this.capacityField);
        panel.add(new JLabel("オープニング"));
        panel.add(this.openerField);
        panel.add(new JLabel("トリ"));
        panel.add(this.closerField);
        return panel;
    }

    /**
     * 生成や保存を実行するボタンを配置したパネルを作成します。
     *
     * @return 操作用パネル
     */
    private JPanel createActionPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton reloadButton = new JButton("再読込");
        JButton generateButton = new JButton("生成");
        JButton excelButton = new JButton("Excel保存");
        JButton csvButton = new JButton("CSV保存");

        reloadButton.addActionListener(event -> loadDataFiles());
        generateButton.addActionListener(event -> generateSetlist());
        excelButton.addActionListener(event -> exportGeneratedSetlist(new XlsxSetlistExporter(), "output_setlist.xlsx"));
        csvButton.addActionListener(event -> exportGeneratedSetlist(new CsvSetlistExporter(), "output_setlist.csv"));

        panel.add(reloadButton);
        panel.add(generateButton);
        panel.add(excelButton);
        panel.add(csvButton);
        return panel;
    }

    /**
     * DataフォルダからCSVまたはExcelファイルを読み込み、選択欄に反映します。
     */
    private void loadDataFiles() {
        this.dataFileBox.removeAllItems();
        File dataDir = new File(DATA_DIRECTORY);
        File[] files = dataDir.listFiles((dir, name) -> name.endsWith(".csv") || name.endsWith(".xlsx"));
        if (files == null || files.length == 0) {
            this.resultArea.setText("Dataフォルダに.csvまたは.xlsxファイルがありません。");
            return;
        }

        for (File file : files) {
            this.dataFileBox.addItem(file);
        }
        this.resultArea.setText("データファイルを選択して、生成ボタンを押してください。");
    }

    /**
     * 画面で入力された条件をもとにセットリストを生成し、結果欄へ表示します。
     */
    private void generateSetlist() {
        File selectedFile = (File) this.dataFileBox.getSelectedItem();
        if (selectedFile == null) {
            showError("読み込むデータファイルを選択してください。");
            return;
        }

        int numberOfSessions = (Integer) this.sessionSpinner.getValue();
        Map<String, Performance> performances = loadPerformances(selectedFile);
        if (performances.isEmpty()) {
            showError("演目データを読み込めませんでした。");
            return;
        }

        int[] capacities;
        try {
            capacities = createCapacities(numberOfSessions);
        } catch (NumberFormatException e) {
            showError("各公演の曲数には1以上の数字を入力してください。");
            return;
        }
        String[] openers = createRepeatedValues(this.openerField.getText(), numberOfSessions);
        String[] closers = createRepeatedValues(this.closerField.getText(), numberOfSessions);

        SetlistGenerator generator = new SetlistGenerator();
        this.generatedSessions = generator.generate(performances, numberOfSessions, capacities, openers, closers);
        this.resultArea.setText(formatSessions(this.generatedSessions));
    }

    /**
     * 選択されたファイルから演目データを読み込みます。
     *
     * @param selectedFile 読み込み対象のファイル
     * @return 読み込んだ演目データ
     */
    private Map<String, Performance> loadPerformances(File selectedFile) {
        PerformanceDataReader loader = PerformanceReaderFactory.create(selectedFile);
        return loader.load(selectedFile);
    }

    /**
     * 画面の曲数入力から各公演の曲数上限を作成します。
     *
     * @param numberOfSessions 公演数
     * @return 曲数上限。未入力の場合はnull
     */
    private int[] createCapacities(int numberOfSessions) {
        String capacityText = this.capacityField.getText().trim();
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

    /**
     * 1つの入力値を公演数分の配列にして返します。
     *
     * @param value            画面から入力された文字列
     * @param numberOfSessions 公演数
     * @return 公演数分の文字列配列
     */
    private String[] createRepeatedValues(String value, int numberOfSessions) {
        String[] values = new String[numberOfSessions];
        for (int i = 0; i < numberOfSessions; i++) {
            values[i] = value == null ? "" : value.trim();
        }
        return values;
    }

    /**
     * 生成済みセットリストを指定された形式でファイルへ保存します。
     *
     * @param exporter 保存処理を行うクラス
     * @param fileName 保存先ファイル名
     */
    private void exportGeneratedSetlist(SetlistExporter exporter, String fileName) {
        if (this.generatedSessions == null || this.generatedSessions.isEmpty()) {
            showError("先にセットリストを生成してください。");
            return;
        }

        exporter.export(this.generatedSessions, fileName);
        JOptionPane.showMessageDialog(this, "Dataフォルダに保存しました: " + fileName);
    }

    /**
     * 生成結果を画面表示用の文字列に変換します。
     *
     * @param sessions 生成されたセットリスト
     * @return 表示用文字列
     */
    private String formatSessions(List<List<Performance>> sessions) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < sessions.size(); i++) {
            builder.append("【第").append(i + 1).append("公演】\n");
            List<Performance> session = sessions.get(i);
            for (int j = 0; j < session.size(); j++) {
                Performance performance = session.get(j);
                builder.append(j + 1)
                        .append(". ")
                        .append(performance.getDisplayTitle())
                        .append(" / ")
                        .append(String.join(", ", performance.getPerformers()))
                        .append('\n');
            }
            builder.append('\n');
        }
        return builder.toString();
    }

    /**
     * エラーメッセージを画面に表示します。
     *
     * @param message 表示するエラーメッセージ
     */
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "エラー", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * セットリスト生成画面を表示します。
     */
    public void showScreen() {
        setVisible(true);
    }

}
