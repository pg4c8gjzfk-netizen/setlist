package jp.ac.u_tokai.cc.javaadvanced;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/** XLSXの読込・シート単位生成・XLSX出力を行うコンソール版です。 */
public class ConsoleSetlistApplication {

    private static final String DATA_DIRECTORY = "Data";
    private static final String OUTPUT_FILE = "output_setlist.xlsx";

    private final Scanner scanner;
    private final PerformanceSearcher searcher;
    private final SetlistGenerator generator;

    /** 標準入力を使うコンソールアプリとして初期化します。 */
    public ConsoleSetlistApplication() {
        this.scanner = new Scanner(System.in);
        this.searcher = new PerformanceSearcher();
        this.generator = new SetlistGenerator();
    }

    /** アプリケーション全体の流れを実行します。 */
    public void run() {
        try {
            System.out.println("=== セットリスト自動作成 ===\n");

            File selectedFile = chooseDataFile();
            if (selectedFile == null) {
                return;
            }

            List<PerformanceSheet> sourceSheets;
            try {
                sourceSheets = new XlsxPerformanceReader().loadSheets(selectedFile);
            } catch (IllegalArgumentException exception) {
                System.out.println("XLSXを読み込めませんでした。\n" + exception.getMessage());
                return;
            }
            if (sourceSheets.isEmpty()) {
                System.out.println("読み込める公演シートがありませんでした。プログラムを終了します。");
                return;
            }

            int performanceCount = sourceSheets.stream()
                    .mapToInt(sheet -> sheet.performances().size())
                    .sum();
            System.out.println("\n>>> 読み込み完了: " + sourceSheets.size()
                    + "公演、全" + performanceCount + "件の演目を登録しました。");
            searchPerformances(toSearchMap(sourceSheets));

            String[] fixedSongs = readFixedSongs();
            List<PerformanceSheet> generatedSheets = generator.generateWithinSheets(
                    sourceSheets, fixedSongs[0], fixedSongs[1]);

            printSheets(generatedSheets);
            exportSheets(generatedSheets);
            printExecutionInfo();
        } finally {
            scanner.close();
        }
    }

    /** Dataフォルダ内から読み込むXLSXを選択します。 */
    private File chooseDataFile() {
        File dataDirectory = new File(DATA_DIRECTORY);
        File[] targetFiles = dataDirectory.listFiles((directory, name) ->
                name.toLowerCase(java.util.Locale.ROOT).endsWith(".xlsx"));

        if (targetFiles == null || targetFiles.length == 0) {
            System.out.println("Dataフォルダ内にXLSXファイルが見つかりません。プログラムを終了します。");
            return null;
        }

        Arrays.sort(targetFiles, java.util.Comparator.comparing(File::getName));
        System.out.println(">>> 読み込むXLSXファイルを選択してください。");
        for (int index = 0; index < targetFiles.length; index++) {
            System.out.println((index + 1) + ": " + targetFiles[index].getName());
        }

        int choice = readMenuChoice("番号を入力：", 1, targetFiles.length);
        File selectedFile = targetFiles[choice - 1];
        System.out.println("\n>>> 「" + selectedFile.getName() + "」を読み込みます...");
        return selectedFile;
    }

    private Map<String, Performance> toSearchMap(List<PerformanceSheet> sheets) {
        Map<String, Performance> searchablePerformances = new LinkedHashMap<>();
        int sequence = 1;
        for (PerformanceSheet sheet : sheets) {
            for (Performance performance : sheet.performances()) {
                String searchKey = performance.getDisplayTitle()
                        + " [" + sheet.name() + " #" + sequence + "]";
                searchablePerformances.put(searchKey, performance);
                sequence++;
            }
        }
        return searchablePerformances;
    }

    /** 演目検索を実行します。 */
    private void searchPerformances(Map<String, Performance> performanceMap) {
        System.out.println("\n--- 演目の検索 ---");
        System.out.print("検索したい演目名を入力してください：");
        searcher.searchAndDisplay(performanceMap, scanner.nextLine());
    }

