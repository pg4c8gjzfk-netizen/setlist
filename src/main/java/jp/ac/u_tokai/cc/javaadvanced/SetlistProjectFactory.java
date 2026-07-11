package jp.ac.u_tokai.cc.javaadvanced;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 既存の自動生成結果と編集可能なプロジェクトモデルを変換します。 */
public final class SetlistProjectFactory {

    private SetlistProjectFactory() {
    }

    /**
     * 初回生成結果を編集可能なプロジェクトへ変換します。
     *
     * <p>同じ {@link Performance} インスタンスには同一の元演目IDを割り当てます。</p>
     *
     * @param generatedSessions 既存の自動生成結果
     * @return 編集可能なプロジェクト
     */
    public static SetlistProject fromGeneratedSessions(List<List<Performance>> generatedSessions) {
        Map<Performance, UUID> sourceIds = new IdentityHashMap<>();
        List<SetlistSession> sessions = new ArrayList<>();
        for (int sessionIndex = 0; sessionIndex < generatedSessions.size(); sessionIndex++) {
            List<SetlistEntry> entries = new ArrayList<>();
            for (Performance performance : generatedSessions.get(sessionIndex)) {
                UUID sourceId = sourceIds.computeIfAbsent(performance, ignored -> UUID.randomUUID());
                entries.add(SetlistEntry.fromPerformance(sourceId, performance));
            }
            sessions.add(new SetlistSession("第" + (sessionIndex + 1) + "公演", entries));
        }
        return new SetlistProject(sessions);
    }

    /**
     * インポートした演目を自動配分せず、未割り当ての1タブとして編集プロジェクトへ変換します。
     *
     * @param performances インポート済みの演目
     * @return 未割り当て演目を持つ編集可能なプロジェクト
     */
    public static SetlistProject fromImportedPerformances(Iterable<Performance> performances) {
        List<SetlistEntry> entries = new ArrayList<>();
        for (Performance performance : performances) {
            entries.add(SetlistEntry.fromPerformance(UUID.randomUUID(), performance));
        }
        return new SetlistProject(List.of(new SetlistSession("未割り当て", entries)));
    }

    /**
     * インポート元のシート名と所属を維持した編集プロジェクトへ変換します。
     *
     * @param performanceSheets シート単位の演目
     * @return 元シートと同じ名前・所属を持つ編集可能なプロジェクト
     */
    public static SetlistProject fromImportedSheets(List<PerformanceSheet> performanceSheets) {
        List<SetlistSession> sessions = new ArrayList<>();
        for (PerformanceSheet performanceSheet : performanceSheets) {
            List<SetlistEntry> entries = new ArrayList<>();
            for (Performance performance : performanceSheet.performances()) {
                entries.add(SetlistEntry.fromPerformance(UUID.randomUUID(), performance));
            }
            sessions.add(new SetlistSession(performanceSheet.name(), entries));
        }
        return new SetlistProject(sessions, true);
    }

    /** GUIで手入力を始めるための空プロジェクトを作成します。 */
    public static SetlistProject newEmptyProject() {
        return new SetlistProject(List.of(new SetlistSession("第1公演", List.of())));
    }
}
