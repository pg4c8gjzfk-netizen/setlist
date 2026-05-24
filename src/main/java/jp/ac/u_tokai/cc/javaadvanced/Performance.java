package jp.ac.u_tokai.cc.javaadvanced;

/**
 * セットリストの演目を管理するクラス
 */

public class Performance {
    /** 演目名 */
    private String title;
    /** 出演者名 */
    private String performer;
    /** 演目の所要時間（単位：分） */
    private int duration;

    /**
     * コンストラクタ(カプセル化によるガード付き)
     * 
     * @param title     演目名
     * @param performer 演者名
     * @param duration  所要時間（分）
     */

    // 不正なデータをはじいてカプセル化を強化
    public Performance(String title, String performer, int duration) {
        // 演目名が未設定の場合の処理
        if (title == null || title.isEmpty()) {
            this.title = "Untitled";
        } else {
            this.title = title;
        }

        // 演者がいない場合の処理
        if (performer == null || performer.isEmpty()) {
            this.performer = "NoPerformer";
        } else {
            this.performer = performer;
        }

        // 所要時間が常に非負になるように補正
        if (duration < 0) {
            this.duration = 0;
        } else {
            this.duration = duration;
        }
    }

    /**
     * 演目情報を表示
     */
    public void show() {
        System.out.println();
        System.out.println("演目:" + this.title);
        System.out.println("時間:" + this.duration);
        System.out.println("演者:" + this.performer);
    }

    /**
     * タイトルの取得
     * 
     * @return 演目名
     */
    public String getTitle() {
        return this.title;
    }

    /**
     * 演者名の取得
     * 
     * @return 演者名
     */
    public String getPerformer() {
        return this.performer;
    }

    /**
     * 所要時間の取得
     * 
     * @return 所要時間（分）
     */
    public int getDuration() {
        return this.duration;
    }
}
