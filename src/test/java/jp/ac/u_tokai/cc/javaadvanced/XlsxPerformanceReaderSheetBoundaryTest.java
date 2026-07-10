package jp.ac.u_tokai.cc.javaadvanced;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** XLSX読込時に元シートの境界を失わないことを検証します。 */
public class XlsxPerformanceReaderSheetBoundaryTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void samePerformanceInDifferentSheetsRemainsInBothSheets() throws Exception {
        File workbookFile = temporaryFolder.newFile("sheet-boundaries.xlsx");
        try (Workbook workbook = new XSSFWorkbook()) {
            addPerformanceSheet(workbook, "第1公演", "共通曲", "出演者A");
            addPerformanceSheet(workbook, "第2公演", "共通曲", "出演者A");
            try (FileOutputStream output = new FileOutputStream(workbookFile)) {
                workbook.write(output);
            }
        }

        List<PerformanceSheet> sheets = new XlsxPerformanceReader().loadSheets(workbookFile);

        assertEquals(List.of("第1公演", "第2公演"), sheets.stream()
                .map(PerformanceSheet::name)
                .toList());
        assertEquals(List.of("共通曲"), titles(sheets.get(0)));
        assertEquals(List.of("共通曲"), titles(sheets.get(1)));
    }

    private void addPerformanceSheet(
            Workbook workbook, String sheetName, String title, String performer) {
        Sheet sheet = workbook.createSheet(sheetName);
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("曲名");
        header.createCell(1).setCellValue("時間");
        header.createCell(2).setCellValue(performer);
        header.createCell(3).setCellValue("備考");

        Row performanceRow = sheet.createRow(1);
        performanceRow.createCell(0).setCellValue(title);
        performanceRow.createCell(1).setCellValue("3:00");
        performanceRow.createCell(2).setCellValue("○");
    }

    private List<String> titles(PerformanceSheet sheet) {
        return sheet.performances().stream().map(Performance::getDisplayTitle).toList();
    }
}
