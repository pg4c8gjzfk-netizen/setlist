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
        return newEmptyProject(1);
    }

    /** 指定した公演数で、GUI手入力用の空プロジェクトを作成します。 */
    public static SetlistProject newEmptyProject(int performanceCount) {
        if (performanceCount < 1) {
            throw new IllegalArgumentException("performanceCount must be positive");
        }
        List<SetlistSession> sessions = new ArrayList<>();
        for (int index = 1; index <= performanceCount; index++) {
            sessions.add(new SetlistSession("第" + index + "公演", List.of()));
        }
        return new SetlistProject(sessions);
    }
}
