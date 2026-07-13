package jp.ac.u_tokai.cc.javaadvanced;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

/** XLSXの不正行や行欠落を黙って許可しないことを検証します。 */
public class XlsxPerformanceReaderValidationTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void invalidDurationFailsWholeImportWithSheetAndRow() throws Exception {
        File workbookFile = temporaryFolder.newFile("invalid-duration.xlsx");
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet first = createSheet(workbook, "第1公演");
            addPerformance(first, 1, "正常曲", "3:00");
            Sheet second = createSheet(workbook, "第2公演");
            addPerformance(second, 1, "秒が不正な曲", "3:99");
            write(workbook, workbookFile);
        }

        try {
            new XlsxPerformanceReader().loadSheets(workbookFile);
            fail("不正行を含むXLSXは読込に失敗する必要があります。");
        } catch (XlsxPerformanceReader.XlsxImportException exception) {
            assertTrue(exception.getMessage().contains("シート「第2公演」の2行目"));
            assertTrue(exception.getMessage().contains("3:99"));
        }
    }

    @Test
    public void blankTitleWithOtherValuesIsReportedInsteadOfSkipped() throws Exception {
        File workbookFile = temporaryFolder.newFile("blank-title.xlsx");
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = createSheet(workbook, "第1公演");
            addPerformance(sheet, 1, "", "3:00");
            write(workbook, workbookFile);
        }

        try {
            new XlsxPerformanceReader().loadSheets(workbookFile);
            fail("曲名がない入力行は読込に失敗する必要があります。");
        } catch (XlsxPerformanceReader.XlsxImportException exception) {
            assertTrue(exception.getMessage().contains("シート「第1公演」の2行目"));
            assertTrue(exception.getMessage().contains("曲名が空欄"));
        }
    }

    @Test
    public void rowsBeyondFormerFiftyRowLimitAreLoaded() throws Exception {
        File workbookFile = temporaryFolder.newFile("many-rows.xlsx");
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = createSheet(workbook, "長い公演");
            for (int rowIndex = 1; rowIndex <= 55; rowIndex++) {
                addPerformance(sheet, rowIndex, "演目" + rowIndex, "3:00");
            }
            Row total = sheet.createRow(56);
            total.createCell(0).setCellValue("合計");
            write(workbook, workbookFile);
        }

        List<PerformanceSheet> sheets = new XlsxPerformanceReader().loadSheets(workbookFile);

        assertEquals(55, sheets.getFirst().performances().size());
        assertEquals("演目55", sheets.getFirst().performances().getLast().getTitle());
    }

    @Test
    public void emptyPerformanceSheetKeepsItsWorkbookBoundary() throws Exception {
        File workbookFile = temporaryFolder.newFile("empty-sheet.xlsx");
        try (Workbook workbook = new XSSFWorkbook()) {
            createSheet(workbook, "空の公演");
            Sheet populated = createSheet(workbook, "通常公演");
            addPerformance(populated, 1, "確認曲", "3:00");
            write(workbook, workbookFile);
        }

        List<PerformanceSheet> sheets = new XlsxPerformanceReader().loadSheets(workbookFile);

        assertEquals(List.of("空の公演", "通常公演"), sheets.stream().map(PerformanceSheet::name).toList());
        assertTrue(sheets.getFirst().performances().isEmpty());
    }

    @Test
    public void missingHeaderFailsWholeImportWithSheetName() throws Exception {
        File workbookFile = temporaryFolder.newFile("missing-header.xlsx");
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("見出しなし公演");
            addPerformance(sheet, 1, "読み飛ばしてはいけない曲", "3:00");
            write(workbook, workbookFile);
        }

        try {
            new XlsxPerformanceReader().loadSheets(workbookFile);
            fail("見出し行がないXLSXは読込に失敗する必要があります。");
        } catch (XlsxPerformanceReader.XlsxImportException exception) {
            assertTrue(exception.getMessage().contains("シート「見出しなし公演」の1行目"));
            assertTrue(exception.getMessage().contains("見出し行がありません"));
        }
    }

    private Sheet createSheet(Workbook workbook, String name) {
        Sheet sheet = workbook.createSheet(name);
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("曲名");
        header.createCell(1).setCellValue("時間");
        header.createCell(2).setCellValue("出演者A");
        header.createCell(3).setCellValue("人数");
        return sheet;
    }

    private void addPerformance(Sheet sheet, int rowIndex, String title, String duration) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(title);
        row.createCell(1).setCellValue(duration);
        row.createCell(2).setCellValue("◯");
    }

    private void write(Workbook workbook, File outputFile) throws Exception {
        try (FileOutputStream output = new FileOutputStream(outputFile)) {
            workbook.write(output);
        }
    }
}
