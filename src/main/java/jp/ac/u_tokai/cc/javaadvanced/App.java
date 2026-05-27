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
// .csvファイルの読み込みを扱うためのライブラリ
import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.io.File;

public class App {
    /**
     * アプリケーションの起動エントリポイント
     * 
     * @param args コマンドライン引数
     */
    public static void main(String[] args) {

        System.out.println("=== セットリスト自動作成アプリ：CSVデータ読み込みテスト ===\n");

        // Mapを用いて演目を整理する
        // Keyを「楽曲名/Title (String)」,Valueを「Performer」、「duration」などとするMapを用意
        // 親クラスで宣言する理由→子クラスでも一緒に保管可能(多態性)
        Map<String, Performance> performanceMap = new HashMap<>();

        // .csvファイルから自動で読み込むコード
        File dataDir = new File("Data"); // Dataフォルダを指定

        // フォルダ内から「.csv」で終わるファイルだけを集める
        File[] csvFiles = dataDir.listFiles((dir, name) -> name.endsWith(".csv"));

        // フォルダが無い、.csvファイルがフォルダ内も1つもない場合のエラー処理
        if (csvFiles == null || csvFiles.length == 0) {
            System.out.println("Dataフォルダ内に.csvファイルが見つかりません。プログラムを終了します。");
            return; // ここで処理を終了する。
        }

        System.out.println(">>> 読み込む.csvファイルを選択してください。");
        for (int i = 0; i < csvFiles.length; i++) {
            // 「1: performances.csv」のように番号付きで表示
            System.out.println((i + 1) + ": " + csvFiles[i].getName());
        }

        // 表示した選択肢の番号を受け付ける
        Scanner scanner = new Scanner(System.in);
        System.out.print("番号を入力：");
        int choice = scanner.nextInt();
        scanner.nextLine(); // 数字を入力した後の「Enterキー(改行)」を空読みして捨てる

        // 入力された番号(１から始まる)を、配列のインデックス(0からはじまる)に直してファイルを取得
        File selectedFile = csvFiles[choice - 1];
        System.out.println("\n>>> 「" + selectedFile.getName() + "」を読み込みます...");

        Path csvPath = selectedFile.toPath();

        try (BufferedReader br = Files.newBufferedReader(csvPath)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");
                if (data.length >= 5) {
                    String title = data[0];
                    String performer = data[1];
                    int duration = Integer.parseInt(data[2]);
                    int bpm = Integer.parseInt(data[3]);
                    String mood = data[4];

                    Song song = new Song(title, performer, duration, bpm, mood);
                    performanceMap.put(song.getTitle(), song);
                }
            }
            System.out.println(">>> 読み込み完了！ 全" + performanceMap.size() + "件の演目をMapに登録しました。");

        } catch (IOException e) {
            System.out.println("エラー：.csvファイルの読み込みに失敗しました。ファイル名や場所を確認してください。");
        } catch (NumberFormatException e) {
            System.out.println("エラー：時間やBPMのデータを数値に変換できませんでした。.csvファイルの中身を確認してください。");
        }

        // Scannerクラスを使った対話型の検索機能
        System.out.println("\n--- 演目の検索 ---");

        // Userに入力を促すメッセージを表示
        System.out.print("検索したい演目名を入力してください：");

        // Userが入力した文字列を searchKey に代入
        String searchKey = scanner.nextLine();

        System.out.println("\n検索中... 「" + searchKey + "」");

        // 入力された文字(searchKey)を使ってMapを検索
        if (performanceMap.containsKey(searchKey)) {
            Performance target = performanceMap.get(searchKey);
            System.out.print("【検索ヒット】");
            target.show();
        } else {
            System.out.println("「" + searchKey + "」という演目は登録されていません。");
        }

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