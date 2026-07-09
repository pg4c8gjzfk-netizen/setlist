package jp.ac.u_tokai.cc.javaadvanced;

import java.util.List;
import java.util.Objects;

/**
 * 1公演分のセットリストです。
 */
public record SetlistSession(String name, List<SetlistEntry> entries) {

    /**
     * 公演を初期化します。
     */
    public SetlistSession {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        name = name.trim();
        Objects.requireNonNull(entries, "entries must not be null");
        if (entries.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("entries must not contain null");
        }
        entries = List.copyOf(entries);
    }
}
