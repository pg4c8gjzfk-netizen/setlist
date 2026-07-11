package jp.ac.u_tokai.cc.javaadvanced;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * 1公演分のセットリストです。
 */
public record SetlistSession(
        String name,
        List<SetlistEntry> entries,
        List<String> performerNames) {

    /** 演目から演者一覧を導出する互換コンストラクタです。 */
    public SetlistSession(String name, List<SetlistEntry> entries) {
        this(name, entries, List.of());
    }

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
        performerNames = mergePerformerNames(performerNames, entries);
    }

    private static List<String> mergePerformerNames(
            List<String> preferredNames, List<SetlistEntry> entries) {
        Objects.requireNonNull(preferredNames, "performerNames must not be null");
        LinkedHashSet<String> names = new LinkedHashSet<>();
        preferredNames.stream()
                .map(value -> value == null ? "" : value.trim())
                .filter(value -> !value.isEmpty())
                .filter(value -> !Performance.NO_PERFORMER.equals(value))
                .forEach(names::add);
        entries.stream()
                .flatMap(entry -> entry.performers().stream())
                .map(value -> value == null ? "" : value.trim())
                .filter(value -> !value.isEmpty())
                .filter(value -> !Performance.NO_PERFORMER.equals(value))
                .forEach(names::add);
        return List.copyOf(names);
    }
}