    /** 全シート共通のオープニング候補とトリ候補を読み取ります。 */
    private String[] readFixedSongs() {
        System.out.println("\n--- オープニング・トリの設定 ---");
        System.out.println("入力した曲名が存在する各シート内で、先頭または末尾へ配置します。");
        System.out.println("指定しない場合はそのままEnterを押してください。");
        System.out.print("オープニング曲名（一部でも可）: ");
        String opener = scanner.nextLine().trim();
        System.out.print("トリ曲名（一部でも可）: ");
        String closer = scanner.nextLine().trim();
        return new String[] {opener, closer};
    }

    /** 生成結果を元シート名のまま表示します。 */
    private void printSheets(List<PerformanceSheet> generatedSheets) {
        System.out.println("\n=== 生成されたセットリスト ===");
        for (PerformanceSheet sheet : generatedSheets) {
            System.out.println("\n【" + sheet.name() + "】");
            List<Performance> performances = sheet.performances();
            List<String> warnings = collectConsecutiveWarnings(performances, sheet.name());

            for (int index = 0; index < performances.size(); index++) {
                Performance performance = performances.get(index);
                System.out.println("  " + (index + 1) + ". " + performance.getDisplayTitle()
                        + " (演者: " + String.join(", ", performance.getPerformers()) + ")");
            }

            if (!warnings.isEmpty()) {
                System.out.println("\n  --- 連続出演アラート ---");
                warnings.forEach(System.out::println);
            }
        }
    }

    /** 連続出演している演者を検出します。 */
    private List<String> collectConsecutiveWarnings(List<Performance> performances, String sheetName) {
        Map<String, Integer> currentStreak = new java.util.HashMap<>();
        List<String> warnings = new ArrayList<>();

        for (Performance performance : performances) {
            List<String> currentPerformers = performance.getPerformers();
            for (String performer : currentPerformers) {
                currentStreak.put(performer, currentStreak.getOrDefault(performer, 0) + 1);
            }

            for (String previousPerformer : new ArrayList<>(currentStreak.keySet())) {
                if (!currentPerformers.contains(previousPerformer)) {
                    addWarningIfNeeded(
                            warnings, previousPerformer, currentStreak.get(previousPerformer), sheetName);
                    currentStreak.put(previousPerformer, 0);
                }
            }
        }

        for (Map.Entry<String, Integer> entry : currentStreak.entrySet()) {
            addWarningIfNeeded(warnings, entry.getKey(), entry.getValue(), sheetName);
        }
        return warnings;
    }

    private void addWarningIfNeeded(
            List<String> warnings, String performer, int streak, String sheetName) {
        if (streak >= 3 && !Performance.NO_PERFORMER.equals(performer)) {
            warnings.add("  警告: " + performer + "さんが「" + sheetName
                    + "」で " + streak + " 連続出演しています");
        }
    }

    /** 生成結果をXLSXへ保存します。 */
    private void exportSheets(List<PerformanceSheet> generatedSheets) {
        try {
            File outputFile = new XlsxSetlistExporter().export(generatedSheets, OUTPUT_FILE);
            System.out.println("\n>>> 保存先: " + outputFile.getAbsolutePath());
        } catch (IOException | IllegalArgumentException exception) {
            throw new IllegalStateException("XLSXの保存に失敗しました。", exception);
        }
    }

    /** 実行日時を表示します。 */
    private void printExecutionInfo() {
        System.out.println("\n--- 実行情報 ---");
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss");
        System.out.println("最終処理日時：" + now.format(formatter));
        System.out.println("\n実行に成功しました");
    }

    /** 指定範囲内のメニュー番号を読み取ります。 */
    private int readMenuChoice(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            try {
                int value = Integer.parseInt(scanner.nextLine());
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException exception) {
                // ループして再入力を促す
            }
            System.out.println(min + "から" + max + "までの番号を入力してください。");
        }
    }
}
