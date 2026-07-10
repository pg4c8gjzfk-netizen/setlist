package jp.ac.u_tokai.cc.javaadvanced;

import static org.junit.Assert.assertEquals;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class SetlistProjectFactoryTest {

    @Test
    public void samePerformanceInstanceKeepsSameSourceIdAcrossSessions() {
        Performance performance = new Performance("同じ演目", List.of("出演者"), 120);

        SetlistProject project = SetlistProjectFactory.fromGeneratedSessions(List.of(
                List.of(performance),
                List.of(performance)));

        assertEquals(
                project.sessions().get(0).entries().get(0).sourcePerformanceId(),
                project.sessions().get(1).entries().get(0).sourcePerformanceId());
    }

    @Test
    public void importedPerformancesStartInOneUnassignedSessionWithoutGeneration() {
        Performance first = new Performance("入力順1", List.of("出演者A"), 120);
        Performance second = new Performance("入力順2", List.of("出演者B"), 180);
        Map<String, Performance> performances = new LinkedHashMap<>();
        performances.put("first", first);
        performances.put("second", second);

        SetlistProject project = SetlistProjectFactory.fromImportedPerformances(performances.values());

        assertEquals(1, project.sessions().size());
        assertEquals("未割り当て", project.sessions().get(0).name());
        assertEquals(List.of("入力順1", "入力順2"), project.sessions().get(0).entries().stream()
                .map(SetlistEntry::title)
                .toList());
    }

    @Test
    public void importedSheetsKeepTheirNamesAndEntriesSeparated() {
        Performance firstSheetEntry = new Performance("第1公演の曲", List.of("出演者A"), 120);
        Performance secondSheetEntry = new Performance("第2公演の曲", List.of("出演者B"), 180);

        SetlistProject project = SetlistProjectFactory.fromImportedSheets(List.of(
                new PerformanceSheet("第1公演", List.of(firstSheetEntry)),
                new PerformanceSheet("第2公演", List.of(secondSheetEntry))));

        assertEquals(List.of("第1公演", "第2公演"), project.sessions().stream()
                .map(SetlistSession::name)
                .toList());
        assertEquals(List.of("第1公演の曲"), project.sessions().get(0).entries().stream()
                .map(SetlistEntry::title)
                .toList());
        assertEquals(List.of("第2公演の曲"), project.sessions().get(1).entries().stream()
                .map(SetlistEntry::title)
                .toList());
    }
}
