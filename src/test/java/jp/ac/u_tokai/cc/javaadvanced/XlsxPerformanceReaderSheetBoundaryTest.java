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

    @Test
    public void duplicateRowsInOneSheetAreNotRemoved() throws Exception {
        File workbookFile = temporaryFolder.newFile("duplicate-rows.xlsx");
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = addPerformanceSheet(workbook, "第1公演", "同名曲", "出演者A");
            addPerformanceRow(sheet, 2, "同名曲");
            try (FileOutputStream output = new FileOutputStream(workbookFile)) {
                workbook.write(output);
            }
        }

        List<PerformanceSheet> sheets = new XlsxPerformanceReader().loadSheets(workbookFile);

        assertEquals(1, sheets.size());
        assertEquals(List.of("同名曲", "同名曲"), titles(sheets.getFirst()));
    }

    @Test
    public void performerHeadersKeepTheirOriginalColumnOrder() throws Exception {
        File workbookFile = temporaryFolder.newFile("performer-order.xlsx");
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("第1公演");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("曲名");
            header.createCell(1).setCellValue("時間");
            header.createCell(2).setCellValue("出演者A");
            header.createCell(3).setCellValue("出演者B");
            header.createCell(4).setCellValue("出演者C");
            header.createCell(5).setCellValue("人数");
            Row performance = sheet.createRow(1);
            performance.createCell(0).setCellValue("順序確認曲");
            performance.createCell(1).setCellValue("3:00");
            performance.createCell(2).setCellValue("◯");
            performance.createCell(4).setCellValue("◯");
            try (FileOutputStream output = new FileOutputStream(workbookFile)) {
                workbook.write(output);
            }
        }

        PerformanceSheet sheet = new XlsxPerformanceReader().loadSheets(workbookFile).getFirst();

        assertEquals(List.of("出演者A", "出演者B", "出演者C"), sheet.performerNames());
        assertEquals(List.of("出演者A", "出演者C"), sheet.performances().getFirst().getPerformers());
    }

    private Sheet addPerformanceSheet(
            Workbook workbook, String sheetName, String title, String performer) {
        Sheet sheet = workbook.createSheet(sheetName);
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("曲名");
        header.createCell(1).setCellValue("時間");
        header.createCell(2).setCellValue(performer);
        header.createCell(3).setCellValue("備考");

        addPerformanceRow(sheet, 1, title);
        return sheet;
    }

    private void addPerformanceRow(Sheet sheet, int rowIndex, String title) {
        Row performanceRow = sheet.createRow(rowIndex);
        performanceRow.createCell(0).setCellValue(title);
        performanceRow.createCell(1).setCellValue("3:00");
        performanceRow.createCell(2).setCellValue("○");
    }

    private List<String> titles(PerformanceSheet sheet) {
        return sheet.performances().stream().map(Performance::getDisplayTitle).toList();
    }
}
