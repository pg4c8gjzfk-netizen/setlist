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
}
