package jp.ac.u_tokai.cc.javaadvanced;

import java.util.List;

/**
 * 演目(Performance)を継承し、自動生成に必要な楽曲情報を持つクラス。
 */
public class Song extends Performance {
    /** 楽曲のテンポ(BPM)。 */
    private final int bpm;
    /** 作品の雰囲気。 */
    private final String mood;

    /**
     * 楽曲を初期化します。
     *
     * @param title      演目名
     * @param performers 演者名一覧
     * @param duration   所要時間（秒）
     * @param bpm        テンポ
     * @param mood       雰囲気
     */
    public Song(String title, List<String> performers, int duration, int bpm, String mood) {
        super(title, performers, duration);
        this.bpm = Math.max(bpm, 1);
        if (mood == null || mood.trim().isEmpty()) {
            this.mood = "未指定";
        } else {
            this.mood = mood.trim();
        }
    }

    /**
     * 親クラスの表示に楽曲独自の情報を追加します。
     */
    @Override
    public void show() {
        super.show();
        System.out.println("    【楽曲情報】BPM:" + this.bpm + ",雰囲気:" + this.mood);
    }

    /**
     * 楽曲のBPMを取得します。
     *
     * @return BPM
     */
    public int getBPM() {
        return this.bpm;
    }

    /**
     * 楽曲の雰囲気を取得します。
     *
     * @return 雰囲気
     */
    public String getMood() {
        return this.mood;
    }
}