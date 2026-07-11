package jp.ac.u_tokai.cc.javaadvanced;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class SetlistProjectFactoryTest {

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
        assertTrue(project.sheetBoundariesLocked());
    }
}
