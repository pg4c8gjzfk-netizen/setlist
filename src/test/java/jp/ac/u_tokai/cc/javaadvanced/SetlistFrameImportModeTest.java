package jp.ac.u_tokai.cc.javaadvanced;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;
import org.junit.Test;

/** 入力形式ごとの編集開始時の境界方針を検証します。 */
public class SetlistFrameImportModeTest {

    @Test
    public void csvStartsAsMovableUnassignedEntries() {
        SetlistProject project = SetlistFrame.createImportedProject(
                new File("input.csv"),
                List.of(new PerformanceSheet(
                        "未割り当て", List.of(new Performance("CSV曲", List.of("出演者"), 120)))));

        assertFalse(project.sheetBoundariesLocked());
        assertEquals(List.of("未割り当て"), project.sessions().stream().map(SetlistSession::name).toList());
        assertEquals("CSV曲", project.sessions().getFirst().entries().getFirst().title());
    }

    @Test
    public void xlsxKeepsEveryWorksheetAsLockedSession() {
        SetlistProject project = SetlistFrame.createImportedProject(
                new File("input.xlsx"),
                List.of(
                        new PerformanceSheet(
                                "第1公演", List.of(new Performance("1曲目", List.of("出演者A"), 120))),
                        new PerformanceSheet(
                                "第2公演", List.of(new Performance("2曲目", List.of("出演者B"), 180)))));

        assertTrue(project.sheetBoundariesLocked());
        assertEquals(List.of("第1公演", "第2公演"),
                project.sessions().stream().map(SetlistSession::name).toList());
        assertEquals("1曲目", project.sessions().get(0).entries().getFirst().title());
        assertEquals("2曲目", project.sessions().get(1).entries().getFirst().title());
    }
}
