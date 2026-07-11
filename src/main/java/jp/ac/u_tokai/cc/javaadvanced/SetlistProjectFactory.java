package jp.ac.u_tokai.cc.javaadvanced;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** XLSXのシート構造を編集可能なプロジェクトモデルへ変換します。 */
public final class SetlistProjectFactory {

    private SetlistProjectFactory() {
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
            sessions.add(new SetlistSession(
                    performanceSheet.name(), entries, performanceSheet.performerNames()));
        }
        return new SetlistProject(sessions, true);
    }

    /** GUIで手入力を始めるための空プロジェクトを作成します。 */
    public static SetlistProject newEmptyProject() {
        return new SetlistProject(List.of(new SetlistSession("第1公演", List.of())));
    }
}
