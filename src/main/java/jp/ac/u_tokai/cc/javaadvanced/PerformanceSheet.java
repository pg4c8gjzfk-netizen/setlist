package jp.ac.u_tokai.cc.javaadvanced;

import java.util.List;
import java.util.Objects;

/** 入力ファイル内の1シートと、そのシートに属する演目を表します。 */
public record PerformanceSheet(String name, List<Performance> performances) {

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
    }
}
