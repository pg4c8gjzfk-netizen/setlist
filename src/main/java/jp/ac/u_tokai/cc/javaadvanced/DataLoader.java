package jp.ac.u_tokai.cc.javaadvanced;

import java.io.File;
import java.util.Map;

/**
 * ファイルから演目データを読み込むための共通インターフェース。
 */
public interface DataLoader {
    /**
     * 指定されたファイルから演目データを読み込みます。
     *
     * @param file 読み込み対象のファイル
     * @return タイトルをキー、演目(Performance)を値とするMap
     */
    Map<String, Performance> load(File file);
}
