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

    /** GUIで手入力を始めるための空プロジェクトを作成します。 */
    public static SetlistProject newEmptyProject() {
        return new SetlistProject(List.of(new SetlistSession("第1公演", List.of())));
    }
}
