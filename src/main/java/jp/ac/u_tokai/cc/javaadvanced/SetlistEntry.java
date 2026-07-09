package jp.ac.u_tokai.cc.javaadvanced;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 公演内で扱う1つの演目行です。
 *
 * <p>元の演目情報と、公演ごとの出演者変更・固定状態を分離して保持します。</p>
 */
public record SetlistEntry(
        UUID id,
        UUID sourcePerformanceId,
        String title,
        int durationSeconds,
        List<String> performers,
        boolean fixed,
        FixedPosition fixedPosition,
        int fixedIndex) {

    /**
     * 演目行を初期化します。
     */
    public SetlistEntry {
        id = Objects.requireNonNull(id, "id must not be null");
        sourcePerformanceId = Objects.requireNonNull(
                sourcePerformanceId, "sourcePerformanceId must not be null");
        title = normalizeTitle(title);
        if (durationSeconds < 0) {
            throw new IllegalArgumentException("durationSeconds must not be negative");
        }
        performers = normalizePerformers(performers);
        fixedPosition = Objects.requireNonNull(fixedPosition, "fixedPosition must not be null");

        if (fixedPosition == FixedPosition.INDEX && fixedIndex < 0) {
            throw new IllegalArgumentException("fixedIndex must be non-negative for INDEX");
        }
        if (fixedPosition != FixedPosition.INDEX && fixedIndex != -1) {
            throw new IllegalArgumentException("fixedIndex must be -1 unless position is INDEX");
        }
    }

    /**
     * 元の演目から、未固定の演目行を作成します。
     *
     * @param sourcePerformanceId 元の演目を識別するID
     * @param performance 元の演目
     * @return 未固定の演目行
     */
    public static SetlistEntry fromPerformance(UUID sourcePerformanceId, Performance performance) {
        Objects.requireNonNull(performance, "performance must not be null");
        return new SetlistEntry(
                UUID.randomUUID(),
                sourcePerformanceId,
                performance.getDisplayTitle(),
                performance.getDuration(),
                performance.getPerformers(),
                false,
                FixedPosition.NONE,
                -1);
    }

    private static String normalizeTitle(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        return value.trim();
    }

    private static List<String> normalizePerformers(List<String> values) {
        Objects.requireNonNull(values, "performers must not be null");
        List<String> normalized = values.stream()
                .map(value -> value == null ? "" : value.trim())
                .filter(value -> !value.isEmpty())
                .toList();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("performers must not be empty");
        }
        return List.copyOf(normalized);
    }
}
