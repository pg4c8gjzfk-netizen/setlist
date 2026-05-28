package jp.ac.u_tokai.cc.javaadvanced;

import java.io.File;
import java.util.Map;

/**
 * データを読み込むための共通インターフェース
 */

public interface DataLoader {
    // 処理を書かず、メソッドの名前と戻り値の「ルール」だけを定義する。
    Map<String, Performance> load(File file);
}
