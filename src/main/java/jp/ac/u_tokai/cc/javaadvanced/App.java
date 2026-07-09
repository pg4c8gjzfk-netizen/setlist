package jp.ac.u_tokai.cc.javaadvanced;

/**
 * セットリスト自動作成アプリケーションのメインクラス
 *
 */

// Map機能を使用するために汎用クラスをインポートする
import java.util.HashMap;
import java.util.Map;
// 検索事項をUserに入力してもらうためのクラスのインポート
import java.util.Scanner;
// 時刻を扱うためのライブラリをインポート
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
// フォルダ内のファイルを探すためのライブラリ
import java.io.File;

public class App {
    /**
     * アプリケーションの起動エントリポイント
     * 
     * @param args コマンドライン引数
     */
    public static void main(String[] args) {

        System.out.println("=== セットリスト自動作成 ===\n");

        int numberOfSessions = 0;
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            System.out.println("作成する公演回数を入力してください。：");
            try {
                numberOfSessions = Integer.parseInt(scanner.nextLine());
                if (numberOfSessions > 0) {
                    break;
                } else {
                    System.out.println("自然数を入力してください。");
                }
            } catch (NumberFormatException e) {
                System.out.println("※正しい数値を入力してください。");
            }
        }

        // Mapを用いて演目を整理する
        // Keyを「楽曲名/Title (String)」,Valueを「Performer」、「duration」などとするMapを用意
        // 親クラスで宣言する理由→子クラスでも一緒に保管可能(多態性)
        Map<String, Performance> performanceMap = new HashMap<>();

        // .csvファイルから自動で読み込むコード
        File dataDir = new File("Data"); // Dataフォルダを指定

        // フォルダ内から「.csv」、または「.xlsx」で終わるファイルだけを集める
        File[] targetFiles = dataDir.listFiles((dir, name) -> name.endsWith(".csv") || name.endsWith(".xlsx"));

        // フォルダが無い、.csvファイルがフォルダ内も1つもない場合のエラー処理
        if (targetFiles == null || targetFiles.length == 0) {
            System.out.println("Dataフォルダ内に.csvファイルが見つかりません。プログラムを終了します。");
            return; // ここで処理を終了する。
        }

        System.out.println(">>> 読み込むファイルを選択してください。");
        for (int i = 0; i < targetFiles.length; i++) {
            // 「1: performances.csv」のように番号付きで表示
            System.out.println((i + 1) + ": " + targetFiles[i].getName());
        }

        // 表示した選択肢の番号を受け付ける
        System.out.print("番号を入力：");
        int choice = scanner.nextInt();
        scanner.nextLine(); // 数字を入力した後の「Enterキー(改行)」を空読みして捨てる

        // 入力された番号(１から始まる)を、配列のインデックス(0からはじまる)に直してファイルを取得
        File selectedFile = targetFiles[choice - 1];
        System.out.println("\n>>> 「" + selectedFile.getName() + "」を読み込みます...");

        // 選ばれたファイルの拡張子を見て、対応するクラスを呼び出す
        DataLoader loader;
        if (selectedFile.getName().endsWith(".xlsx")) {
            loader = new DataLoader_Excel();
        } else {
            loader = new DataLoader_csv();
        }

        performanceMap = loader.load(selectedFile);

        System.out.println(">>>読み込み完了 全" + performanceMap.size() + "件の演目をMapに登録しました。");

        // Scannerクラスを使った対話型の検索機能
        System.out.println("\n--- 演目の検索 ---");

        // Userに入力を促すメッセージを表示
        System.out.print("検索したい演目名を入力してください：");

        // Userが入力した文字列を searchKey に代入
        String searchKey = scanner.nextLine();

        System.out.println("\n検索中... 「" + searchKey + "」");

        // 検索クラスを呼び出して、検索と表示を任せる
        DataSearcher searcher = new DataSearcher();
        searcher.searchAndDisplay(performanceMap, searchKey);

        // ==== 各公演の演目数設定 ====
        System.out.println("\n--- 各公演の演目数設定 ---");
        System.out.println("1: 設定しない（全曲を均等に自動配分）");
        System.out.println("2: 全公演一括で同じ演目数を設定する");
        System.out.println("3: 公演ごとに個別に演目数を設定する");
        System.out.print("番号を入力：");
        
        int capacityChoice = 1;
        try {
            capacityChoice = Integer.parseInt(scanner.nextLine());
        } catch (Exception e) {}

