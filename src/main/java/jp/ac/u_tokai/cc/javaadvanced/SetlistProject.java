package jp.ac.u_tokai.cc.javaadvanced;

import java.util.List;
import java.util.Objects;

/**
 * 編集・再生成の対象となるセットリスト全体です。
 */
public record SetlistProject(List<SetlistSession> sessions) {

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
