package jp.ac.u_tokai.cc.javaadvanced;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** XLSX出力でも元シート名と所属を維持することを検証します。 */
public class XlsxSetlistExporterTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void exportKeepsSheetNamesAndEntriesSeparated() throws Exception {
        Performance first = new Performance("第1公演の曲", List.of("出演者A"), 120);
        Performance second = new Performance("第2公演の曲", List.of("出演者B"), 180);
        List<PerformanceSheet> sourceSheets = List.of(
                new PerformanceSheet("0427M", List.of(first)),
                new PerformanceSheet("0513W", List.of(second)));

        File output = new XlsxSetlistExporter(temporaryFolder.newFolder("output").toPath())
                .export(sourceSheets, "setlist.xlsx");

        assertTrue(output.isFile());
        try (Workbook workbook = new XSSFWorkbook(output)) {
            assertEquals(2, workbook.getNumberOfSheets());
            assertEquals("0427M", workbook.getSheetAt(0).getSheetName());
            assertEquals("0513W", workbook.getSheetAt(1).getSheetName());
            assertEquals("第1公演の曲", workbook.getSheetAt(0).getRow(1).getCell(1).getStringCellValue());
            assertEquals("第2公演の曲", workbook.getSheetAt(1).getRow(1).getCell(1).getStringCellValue());
        }
    }
}
