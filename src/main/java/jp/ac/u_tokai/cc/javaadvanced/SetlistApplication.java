package jp.ac.u_tokai.cc.javaadvanced;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * コンソール操作、ファイル入出力、セットリスト生成の流れを管理するクラス。
 * Appクラスに処理を詰め込まず、実際のアプリケーションとして使いやすい単位に分けています。
 */
public class SetlistApplication {
    private static final String DATA_DIRECTORY = "Data";
    private static final String EXCEL_OUTPUT_FILE = "output_setlist.xlsx";
    private static final String CSV_OUTPUT_FILE = "output_setlist.csv";

    private final Scanner scanner;
    private final PerformanceSearcher searcher;
    private final SetlistGenerator generator;

    /**
     * 標準入力を使うコンソールアプリとして初期化します。
     */
    public SetlistApplication() {
        this.scanner = new Scanner(System.in);
        this.searcher = new PerformanceSearcher();
        this.generator = new SetlistGenerator();
    }

    /**
     * アプリケーション全体の流れを実行します。
     */
    public void run() {
        System.out.println("=== セットリスト自動作成 ===\n");

        int numberOfSessions = readPositiveInt("作成する公演回数を入力してください。：");
        File selectedFile = chooseDataFile();
        if (selectedFile == null) {
            return;
        }

        DataLoader loader = DataLoaderFactory.create(selectedFile);
        Map<String, Performance> performanceMap = loader.load(selectedFile);
        if (performanceMap.isEmpty()) {
            System.out.println("読み込める演目データがありませんでした。プログラムを終了します。");
            return;
        }

        System.out.println("\n>>> 読み込み完了: 全" + performanceMap.size() + "件の演目を登録しました。");
        searchPerformances(performanceMap);

        int[] capacities = readCapacities(numberOfSessions);
        String[][] fixedSongs = readFixedSongs(numberOfSessions);
        List<List<Performance>> generatedSessions = generator.generate(
                performanceMap,
                numberOfSessions,
                capacities,
                fixedSongs[0],
                fixedSongs[1]);

        printSessions(generatedSessions);
        exportSessions(generatedSessions);
        printExecutionInfo();
        scanner.close();
    }

    /**
     * Dataフォルダ内から読み込み対象のデータファイルを選択します。
     *
     * @return 選択されたファイル。対象ファイルがない場合はnull
     */
    private File chooseDataFile() {
        File dataDir = new File(DATA_DIRECTORY);
        File[] targetFiles = dataDir.listFiles((dir, name) -> name.endsWith(".csv") || name.endsWith(".xlsx"));

        if (targetFiles == null || targetFiles.length == 0) {
            System.out.println("Dataフォルダ内に.csvまたは.xlsxファイルが見つかりません。プログラムを終了します。");
            return null;
        }

        System.out.println(">>> 読み込むファイルを選択してください。");
        for (int i = 0; i < targetFiles.length; i++) {
            System.out.println((i + 1) + ": " + targetFiles[i].getName());
        }

        int choice = readMenuChoice("番号を入力：", 1, targetFiles.length);
        File selectedFile = targetFiles[choice - 1];
        System.out.println("\n>>> 「" + selectedFile.getName() + "」を読み込みます...");
        return selectedFile;
    }

    /**
     * 演目検索を実行します。
     */
    private void searchPerformances(Map<String, Performance> performanceMap) {
        System.out.println("\n--- 演目の検索 ---");
        System.out.print("検索したい演目名を入力してください：");
        String searchKey = scanner.nextLine();
        searcher.searchAndDisplay(performanceMap, searchKey);
    }

    /**
     * 各公演の演目数設定を読み取ります。
     */
    private int[] readCapacities(int numberOfSessions) {
        System.out.println("\n--- 各公演の演目数設定 ---");
        System.out.println("1: 設定しない（全曲を均等に自動配分）");
        System.out.println("2: 全公演一括で同じ演目数を設定する");
        System.out.println("3: 公演ごとに個別に演目数を設定する");

        int capacityChoice = readMenuChoice("番号を入力：", 1, 3);
        if (capacityChoice == 1) {
            return null;
        }

        int[] capacities = new int[numberOfSessions];
        if (capacityChoice == 2) {
            int capacity = readPositiveInt("1公演あたりの演目数を入力してください：");
            for (int i = 0; i < numberOfSessions; i++) {
                capacities[i] = capacity;
            }
        } else {
            for (int i = 0; i < numberOfSessions; i++) {
                capacities[i] = readPositiveInt("第" + (i + 1) + "公演の演目数を入力してください：");
            }
        }
        return capacities;
    }

