package jp.ac.u_tokai.cc.javaadvanced;

import java.util.List;

/**
 * 演目(Performance)を継承、自動生成に必要な要素を持つ楽曲用のクラス
 */

public class Song extends Performance {
    // 楽曲のテンポ(BPM)
    private int BPM;
    // 作品の雰囲気(例：激しい、しっとり)
    private String mood;

    /**
     * コンストラクタ
     * 
     * @param title     演目名
     * @param performer 演者名
     * @param duration  所要時間
     * @param BPM       テンポ
     * @param mood      雰囲気
     */
    public Song(String title, List<String> performers, int duration, int BPM, String mood) {
        // 親クラス(Performance.java)に基本情報を渡して安全に初期化
        super(title, performers, duration);

        // BPMが常に非負整数であり、負整数の場合に"1"としてカプセル化
        if (BPM <= 0) {
            this.BPM = 1;
        } else {
            this.BPM = BPM;
        }

        /*
         * 雰囲気が未入力の場合、空文字””に指定
         * 空文字の場合のみ「連続した作品の雰囲気を禁止」というルールの対象外にするひつようがあるけど一旦このまま
         */
        if (mood == null || mood.isEmpty()) {
            this.mood = "未指定";
        } else {
            this.mood = mood;
        }
    }

    /**
     * 親クラス(Performance.java)のメソッドを上書き、自動生成用のパラメタも一緒に表示
     */
    @Override
    public void show() {
        super.show(); // 親クラスの表示(タイトル、演者、時間)を呼び出す
        // 楽曲独自のデータ(テンポ、雰囲気)を付け足す
        System.out.println("    【楽曲情報】BPM:" + this.BPM + ",雰囲気:" + this.mood);
    }

    /**
     * 楽曲のBPMの取得
     * 
     * @return BPM
     */
    public int getBPM() {
        return this.BPM;
    }

    /**
     * 楽曲の雰囲気の取得
     * 
     * @return 雰囲気
     */
    public String getMood() {
        return this.mood;
    }
}