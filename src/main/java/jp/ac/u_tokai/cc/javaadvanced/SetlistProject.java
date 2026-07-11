package jp.ac.u_tokai.cc.javaadvanced;

import java.util.List;
import java.util.Objects;

/**
 * 編集・再生成の対象となるセットリスト全体です。
 *
 * @param sessions 公演単位のセットリスト
 * @param sheetBoundariesLocked XLSXの元シート境界を変更できない場合は {@code true}
 */
public record SetlistProject(List<SetlistSession> sessions, boolean sheetBoundariesLocked) {

    /** シート境界を持たない手入力・CSV向けプロジェクトを作成します。 */
    public SetlistProject(List<SetlistSession> sessions) {
        this(sessions, false);
    }

    /**
     * セットリストを初期化します。
     */
    public SetlistProject {
        Objects.requireNonNull(sessions, "sessions must not be null");
        if (sessions.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("sessions must not contain null");
        }
        sessions = List.copyOf(sessions);
    }
}