        int[] capacities = null;
        if (capacityChoice == 2) {
            capacities = new int[numberOfSessions];
            System.out.print("1公演あたりの演目数を入力してください：");
            int cap = 9999;
            try { cap = Integer.parseInt(scanner.nextLine()); } catch(Exception e) {}
            for(int i = 0; i < numberOfSessions; i++) {
                capacities[i] = cap;
            }
        } else if (capacityChoice == 3) {
            capacities = new int[numberOfSessions];
            for (int i = 0; i < numberOfSessions; i++) {
                System.out.print("第" + (i + 1) + "公演の演目数を入力してください：");
                int cap = 9999;
                try { cap = Integer.parseInt(scanner.nextLine()); } catch(Exception e) {}
                capacities[i] = cap;
            }
        }

        // ==== オープニング・トリの設定 ====
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

        // ==== セットリスト生成機能の呼び出し ====
        SetlistGenerator generator = new SetlistGenerator();
        java.util.List<java.util.List<Performance>> generatedSessions = generator.generate(performanceMap, numberOfSessions, capacities, openers, closers);

        // コンソールに結果を出力して確認
        System.out.println("\n=== 生成されたセットリスト ===");
        for (int i = 0; i < generatedSessions.size(); i++) {
            System.out.println("\n【第 " + (i + 1) + " 公演】");
            java.util.List<Performance> session = generatedSessions.get(i);
            
            // 連続出演カウント用
            Map<String, Integer> currentStreak = new HashMap<>();
            java.util.List<String> warnings = new java.util.ArrayList<>();

            for (int j = 0; j < session.size(); j++) {
                Performance p = session.get(j);
                java.util.List<String> currentPerformers = p.getPerformers();

                // 出演している人の連続記録を+1する
                for (String performer : currentPerformers) {
                    currentStreak.put(performer, currentStreak.getOrDefault(performer, 0) + 1);
                }

                // 出演していない人の連続記録が途切れるので、そのタイミングで3連続以上なら警告を保存しリセット
                for (String previousPerformer : new java.util.ArrayList<>(currentStreak.keySet())) {
                    if (!currentPerformers.contains(previousPerformer)) {
                        int streak = currentStreak.get(previousPerformer);
                        if (streak >= 3) {
                            warnings.add("  ⚠️ " + previousPerformer + "さんが第" + (i + 1) + "公演で " + streak + " 連続出演しています");
                        }
                        currentStreak.put(previousPerformer, 0); // 記録リセット
                    }
                }

                // コンソール表示時も余計な "(_" などを消して綺麗に見せる
                String displayTitle = p.getTitle().split("\\(_")[0];
                System.out.println("  " + (j + 1) + ". " + displayTitle + " (演者: " + String.join(", ", currentPerformers) + ")");
            }

            // その公演の最後まで出続けた人の最終チェック
            for (String performer : currentStreak.keySet()) {
                int streak = currentStreak.get(performer);
                if (streak >= 3) {
                    warnings.add("  ⚠️ " + performer + "さんが第" + (i + 1) + "公演で " + streak + " 連続出演しています");
                }
            }

            // 警告があれば一覧表示
            if (!warnings.isEmpty()) {
                System.out.println("\n  --- 🚨 連続出演アラート ---");
                for (String w : warnings) {
                    System.out.println(w);
                }
            }
        }

        // ==== 出力形式の選択とファイル保存 ====
        System.out.println("\n=== 出力処理 ===");
        System.out.println("出力するファイル形式を選択してください。");
        System.out.println("1: Excelファイル (.xlsx)  ※各公演をシートごとに分割して出力します");
        System.out.println("2: CSVファイル (.csv)   ※1つのファイルに全公演を連続して出力します");
        System.out.print("番号を入力：");
        
        int exportChoice = 1;
        try {
            exportChoice = scanner.nextInt();
            scanner.nextLine(); // 空読み
        } catch (Exception e) {
            System.out.println("※無効な入力のため、デフォルトのExcel形式で出力します。");
            scanner.nextLine();
        }

        if (exportChoice == 2) {
            DataExporter_csv exporter = new DataExporter_csv();
            exporter.export(generatedSessions, "output_setlist.csv");
        } else {
            DataExporter_Excel exporter = new DataExporter_Excel();
            exporter.export(generatedSessions, "output_setlist.xlsx");
        }
        // =======================================

        scanner.close();

        // java.time パッケージを活用したタイムスタンプ機能
        System.out.println("\n--- 実行情報 ---");

        // 現在の時刻を取得（クラスメソッドの活用）
        LocalDateTime now = LocalDateTime.now();

        // わかりやすいフォーマットに変更
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss");

        // 指定したフォーマットで時刻を文字列に変換して出力(インスタンスメソッドの活用)
        String formattedNow = now.format(formatter);
        System.out.println("最終処理日時：" + formattedNow);

        System.out.println("\n実行に成功");

    }
}