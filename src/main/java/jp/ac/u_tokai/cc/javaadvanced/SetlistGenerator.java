package jp.ac.u_tokai.cc.javaadvanced;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * セットリストを自動生成するクラス
 */
public class SetlistGenerator {

    // 公演回数（numberOfSessions）を受け取るように引数を追加
    public void generate(Map<String, Performance> allPerformances, int numberOfSessions) {
        System.out.println("\n=== セットリストの自動生成を開始します ===");

        // リストの中にリストを入れる「二次元リスト（sessions）」を用意
        List<List<Performance>> sessions = new ArrayList<>();

        // 入力された数だけ、空の箱（公演枠）を作って sessions に追加する
        for (int i = 0; i < numberOfSessions; i++) {
            sessions.add(new ArrayList<>());
        }
        
        System.out.println("👀 [システム] " + numberOfSessions + " 公演分の空の枠を用意しました！");

        // --- 今後のアルゴリズム構築用スペース ---
        
        // TODO: 絶対条件の処理（help!!の固定など）
        
        // TODO: 配慮条件の処理（連続回避と割り振り）

        System.out.println("（アルゴリズム構築中...）");
    }
}