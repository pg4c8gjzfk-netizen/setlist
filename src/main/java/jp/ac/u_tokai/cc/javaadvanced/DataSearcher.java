package jp.ac.u_tokai.cc.javaadvanced;

import java.util.Map;

/**
 * 演目の検索に関する処理を専門に行うクラス
 */
public class DataSearcher {

    /**
     * Mapの中からキーワードに一致する演目を検索し、結果を表示する
     * * @param performanceMap 検索対象の全データが入ったMap
     * @param searchKey      検索するキーワード
     */
    public void searchAndDisplay(Map<String, Performance> performanceMap, String searchKey) {
        // キーワードが空っぽの場合は処理をしない
        if (searchKey == null || searchKey.trim().isEmpty()) {
            System.out.println("検索キーワードが入力されていません。");
            return;
        }

        boolean isFound = false;
        // 検索キーワードの前後の空白を消し、小文字に統一
        String normalizedQuery = searchKey.trim().toLowerCase();

        // Mapに登録されているすべての曲名を1つずつチェック
        for (String key : performanceMap.keySet()) {
            // 登録されている曲名も小文字にして、キーワードが含まれているかチェック
            if (key.toLowerCase().contains(normalizedQuery)) {
                Performance target = performanceMap.get(key);
                System.out.print("【検索ヒット】");
                target.show();
                isFound = true;
            }
        }

        // 1件もヒットしなかった場合
        if (!isFound) {
            System.out.println("「" + searchKey + "」を含む演目は見つかりませんでした。");
        }
    }
}