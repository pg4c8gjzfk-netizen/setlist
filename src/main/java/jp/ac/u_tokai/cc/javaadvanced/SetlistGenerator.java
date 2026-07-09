package jp.ac.u_tokai.cc.javaadvanced;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * セットリストを自動生成するクラス。
 */
public class SetlistGenerator {

    /**
     * 読み込まれた全演目データをもとに、指定された公演回数分のセットリストを生成します。
     *
     * @param allPerformances  全ての演目データが格納されたMap
     * @param numberOfSessions 作成する公演回数
     * @param capacities       各公演の演目数上限
     * @param openers          各公演のオープニング曲名リスト
     * @param closers          各公演のトリ曲名リスト
     * @return 生成された複数公演分のセットリスト（二次元リスト）
     */
    public List<List<Performance>> generate(
            Map<String, Performance> allPerformances,
            int numberOfSessions,
            int[] capacities,
            String[] openers,
            String[] closers) {
        System.out.println("\n=== セットリストの自動生成を開始します ===");

        int[] sessionCapacities = prepareCapacities(numberOfSessions, capacities);
        List<List<Performance>> sessions = createEmptySessions(numberOfSessions);
        List<List<Performance>> groups = groupByTitle(allPerformances);

        assignPerformances(sessions, groups, sessionCapacities);
        optimizeSessions(sessions, openers, closers);

        System.out.println("セットリストの生成が完了しました！\n");
        return sessions;
    }

    /**
     * 各公演の演目数上限を準備します。
     */
    private int[] prepareCapacities(int numberOfSessions, int[] capacities) {
        if (capacities != null) {
            return capacities;
        }

        int[] defaultCapacities = new int[numberOfSessions];
        for (int i = 0; i < numberOfSessions; i++) {
            defaultCapacities[i] = Integer.MAX_VALUE;
        }
        return defaultCapacities;
    }

    /**
     * 公演数分の空リストを作成します。
     */
    private List<List<Performance>> createEmptySessions(int numberOfSessions) {
        List<List<Performance>> sessions = new ArrayList<>();
        for (int i = 0; i < numberOfSessions; i++) {
            sessions.add(new ArrayList<>());
        }
        System.out.println("[システム] " + numberOfSessions + " 公演分の空の枠を用意しました。");
        return sessions;
    }

    /**
     * 同じ表示名の演目をグループ化します。
     */
    private List<List<Performance>> groupByTitle(Map<String, Performance> allPerformances) {
        Map<String, List<Performance>> groupedByTitle = new HashMap<>();
        for (Performance performance : allPerformances.values()) {
            groupedByTitle.putIfAbsent(performance.getDisplayTitle(), new ArrayList<>());
            groupedByTitle.get(performance.getDisplayTitle()).add(performance);
        }

        List<List<Performance>> groups = new ArrayList<>(groupedByTitle.values());
        Collections.shuffle(groups);
        return groups;
    }

    /**
     * 演目を公演へ振り分けます。
     */
    private void assignPerformances(List<List<Performance>> sessions, List<List<Performance>> groups, int[] capacities) {
        int sessionIndex = 0;
        for (List<Performance> group : groups) {
            Collections.shuffle(group);
            for (Performance performance : group) {
                boolean assigned = false;
                for (int i = 0; i < sessions.size(); i++) {
                    int targetIndex = (sessionIndex + i) % sessions.size();
                    if (sessions.get(targetIndex).size() < capacities[targetIndex]) {
                        sessions.get(targetIndex).add(performance);
                        sessionIndex = targetIndex + 1;
                        assigned = true;
                        break;
                    }
                }
                if (!assigned) {
                    System.out.println("[警告] 枠がいっぱいで「" + performance.getTitle() + "」を組み込めませんでした。");
                }
            }
        }
    }

    /**
     * 各公演内の順番を最適化します。
     */
    private void optimizeSessions(List<List<Performance>> sessions, String[] openers, String[] closers) {
        for (int i = 0; i < sessions.size(); i++) {
            String opener = (openers != null && openers.length > i) ? openers[i] : null;
            String closer = (closers != null && closers.length > i) ? closers[i] : null;
            sessions.set(i, optimizeOrder(sessions.get(i), opener, closer));
        }
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