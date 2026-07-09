package jp.ac.u_tokai.cc.javaadvanced;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * セットリストを自動生成するクラス
 */
public class SetlistGenerator {

    /**
     * 読み込まれた全演目データをもとに、指定された公演回数分のセットリストを生成します。
     * 
     * @param allPerformances  全ての演目データが格納されたMap
     * @param numberOfSessions 作成する公演回数
     * @param capacities       各公演のキャパシティ
     * @param openers          各公演のオープニング曲名リスト
     * @param closers          各公演のトリ曲名リスト
     * @return 生成された複数公演分のセットリスト（二次元リスト）
     */
    public List<List<Performance>> generate(Map<String, Performance> allPerformances, int numberOfSessions, int[] capacities, String[] openers, String[] closers) {
        System.out.println("\n=== セットリストの自動生成を開始します ===");

        if (capacities == null) {
            capacities = new int[numberOfSessions];
            for (int i = 0; i < numberOfSessions; i++) capacities[i] = 9999;
        }

        List<List<Performance>> sessions = new ArrayList<>();
        for (int i = 0; i < numberOfSessions; i++) {
            sessions.add(new ArrayList<>());
        }

        System.out.println("👀 [システム] " + numberOfSessions + " 公演分の空の枠を用意しました！");

        // 1. 公演への振り分け（同じ曲が同じ公演に被らないようにするロジック）
        // ※優先度C：出演者の重複が最小限になるよう自動配分
        
        // 曲名（"(_" などを除去した本来のベース名）ごとにグループ化する
        java.util.Map<String, List<Performance>> groupedByTitle = new java.util.HashMap<>();
        for (Performance p : allPerformances.values()) {
            String baseTitle = p.getTitle().split("\\(_")[0];
            groupedByTitle.putIfAbsent(baseTitle, new ArrayList<>());
            groupedByTitle.get(baseTitle).add(p);
        }

        // グループ単位でシャッフルして、規則的な偏りをなくす
        List<List<Performance>> groups = new ArrayList<>(groupedByTitle.values());
        Collections.shuffle(groups);

        // 各グループ内の曲を順番に別々の公演へ割り振っていく
        int sessionIndex = 0;
        for (List<Performance> group : groups) {
            Collections.shuffle(group);
            for (Performance p : group) {
                boolean assigned = false;
                for (int i = 0; i < numberOfSessions; i++) {
                    int idx = (sessionIndex + i) % numberOfSessions;
                    if (sessions.get(idx).size() < capacities[idx]) {
                        sessions.get(idx).add(p);
                        sessionIndex = idx + 1;
                        assigned = true;
                        break;
                    }
                }
                if (!assigned) {
                    System.out.println("  ⚠️ [警告] 枠がいっぱいで「" + p.getTitle() + "」を組み込めませんでした。");
                }
            }
        }

        // 2. 各公演内で順番を最適化（インターバル制約の解決とトリ・オープニングの設定）
        for (int s = 0; s < sessions.size(); s++) {
            List<Performance> currentSession = sessions.get(s);
            String opener = (openers != null && openers.length > s) ? openers[s] : null;
            String closer = (closers != null && closers.length > s) ? closers[s] : null;
            
            // 優先度B：インターバルの確保（連続出演回避）と特定曲の固定を行う
            currentSession = optimizeOrder(currentSession, opener, closer);
            sessions.set(s, currentSession);
        }
        
        System.out.println("✅ セットリストの生成が完了しました！\n");
        return sessions;
    }

    /**
     * インターバル（連続出演回避）を満たす順序を生成する（ランダム試行アルゴリズム）
     */
    private List<Performance> optimizeOrder(List<Performance> original, String openerTitle, String closerTitle) {
        if (original.size() <= 1) return original;

        List<Performance> bestOrder = new ArrayList<>(original);
        int bestViolations = Integer.MAX_VALUE;

        // 最大1000回ランダムシャッフルして、連続出演の違反ゼロの配列を探す
        for (int attempt = 0; attempt < 1000; attempt++) {
            List<Performance> candidate = new ArrayList<>(original);
            Collections.shuffle(candidate);
            
            // オープニングの固定
            if (openerTitle != null && !openerTitle.trim().isEmpty()) {
                for (int i = 0; i < candidate.size(); i++) {
                    if (candidate.get(i).getTitle().contains(openerTitle)) {
                        Collections.swap(candidate, 0, i);
                        break;
                    }
                }
            }
            
            // トリの固定
            if (closerTitle != null && !closerTitle.trim().isEmpty()) {
                for (int i = 0; i < candidate.size(); i++) {
                    if (candidate.get(i).getTitle().contains(closerTitle)) {
                        // オープニングと被らないようにする
                        if (i != 0 || (openerTitle == null || openerTitle.isEmpty())) {
                            Collections.swap(candidate, candidate.size() - 1, i);
                        }
                        break;
                    }
                }
            }
            
            int violations = countViolations(candidate);
            if (violations == 0) {
                return candidate; // 完璧な順序が見つかったら即終了
            }
            // より違反が少ないマシな順序を保存しておく
            if (violations < bestViolations) {
                bestViolations = violations;
                bestOrder = new ArrayList<>(candidate);
            }
        }

        System.out.println("⚠️ 警告：一部の出演者が連続してしまう可能性があります。");
        return bestOrder; // 1000回試してもダメなら、一番マシなものを返す
    }

    /**
     * 連続出演などの制約違反の数をカウントする
     */
    private int countViolations(List<Performance> order) {
        int violations = 0;
        for (int i = 0; i < order.size() - 1; i++) {
            Performance current = order.get(i);
            Performance next = order.get(i + 1);
            if (sharesPerformer(current, next)) {
                violations++;
            }
        }
        return violations;
    }

    /**
     * 2つの演目で出演者が被っているか判定する
     */
    private boolean sharesPerformer(Performance p1, Performance p2) {
        for (String performer : p1.getPerformers()) {
            // ダミーデータや空欄は無視
            if (performer == null || performer.equals("noPerformer") || performer.trim().isEmpty()) {
                continue; 
            }
            if (p2.getPerformers().contains(performer)) {
                return true;
            }
        }
        return false;
    }
}