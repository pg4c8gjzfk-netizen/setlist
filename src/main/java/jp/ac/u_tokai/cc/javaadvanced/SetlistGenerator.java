package jp.ac.u_tokai.cc.javaadvanced;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * セットリストを自動生成するクラス。
 */
public class SetlistGenerator {

    /**
     * 入力シートごとに独立して曲順を生成します。
     *
     * <p>演目は元シートの外へ移動しません。シート名・シート数・各シートの演目集合を
     * 生成前後で検証し、境界が壊れた場合は処理を失敗させます。</p>
     *
     * @param sourceSheets 元シート単位の演目
     * @param openerTitle 各シートで先頭候補にする演目名
     * @param closerTitle 各シートで末尾候補にする演目名
     * @return シート境界を維持した生成結果
     */
    public List<PerformanceSheet> generateWithinSheets(
            List<PerformanceSheet> sourceSheets, String openerTitle, String closerTitle) {
        Objects.requireNonNull(sourceSheets, "sourceSheets must not be null");

        List<PerformanceSheet> generatedSheets = new ArrayList<>();
        for (PerformanceSheet sourceSheet : sourceSheets) {
            List<Performance> generatedOrder = optimizeOrder(
                    new ArrayList<>(sourceSheet.performances()), openerTitle, closerTitle);
            generatedSheets.add(new PerformanceSheet(sourceSheet.name(), generatedOrder));
        }

        verifySheetMembershipPreserved(sourceSheets, generatedSheets);
        return List.copyOf(generatedSheets);
    }

    /** シート数・名前・演目インスタンスが生成前後で一致することを検証します。 */
    private void verifySheetMembershipPreserved(
            List<PerformanceSheet> before, List<PerformanceSheet> after) {
        if (before.size() != after.size()) {
            throw new IllegalStateException("生成後のシート数が入力と一致しません。");
        }
        for (int sheetIndex = 0; sheetIndex < before.size(); sheetIndex++) {
            PerformanceSheet sourceSheet = before.get(sheetIndex);
            PerformanceSheet generatedSheet = after.get(sheetIndex);
            if (!sourceSheet.name().equals(generatedSheet.name())
                    || !containsSameInstances(
                            sourceSheet.performances(), generatedSheet.performances())) {
                throw new IllegalStateException(
                        "生成後にシート「" + sourceSheet.name() + "」の演目が別シートへ移動しています。");
            }
        }
    }

    private boolean containsSameInstances(List<Performance> before, List<Performance> after) {
        if (before.size() != after.size()) {
            return false;
        }
        Map<Performance, Integer> remainingCounts = new IdentityHashMap<>();
        for (Performance performance : before) {
            remainingCounts.merge(performance, 1, Integer::sum);
        }
        for (Performance performance : after) {
            Integer count = remainingCounts.get(performance);
            if (count == null) {
                return false;
            }
            if (count == 1) {
                remainingCounts.remove(performance);
            } else {
                remainingCounts.put(performance, count - 1);
            }
        }
        return remainingCounts.isEmpty();
    }

    /**
     * インターバル（連続出演回避）を満たす順序を生成します。
     */
    private List<Performance> optimizeOrder(List<Performance> original, String openerTitle, String closerTitle) {
        if (original.size() <= 1) {
            return original;
        }

        List<Performance> bestOrder = new ArrayList<>(original);
        int bestViolations = Integer.MAX_VALUE;

        for (int attempt = 0; attempt < 1000; attempt++) {
            List<Performance> candidate = new ArrayList<>(original);
            Collections.shuffle(candidate);
            fixOpeningAndClosing(candidate, openerTitle, closerTitle);

            int violations = countViolations(candidate);
            if (violations == 0) {
                return candidate;
            }
            if (violations < bestViolations) {
                bestViolations = violations;
                bestOrder = new ArrayList<>(candidate);
            }
        }

        System.out.println("[警告] 一部の出演者が連続してしまう可能性があります。");
        return bestOrder;
    }

    /**
     * 指定されたオープニング曲とトリ曲を先頭・末尾に固定します。
     */
    private void fixOpeningAndClosing(List<Performance> candidate, String openerTitle, String closerTitle) {
        if (openerTitle != null && !openerTitle.trim().isEmpty()) {
            moveMatchingTitle(candidate, openerTitle, 0);
        }

        if (closerTitle != null && !closerTitle.trim().isEmpty()) {
            int closerIndex = findMatchingTitleIndex(candidate, closerTitle);
            if (closerIndex >= 0 && closerIndex != 0) {
                Collections.swap(candidate, candidate.size() - 1, closerIndex);
            }
        }
    }

    /**
     * 指定名を含む演目を指定位置へ移動します。
     */
    private void moveMatchingTitle(List<Performance> performances, String titlePart, int targetIndex) {
        int index = findMatchingTitleIndex(performances, titlePart);
        if (index >= 0) {
            Collections.swap(performances, targetIndex, index);
        }
    }

    /**
     * 指定名を含む演目の位置を探します。
     */
    private int findMatchingTitleIndex(List<Performance> performances, String titlePart) {
        for (int i = 0; i < performances.size(); i++) {
            if (performances.get(i).getTitle().contains(titlePart)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 連続出演などの制約違反の数をカウントします。
     */
    private int countViolations(List<Performance> order) {
        int violations = 0;
        for (int i = 0; i < order.size() - 1; i++) {
            if (sharesPerformer(order.get(i), order.get(i + 1))) {
                violations++;
            }
        }
        return violations;
    }

    /**
     * 2つの演目で出演者が被っているか判定します。
     */
    private boolean sharesPerformer(Performance first, Performance second) {
        for (String performer : first.getPerformers()) {
            if (performer == null || Performance.NO_PERFORMER.equals(performer) || performer.trim().isEmpty()) {
                continue;
            }
            if (second.getPerformers().contains(performer)) {
                return true;
            }
        }
        return false;
    }
}
