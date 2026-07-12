package jp.ac.u_tokai.cc.javaadvanced;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;
import java.util.UUID;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.PaneInformation;
import org.apache.poi.xssf.usermodel.XSSFSheet;
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
            assertEquals("出演者A", workbook.getSheetAt(0).getRow(0).getCell(3).getStringCellValue());
            assertEquals("出演者B", workbook.getSheetAt(1).getRow(0).getCell(3).getStringCellValue());
            assertEquals("第1公演の曲", workbook.getSheetAt(0).getRow(1).getCell(1).getStringCellValue());
            assertEquals("第2公演の曲", workbook.getSheetAt(1).getRow(1).getCell(1).getStringCellValue());
            assertEquals("○", workbook.getSheetAt(0).getRow(1).getCell(3).getStringCellValue());
            assertEquals("○", workbook.getSheetAt(1).getRow(1).getCell(3).getStringCellValue());
        }
    }

    @Test
    public void exportProjectUsesLatestEditedValuesAndKeepsSessionsSeparated() throws Exception {
        SetlistEntry editedFirst = entry("編集後の第1公演曲", List.of("代理出演者"));
        SetlistEntry editedSecond = entry("編集後の第2公演曲", List.of("出演者B", "出演者C"));
        SetlistProject currentProject = new SetlistProject(List.of(
                new SetlistSession("0427M", List.of(editedFirst), List.of("代理出演者")),
                new SetlistSession("0513W", List.of(editedSecond), List.of("出演者B", "出演者C"))), true);
        File output = new File(temporaryFolder.newFolder("current-project-output"), "setlist.xlsx");

        new XlsxSetlistExporter().export(currentProject, output);

        try (Workbook workbook = new XSSFWorkbook(output)) {
            assertEquals(2, workbook.getNumberOfSheets());
            assertEquals("0427M", workbook.getSheetAt(0).getSheetName());
            assertEquals("0513W", workbook.getSheetAt(1).getSheetName());
            assertEquals("編集後の第1公演曲", workbook.getSheetAt(0).getRow(1).getCell(1).getStringCellValue());
            assertEquals("代理出演者", workbook.getSheetAt(0).getRow(0).getCell(3).getStringCellValue());
            assertEquals("○", workbook.getSheetAt(0).getRow(1).getCell(3).getStringCellValue());
            assertEquals("編集後の第2公演曲", workbook.getSheetAt(1).getRow(1).getCell(1).getStringCellValue());
            assertEquals("出演者B", workbook.getSheetAt(1).getRow(0).getCell(3).getStringCellValue());
            assertEquals("出演者C", workbook.getSheetAt(1).getRow(0).getCell(4).getStringCellValue());
            assertEquals("○", workbook.getSheetAt(1).getRow(1).getCell(3).getStringCellValue());
            assertEquals("○", workbook.getSheetAt(1).getRow(1).getCell(4).getStringCellValue());
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            assertEquals(1d, evaluator.evaluate(workbook.getSheetAt(0).getRow(1).getCell(4))
                    .getNumberValue(), 0.00001d);
            assertEquals(2d, evaluator.evaluate(workbook.getSheetAt(1).getRow(1).getCell(5))
                    .getNumberValue(), 0.00001d);
        }
    }

    @Test
    public void exportCreatesPerformerMatrixTotalsAndPrintLayout() throws Exception {
        SetlistEntry first = entry("オープニング", List.of("出演者A", "出演者C"), 125);
        SetlistEntry second = entry("エンディング", List.of("出演者B"), 60);
        SetlistProject project = new SetlistProject(List.of(new SetlistSession(
                "本番",
                List.of(first, second),
                List.of("出演者A", "出演者B", "出演者C"))), true);
        File output = new File(temporaryFolder.newFolder("formatted-output"), "setlist.xlsx");

        new XlsxSetlistExporter().export(project, output);

        try (Workbook workbook = new XSSFWorkbook(output)) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals("順番", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("演目名", sheet.getRow(0).getCell(1).getStringCellValue());
            assertEquals("時間", sheet.getRow(0).getCell(2).getStringCellValue());
            assertEquals("出演者A", sheet.getRow(0).getCell(3).getStringCellValue());
            assertEquals("出演者B", sheet.getRow(0).getCell(4).getStringCellValue());
            assertEquals("出演者C", sheet.getRow(0).getCell(5).getStringCellValue());
            assertEquals("出演人数", sheet.getRow(0).getCell(6).getStringCellValue());

            assertEquals(CellType.NUMERIC, sheet.getRow(1).getCell(2).getCellType());
            assertEquals(125d / 86400d, sheet.getRow(1).getCell(2).getNumericCellValue(), 0.0000001d);
            assertEquals("[m]:ss", sheet.getRow(1).getCell(2).getCellStyle().getDataFormatString());
            assertEquals("○", sheet.getRow(1).getCell(3).getStringCellValue());
            assertEquals("", sheet.getRow(1).getCell(4).getStringCellValue());
            assertEquals("○", sheet.getRow(1).getCell(5).getStringCellValue());
            assertEquals("COUNTIF(D2:F2,\"○\")", sheet.getRow(1).getCell(6).getCellFormula());

            assertEquals("合計", sheet.getRow(3).getCell(0).getStringCellValue());
            assertEquals("COUNTA(B2:B3)", sheet.getRow(3).getCell(1).getCellFormula());
            assertEquals("SUM(C2:C3)", sheet.getRow(3).getCell(2).getCellFormula());
            assertEquals("COUNTIF(D2:D3,\"○\")", sheet.getRow(3).getCell(3).getCellFormula());
            assertEquals("SUM(G2:G3)", sheet.getRow(3).getCell(6).getCellFormula());

            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            assertEquals(2d, evaluator.evaluate(sheet.getRow(3).getCell(1)).getNumberValue(), 0.00001d);
            assertEquals(185d / 86400d,
                    evaluator.evaluate(sheet.getRow(3).getCell(2)).getNumberValue(), 0.0000001d);
            assertEquals(1d, evaluator.evaluate(sheet.getRow(3).getCell(3)).getNumberValue(), 0.00001d);
            assertEquals(1d, evaluator.evaluate(sheet.getRow(3).getCell(4)).getNumberValue(), 0.00001d);
            assertEquals(1d, evaluator.evaluate(sheet.getRow(3).getCell(5)).getNumberValue(), 0.00001d);
            assertEquals(3d, evaluator.evaluate(sheet.getRow(3).getCell(6)).getNumberValue(), 0.00001d);

            assertTrue(sheet.getRow(0).getCell(0).getCellStyle().getFontIndex() >= 0);
            assertTrue(workbook.getFontAt(sheet.getRow(0).getCell(0).getCellStyle().getFontIndex())
                    .getBold());
            assertEquals(IndexedColors.WHITE.getIndex(),
                    workbook.getFontAt(sheet.getRow(0).getCell(0).getCellStyle().getFontIndex())
                            .getColor());
            assertEquals(FillPatternType.SOLID_FOREGROUND,
                    sheet.getRow(0).getCell(0).getCellStyle().getFillPattern());
            assertFalse(sheet.isDisplayGridlines());
            assertFalse(sheet.isPrintGridlines());
            assertTrue(sheet.getFitToPage());
            assertTrue(sheet.getPrintSetup().getLandscape());
            assertEquals(PrintSetup.A4_PAPERSIZE, sheet.getPrintSetup().getPaperSize());
            assertEquals(1, sheet.getPrintSetup().getFitWidth());
            assertEquals(0, sheet.getPrintSetup().getFitHeight());
            assertFalse(((XSSFSheet) sheet).getCTWorksheet().getPageSetup()
                    .isSetUsePrinterDefaults());
            PaneInformation pane = sheet.getPaneInformation();
            assertNotNull(pane);
            assertTrue(pane.isFreezePane());
            assertEquals(3, pane.getVerticalSplitPosition());
            assertEquals(1, pane.getHorizontalSplitPosition());
            assertEquals(0, sheet.getRepeatingRows().getFirstRow());
            assertEquals(0, sheet.getRepeatingColumns().getFirstColumn());
            assertEquals(2, sheet.getRepeatingColumns().getLastColumn());
            assertNotNull(((XSSFSheet) sheet).getCTWorksheet().getAutoFilter());
            assertTrue(workbook.getPrintArea(0).contains("$A$1"));
            assertTrue(workbook.getPrintArea(0).contains("$G$4"));
        }
    }

    @Test
    public void exportHandlesPerformanceWithoutNamedPerformers() throws Exception {
        Performance performance = new Performance("出演者未定", List.of(), 60);
        File output = new XlsxSetlistExporter(temporaryFolder.newFolder("no-performer-output").toPath())
                .export(List.of(new PerformanceSheet("未定公演", List.of(performance))), "setlist.xlsx");

        try (Workbook workbook = new XSSFWorkbook(output)) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals("出演人数", sheet.getRow(0).getCell(3).getStringCellValue());
            assertEquals(0d, sheet.getRow(1).getCell(3).getNumericCellValue(), 0d);
            assertEquals("SUM(D2:D2)", sheet.getRow(2).getCell(3).getCellFormula());
        }
    }

    private SetlistEntry entry(String title, List<String> performers) {
        return entry(title, performers, 180);
    }

    private SetlistEntry entry(String title, List<String> performers, int durationSeconds) {
        return new SetlistEntry(
                UUID.randomUUID(), UUID.randomUUID(), title, durationSeconds,
                performers, false, FixedPosition.NONE, -1);
    }
}
