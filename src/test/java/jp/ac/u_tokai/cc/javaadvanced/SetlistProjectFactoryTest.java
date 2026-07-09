package jp.ac.u_tokai.cc.javaadvanced;

import static org.junit.Assert.assertEquals;

import java.util.List;
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
}
