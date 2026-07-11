package jp.ac.u_tokai.cc.javaadvanced;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

/** 初回生成が入力シート間で演目を移動させないことを検証します。 */
public class SetlistGeneratorSheetBoundaryTest {

    @Test
    public void repeatedGenerationNeverMovesEntriesAcrossSheets() {
        Performance firstA = performance("第1公演A", "出演者A");
        Performance firstB = performance("第1公演B", "出演者B");
        Performance firstC = performance("第1公演C", "出演者C");
        Performance secondA = performance("第2公演A", "出演者D");
        Performance secondB = performance("第2公演B", "出演者E");
        Performance secondC = performance("第2公演C", "出演者F");

        List<Performance> firstEntries = List.of(firstA, firstB, firstC);
        List<Performance> secondEntries = List.of(secondA, secondB, secondC);
        List<PerformanceSheet> sourceSheets = List.of(
                new PerformanceSheet("第1公演", firstEntries),
                new PerformanceSheet("第2公演", secondEntries));

        SetlistGenerator generator = new SetlistGenerator();
        for (int attempt = 0; attempt < 100; attempt++) {
            List<PerformanceSheet> generated = generator.generateWithinSheets(sourceSheets);

            assertEquals(List.of("第1公演", "第2公演"), generated.stream()
                    .map(PerformanceSheet::name)
                    .toList());
            assertContainsSameInstances(firstEntries, generated.get(0).performances());
            assertContainsSameInstances(secondEntries, generated.get(1).performances());
        }
    }

    private Performance performance(String title, String performer) {
        return new Performance(title, List.of(performer), 180);
    }

    private void assertContainsSameInstances(
            List<Performance> expected, List<Performance> actual) {
        assertEquals(expected.size(), actual.size());
        for (Performance performance : actual) {
            assertTrue(expected.stream().anyMatch(candidate -> candidate == performance));
        }
    }
}
