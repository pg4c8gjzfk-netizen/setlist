package jp.ac.u_tokai.cc.javaadvanced;

/**
 * セットリスト自動作成アプリケーションのメインクラス
 *
 */

//Map機能を使用するために汎用クラスをインポートする
import java.util.HashMap;
import java.util.Map;
//検索事項をUserに入力してもらうためのクラスのインポート
import java.util.Scanner;

public class App {
    /**
     * アプリケーションの起動エントリポイント
     * 
     * @param args コマンドライン引数
     */
    public static void main(String[] args) {

        System.out.println("=== セットリスト自動作成アプリ：Map機能によるデータ整理テスト ===\n");

        // 複数のクラスをインスタンス化（親クラス Performance）
        // 新しい要件に合わせて「作品名」「演者」「時間」の3つのデータを渡します。
        Performance p = new Performance("セトリ自動作成アプリのデモ", "Hikaru", 5);

        // インスタンス化
        // 楽曲専用の要件である「テンポ（BPM）」と「雰囲気」を追加
        Song song1 = new Song("test1", "Tsukato", 15, 160, "激しい");

        // 未入力を許容するカプセル化のテスト
        // 雰囲気を ""（空文字）に設定し、内部で「未指定」に自動変換されるかのテスト
        Song song2 = new Song("test2", "Tomo", 10, 80, "");

        // Mapを用いて演目を整理する
        // Keyを「楽曲名/Title (String)」,Valueを「Performer」、「duration」などとするMapを用意
        // 親クラスで宣言する理由→子クラスでも一緒に保管可能(多態性)
        Map<String, Performance> performanceMap = new HashMap<>();

        // titleとデータをペアにしてMapに登録
        performanceMap.put(p.getTitle(), p);
        performanceMap.put(song1.getTitle(), song1);
        performanceMap.put(song2.getTitle(), song2);

        System.out.println(">>> 演目をMapに登録しました。全" + performanceMap.size() + "件");

        //Scannerクラスを使った対話型の検索機能
        System.out.println("\n--- 演目の検索 ---");

        // キーボード入力を受け取るためのScannerインスタンスを作成
        Scanner scanner = new Scanner(System.in);

        //Userに入力を促すメッセージを表示
        System.out.print("検索したい演目名を入力してください：");

        //Userが入力した文字列を searchKey に代入
        String searchKey = scanner.nextLine();

        System.out.println("\n検索中... 「"  + searchKey + "」");

        // 入力された文字(searchKey)を使ってMapを検索
        if (performanceMap.containsKey(searchKey)) {
            Performance target = performanceMap.get(searchKey);
            System.out.print("【検索ヒット】");
            target.show();
        } else {
            System.out.println("「" + searchKey + "」という演目は登録されていません。");
        }

        scanner.close();

        System.out.println("\n実行に成功");

    }
}