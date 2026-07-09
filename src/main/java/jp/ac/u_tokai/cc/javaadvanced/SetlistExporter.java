package jp.ac.u_tokai.cc.javaadvanced;

import java.util.List;

/**
 * 生成されたセットリストを外部ファイルへ保存するための共通インターフェース。
 */
public interface SetlistExporter {
    /**
     * セットリストを指定されたファイル名で保存します。
     *
     * @param sessions 生成されたセットリスト
     * @param fileName 出力するファイル名
     */
    void export(List<List<Performance>> sessions, String fileName);
}
