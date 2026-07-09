package jp.ac.u_tokai.cc.javaadvanced;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 演目の検索に関する処理を専門に行うクラス。
 */
public class PerformanceSearcher {

    /**
     * Mapの中からキーワードに一致する演目を検索します。
     *
     * @param performanceMap 検索対象の全データが入ったMap
     * @param searchKey      検索するキーワード
     * @return 検索に一致した演目の一覧
     */
    public List<Performance> findByTitle(Map<String, Performance> performanceMap, String searchKey) {
        List<Performance> results = new ArrayList<>();
        if (searchKey == null || searchKey.trim().isEmpty()) {
            return results;
        }

        String normalizedQuery = searchKey.trim().toLowerCase();
        for (Map.Entry<String, Performance> entry : performanceMap.entrySet()) {
            if (entry.getKey().toLowerCase().contains(normalizedQuery)) {
                results.add(entry.getValue());
            }
        }
        return results;
    }

    /**
     * 検索結果をコンソールに表示します。
     *
     * @param performanceMap 検索対象の全データが入ったMap
     * @param searchKey      検索するキーワード
     */
    public void searchAndDisplay(Map<String, Performance> performanceMap, String searchKey) {
        List<Performance> results = findByTitle(performanceMap, searchKey);
        if (searchKey == null || searchKey.trim().isEmpty()) {
            System.out.println("検索キーワードが入力されていません。");
            return;
        }
        if (results.isEmpty()) {
            System.out.println("「" + searchKey + "」を含む演目は見つかりませんでした。");
            return;
        }

        for (Performance performance : results) {
            System.out.print("\n【検索ヒット】");
            performance.show();
        }
    }
}