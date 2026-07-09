package jp.ac.u_tokai.cc.javaadvanced;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * セットリストで扱う1つの演目を管理するクラス。
 */
public class Performance {
    /** 出演者未設定時に使う表示名。 */
    public static final String NO_PERFORMER = "noPerformer";

    /** 演目名 */
    private final String title;
    /** 出演者名 */
    private final List<String> performers;
    /** 演目の所要時間（単位：秒） */
    private final int duration;

    /**
     * 演目を初期化します。
     *
     * @param title      演目名
     * @param performers 演者名一覧
     * @param duration   所要時間（秒）
     */
    public Performance(String title, List<String> performers, int duration) {
        if (title == null || title.trim().isEmpty()) {
            this.title = "Untitled";
        } else {
            this.title = title.trim();
        }

        if (performers == null || performers.isEmpty()) {
            this.performers = new ArrayList<>();
            this.performers.add(NO_PERFORMER);
        } else {
            this.performers = new ArrayList<>(performers);
        }

        this.duration = Math.max(duration, 0);
    }

    /**
     * 演目情報を表示します。
     */
    public void show() {
        System.out.println("\n演目:" + this.title);
        System.out.println("時間：" + formatDuration());
        System.out.println("演者:" + String.join(",", this.performers));
    }

    /**
     * 演目名を取得します。
     *
     * @return 演目名
     */
    public String getTitle() {
        return this.title;
    }

    /**
     * 表示用の演目名を取得します。
     *
     * @return 派生名を除いた演目名
     */
    public String getDisplayTitle() {
        return this.title.split("\\(_")[0];
    }

    /**
     * 演者名を取得します。
     *
     * @return 演者名一覧
     */
    public List<String> getPerformers() {
        return Collections.unmodifiableList(this.performers);
    }

    /**
     * 所要時間を取得します。
     *
     * @return 所要時間（秒）
     */
    public int getDuration() {
        return this.duration;
    }

    /**
     * 所要時間を分:秒の形式に変換します。
     *
     * @return 分:秒の文字列
     */
    private String formatDuration() {
        int minutes = this.duration / 60;
        int seconds = this.duration % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}