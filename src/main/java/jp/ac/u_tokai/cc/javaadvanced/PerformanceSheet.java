package jp.ac.u_tokai.cc.javaadvanced;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/** 入力ファイル内の1シートと、そのシートに属する演目を表します。 */
public record PerformanceSheet(
        String name,
        List<Performance> performances,
        List<String> performerNames) {

    /** 演目から演者一覧を導出する互換コンストラクタです。 */
    public PerformanceSheet(String name, List<Performance> performances) {
        this(name, performances, derivePerformerNames(performances));
    }

    /** シート名と演目一覧を検証し、不変な値として保持します。 */
    public PerformanceSheet {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("sheet name must not be blank");
        }
        name = name.trim();
        Objects.requireNonNull(performances, "performances must not be null");
        performances = List.copyOf(performances);
        if (performances.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("performances must not contain null");
        }
        performerNames = mergePerformerNames(performerNames, performances);
    }

    private static List<String> derivePerformerNames(List<Performance> performances) {
        Objects.requireNonNull(performances, "performances must not be null");
        return mergePerformerNames(List.of(), performances);
    }

    private static List<String> mergePerformerNames(
            List<String> preferredNames, List<Performance> performances) {
        Objects.requireNonNull(preferredNames, "performerNames must not be null");
        Objects.requireNonNull(performances, "performances must not be null");
        LinkedHashSet<String> names = new LinkedHashSet<>();
        preferredNames.stream()
                .map(value -> value == null ? "" : value.trim())
                .filter(value -> !value.isEmpty())
                .filter(value -> !Performance.NO_PERFORMER.equals(value))
                .forEach(names::add);
        performances.stream()
                .filter(Objects::nonNull)
                .flatMap(performance -> performance.getPerformers().stream())
                .map(value -> value == null ? "" : value.trim())
                .filter(value -> !value.isEmpty())
                .filter(value -> !Performance.NO_PERFORMER.equals(value))
                .forEach(names::add);
        return List.copyOf(names);
    }
}