    /**
     * 各公演のオープニング曲とトリ曲の指定を読み取ります。
     */
    private String[][] readFixedSongs(int numberOfSessions) {
        System.out.println("\n--- オープニング・トリの設定 ---");
        System.out.println("特定の曲を各公演のオープニング（最初）やトリ（最後）に指定できます。");
        System.out.println("※指定しない場合はそのままEnterを押してください。");

        String[] openers = new String[numberOfSessions];
        String[] closers = new String[numberOfSessions];

        for (int i = 0; i < numberOfSessions; i++) {
            System.out.print("第" + (i + 1) + "公演のオープニング曲名（一部でも可）: ");
            openers[i] = scanner.nextLine().trim();
            System.out.print("第" + (i + 1) + "公演のトリ曲名（一部でも可）: ");
            closers[i] = scanner.nextLine().trim();
        }
        return new String[][] { openers, closers };
    }

    /**
     * 生成結果をコンソールに表示します。
     */
    private void printSessions(List<List<Performance>> generatedSessions) {
        System.out.println("\n=== 生成されたセットリスト ===");
        for (int i = 0; i < generatedSessions.size(); i++) {
            System.out.println("\n【第 " + (i + 1) + " 公演】");
            List<Performance> session = generatedSessions.get(i);
            List<String> warnings = collectConsecutiveWarnings(session, i + 1);

            for (int j = 0; j < session.size(); j++) {
                Performance performance = session.get(j);
                System.out.println("  " + (j + 1) + ". " + performance.getDisplayTitle()
                        + " (演者: " + String.join(", ", performance.getPerformers()) + ")");
            }

            if (!warnings.isEmpty()) {
                System.out.println("\n  --- 連続出演アラート ---");
                for (String warning : warnings) {
                    System.out.println(warning);
                }
            }
        }
    }

    /**
     * 連続出演している演者を検出します。
     */
    private List<String> collectConsecutiveWarnings(List<Performance> session, int sessionNumber) {
        Map<String, Integer> currentStreak = new HashMap<>();
        List<String> warnings = new ArrayList<>();

        for (Performance performance : session) {
            List<String> currentPerformers = performance.getPerformers();
            for (String performer : currentPerformers) {
                currentStreak.put(performer, currentStreak.getOrDefault(performer, 0) + 1);
            }

            for (String previousPerformer : new ArrayList<>(currentStreak.keySet())) {
                if (!currentPerformers.contains(previousPerformer)) {
                    addWarningIfNeeded(warnings, previousPerformer, currentStreak.get(previousPerformer), sessionNumber);
                    currentStreak.put(previousPerformer, 0);
                }
            }
        }

        for (Map.Entry<String, Integer> entry : currentStreak.entrySet()) {
            addWarningIfNeeded(warnings, entry.getKey(), entry.getValue(), sessionNumber);
        }
        return warnings;
    }

    /**
     * 3連続以上の出演がある場合だけ警告を追加します。
     */
    private void addWarningIfNeeded(List<String> warnings, String performer, int streak, int sessionNumber) {
        if (streak >= 3 && !Performance.NO_PERFORMER.equals(performer)) {
            warnings.add("  警告: " + performer + "さんが第" + sessionNumber + "公演で " + streak + " 連続出演しています");
        }
    }

    /**
     * 生成したセットリストをファイルへ保存します。
     */
    private void exportSessions(List<List<Performance>> generatedSessions) {
        System.out.println("\n=== 出力処理 ===");
        System.out.println("出力するファイル形式を選択してください。");
        System.out.println("1: Excelファイル (.xlsx)  ※各公演をシートごとに分割して出力します");
        System.out.println("2: CSVファイル (.csv)   ※1つのファイルに全公演を連続して出力します");

        int exportChoice = readMenuChoice("番号を入力：", 1, 2);
        DataExporter exporter;
        String fileName;
        if (exportChoice == 2) {
            exporter = new CsvDataExporter();
            fileName = CSV_OUTPUT_FILE;
        } else {
            exporter = new ExcelDataExporter();
            fileName = EXCEL_OUTPUT_FILE;
        }
        exporter.export(generatedSessions, fileName);
    }

    /**
     * 実行日時を表示します。
     */
    private void printExecutionInfo() {
        System.out.println("\n--- 実行情報 ---");
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss");
        System.out.println("最終処理日時：" + now.format(formatter));
        System.out.println("\n実行に成功しました");
    }

    /**
     * 指定範囲内のメニュー番号を読み取ります。
     */
    private int readMenuChoice(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            try {
                int value = Integer.parseInt(scanner.nextLine());
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException e) {
                // ループして再入力を促す
            }
            System.out.println(min + "から" + max + "までの番号を入力してください。");
        }
    }

    /**
     * 1以上の整数を読み取ります。
     */
    private int readPositiveInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                int value = Integer.parseInt(scanner.nextLine());
                if (value > 0) {
                    return value;
                }
            } catch (NumberFormatException e) {
                // ループして再入力を促す
            }
            System.out.println("自然数を入力してください。");
        }
    }
}
