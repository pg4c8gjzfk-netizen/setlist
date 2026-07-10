package jp.ac.u_tokai.cc.javaadvanced;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * ファイルから演目データを読み込むための共通インターフェース。
 */
public interface PerformanceDataReader {
    /**
     * 指定されたファイルから演目データを読み込みます。
     *
     * @param file 読み込み対象のファイル
     * @return タイトルをキー、演目(Performance)を値とするMap
     */
    Map<String, Performance> load(File file);

    /**
     * シート境界を保持して演目を読み込みます。
     *
     * <p>シートを持たない形式は、従来どおり「未割り当て」の1グループとして扱います。</p>
     *
     * @param file 読み込み対象のファイル
     * @return シート単位の演目一覧
     */
    default List<PerformanceSheet> loadSheets(File file) {
        Map<String, Performance> performances = load(file);
        if (performances.isEmpty()) {
            return List.of();
        }
        return List.of(new PerformanceSheet("未割り当て", List.copyOf(performances.values())));
    }
}
