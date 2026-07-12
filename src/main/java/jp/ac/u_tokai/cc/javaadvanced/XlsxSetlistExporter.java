package jp.ac.u_tokai.cc.javaadvanced;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.PageMargin;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * 生成されたセットリストをExcelファイル（.xlsx）として保存するクラス。
 */
public class XlsxSetlistExporter {

    private static final int HEADER_ROW = 0;
    private static final int FIRST_DATA_ROW = 1;
    private static final int ORDER_COLUMN = 0;
    private static final int TITLE_COLUMN = 1;
    private static final int DURATION_COLUMN = 2;
    private static final int FIRST_PERFORMER_COLUMN = 3;
    private static final double SECONDS_PER_DAY = 24d * 60d * 60d;

    private final Path outputDirectory;

    /** ユーザーのDocuments配下にある安定した出力先を使用します。 */
    public XlsxSetlistExporter() {
        this(new AppFileLocations().outputDirectory().toPath());
    }

    /** テストなどで出力先を差し替えます。 */
    XlsxSetlistExporter(Path outputDirectory) {
        this.outputDirectory = Objects.requireNonNull(
                outputDirectory, "outputDirectory must not be null");
    }

    /**
     * 生成されたセットリストをExcelファイルに書き出します。
     * 公演ごとにシートを分け、後から実際の表計算ソフトで確認しやすい形で保存します。
     *
     * @param performanceSheets 元シート名と所属を保持した生成結果
     * @param fileName 出力するファイル名（例: "output_setlist.xlsx"）
     * @return 保存したファイル
     * @throws IOException 保存に失敗した場合
     */
    public File export(List<PerformanceSheet> performanceSheets, String fileName) throws IOException {
        Objects.requireNonNull(performanceSheets, "performanceSheets must not be null");
        if (performanceSheets.isEmpty()) {
            throw new IllegalArgumentException("出力する公演シートがありません。");
        }
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("出力ファイル名を指定してください。");
        }

        List<ExportSession> sessions = new ArrayList<>();
        for (PerformanceSheet performanceSheet : performanceSheets) {
            List<ExportEntry> entries = performanceSheet.performances().stream()
                    .map(performance -> new ExportEntry(
                            performance.getDisplayTitle(),
                            performance.getDuration(),
                            performance.getPerformers()))
                    .toList();
            sessions.add(new ExportSession(
                    performanceSheet.name(), entries, performanceSheet.performerNames()));
        }
        return exportSessions(sessions, outputDirectory.resolve(fileName).toFile());
    }

    /**
     * 編集画面の最新プロジェクトを配布用XLSXへ書き出します。
     *
     * @param project 現在の編集内容
     * @param outputFile 保存先
     * @return 保存したファイル
     * @throws IOException 保存に失敗した場合
     */
    public File export(SetlistProject project, File outputFile) throws IOException {
        Objects.requireNonNull(project, "project must not be null");
        if (project.sessions().isEmpty()) {
            throw new IllegalArgumentException("出力する公演シートがありません。");
        }

        List<ExportSession> sessions = new ArrayList<>();
        for (SetlistSession session : project.sessions()) {
            List<ExportEntry> entries = session.entries().stream()
                    .map(entry -> new ExportEntry(
                            entry.title(), entry.durationSeconds(), entry.performers()))
                    .toList();
            sessions.add(new ExportSession(
                    session.name(), entries, session.performerNames()));
        }
        return exportSessions(sessions, outputFile);
    }

    private File exportSessions(List<ExportSession> sessions, File outputFile) throws IOException {
        Objects.requireNonNull(outputFile, "outputFile must not be null");
        File absoluteFile = outputFile.getAbsoluteFile();
        File parentDirectory = absoluteFile.getParentFile();
        if (parentDirectory != null) {
            Files.createDirectories(parentDirectory.toPath());
        }

        try (Workbook workbook = new XSSFWorkbook();
                FileOutputStream fos = new FileOutputStream(absoluteFile)) {
            ExportStyles styles = createStyles(workbook);
            for (ExportSession session : sessions) {
                writeSessionSheet(workbook, session, styles);
            }

            workbook.getCreationHelper().createFormulaEvaluator().evaluateAll();
            workbook.setForceFormulaRecalculation(true);
            workbook.write(fos);
        }
        System.out.println("[成功] セットリストをExcel形式で保存しました: " + absoluteFile.getAbsolutePath());
        return absoluteFile;
    }

    /**
     * 1公演分のセットリストを1つのシートへ書き込みます。
     */
    private void writeSessionSheet(
            Workbook workbook, ExportSession session, ExportStyles styles) {
        Sheet sheet = workbook.createSheet(session.name());
        int performerCountColumn = FIRST_PERFORMER_COLUMN + session.performerNames().size();
        Row headerRow = sheet.createRow(HEADER_ROW);
        writeHeaderCell(headerRow, ORDER_COLUMN, "順番", styles.header());
        writeHeaderCell(headerRow, TITLE_COLUMN, "演目名", styles.header());
        writeHeaderCell(headerRow, DURATION_COLUMN, "時間", styles.header());
        for (int performerIndex = 0; performerIndex < session.performerNames().size(); performerIndex++) {
            writeHeaderCell(
                    headerRow,
                    FIRST_PERFORMER_COLUMN + performerIndex,
                    session.performerNames().get(performerIndex),
                    styles.header());
        }
        writeHeaderCell(headerRow, performerCountColumn, "出演人数", styles.header());
        headerRow.setHeightInPoints(30f);

        for (int i = 0; i < session.entries().size(); i++) {
            Row row = sheet.createRow(FIRST_DATA_ROW + i);
            ExportEntry entry = session.entries().get(i);
            boolean alternate = i % 2 == 1;
            writeDataRow(
                    row,
                    i + 1,
                    entry,
                    session.performerNames(),
                    performerCountColumn,
                    alternate,
                    styles);
        }

        int totalRowIndex = FIRST_DATA_ROW + session.entries().size();
        writeTotalRow(
                sheet.createRow(totalRowIndex),
                session.entries().size(),
                session.performerNames().size(),
                performerCountColumn,
                styles);
        configureSheet(workbook, sheet, session, performerCountColumn, totalRowIndex);
    }

    private void writeHeaderCell(Row row, int columnIndex, String value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void writeDataRow(
            Row row,
            int order,
            ExportEntry entry,
            List<String> performerNames,
            int performerCountColumn,
            boolean alternate,
            ExportStyles styles) {
        CellStyle textStyle = alternate ? styles.alternateText() : styles.text();
        CellStyle centerStyle = alternate ? styles.alternateCenter() : styles.center();
        CellStyle durationStyle = alternate ? styles.alternateDuration() : styles.duration();
        CellStyle markStyle = alternate ? styles.alternateMark() : styles.mark();

        Cell orderCell = row.createCell(ORDER_COLUMN);
        orderCell.setCellValue(order);
        orderCell.setCellStyle(centerStyle);

        Cell titleCell = row.createCell(TITLE_COLUMN);
        titleCell.setCellValue(entry.title());
        titleCell.setCellStyle(textStyle);

        Cell durationCell = row.createCell(DURATION_COLUMN);
        durationCell.setCellValue(entry.durationSeconds() / SECONDS_PER_DAY);
        durationCell.setCellStyle(durationStyle);

        Set<String> activePerformers = new HashSet<>(entry.performers());
        for (int performerIndex = 0; performerIndex < performerNames.size(); performerIndex++) {
            Cell performerCell = row.createCell(FIRST_PERFORMER_COLUMN + performerIndex);
            performerCell.setCellValue(
                    activePerformers.contains(performerNames.get(performerIndex)) ? "○" : "");
            performerCell.setCellStyle(markStyle);
        }

        Cell performerCountCell = row.createCell(performerCountColumn);
        if (performerNames.isEmpty()) {
            performerCountCell.setCellValue(0);
        } else {
            int excelRow = row.getRowNum() + 1;
            String firstColumn = CellReference.convertNumToColString(FIRST_PERFORMER_COLUMN);
            String lastColumn = CellReference.convertNumToColString(performerCountColumn - 1);
            performerCountCell.setCellFormula(
                    "COUNTIF(" + firstColumn + excelRow + ":" + lastColumn + excelRow + ",\"○\")");
        }
        performerCountCell.setCellStyle(centerStyle);
        row.setHeightInPoints(22f);
    }

    private void writeTotalRow(
            Row totalRow,
            int entryCount,
            int performerCount,
            int performerCountColumn,
            ExportStyles styles) {
        Cell labelCell = totalRow.createCell(ORDER_COLUMN);
        labelCell.setCellValue("合計");
        labelCell.setCellStyle(styles.totalLabel());

        Cell performanceCountCell = totalRow.createCell(TITLE_COLUMN);
        Cell durationCell = totalRow.createCell(DURATION_COLUMN);
        if (entryCount == 0) {
            performanceCountCell.setCellValue(0);
            durationCell.setCellValue(0);
        } else {
            int lastExcelDataRow = totalRow.getRowNum();
            performanceCountCell.setCellFormula(
                    "COUNTA(B2:B" + lastExcelDataRow + ")");
            durationCell.setCellFormula(
                    "SUM(C2:C" + lastExcelDataRow + ")");
        }
        performanceCountCell.setCellStyle(styles.totalPerformanceCount());
        durationCell.setCellStyle(styles.totalDuration());

        for (int performerIndex = 0; performerIndex < performerCount; performerIndex++) {
            int columnIndex = FIRST_PERFORMER_COLUMN + performerIndex;
            Cell performerTotalCell = totalRow.createCell(columnIndex);
            if (entryCount == 0) {
                performerTotalCell.setCellValue(0);
            } else {
                String columnName = CellReference.convertNumToColString(columnIndex);
                int lastExcelDataRow = totalRow.getRowNum();
                performerTotalCell.setCellFormula(
                        "COUNTIF(" + columnName + "2:" + columnName + lastExcelDataRow + ",\"○\")");
            }
            performerTotalCell.setCellStyle(styles.totalParticipationCount());
        }

        Cell allParticipationsCell = totalRow.createCell(performerCountColumn);
        if (entryCount == 0) {
            allParticipationsCell.setCellValue(0);
        } else {
            String columnName = CellReference.convertNumToColString(performerCountColumn);
            int lastExcelDataRow = totalRow.getRowNum();
            allParticipationsCell.setCellFormula(
                    "SUM(" + columnName + "2:" + columnName + lastExcelDataRow + ")");
        }
        allParticipationsCell.setCellStyle(styles.totalParticipationCount());
        totalRow.setHeightInPoints(24f);
    }

    private void configureSheet(
            Workbook workbook,
            Sheet sheet,
            ExportSession session,
            int performerCountColumn,
            int totalRowIndex) {
        sheet.setDefaultRowHeightInPoints(22f);
        sheet.setDisplayGridlines(false);
        sheet.setPrintGridlines(false);
        sheet.setHorizontallyCenter(true);
        sheet.setAutobreaks(true);
        sheet.setFitToPage(true);
        sheet.setZoom(90);
        sheet.createFreezePane(FIRST_PERFORMER_COLUMN, FIRST_DATA_ROW);
        sheet.setAutoFilter(new CellRangeAddress(
                HEADER_ROW,
                Math.max(HEADER_ROW, totalRowIndex - 1),
                ORDER_COLUMN,
                performerCountColumn));
        sheet.setRepeatingRows(CellRangeAddress.valueOf("1:1"));
        sheet.setRepeatingColumns(CellRangeAddress.valueOf("A:C"));

        sheet.setColumnWidth(ORDER_COLUMN, 7 * 256);
        sheet.setColumnWidth(TITLE_COLUMN, 36 * 256);
        sheet.setColumnWidth(DURATION_COLUMN, 10 * 256);
        for (int performerIndex = 0; performerIndex < session.performerNames().size(); performerIndex++) {
            String performerName = session.performerNames().get(performerIndex);
            int characterWidth = Math.max(8, Math.min(14, performerName.length() + 3));
            sheet.setColumnWidth(FIRST_PERFORMER_COLUMN + performerIndex, characterWidth * 256);
        }
        sheet.setColumnWidth(performerCountColumn, 11 * 256);

        PrintSetup printSetup = sheet.getPrintSetup();
        printSetup.setLandscape(true);
        printSetup.setPaperSize(PrintSetup.A4_PAPERSIZE);
        printSetup.setFitWidth((short) 1);
        printSetup.setFitHeight((short) 0);
        if (sheet instanceof XSSFSheet xssfSheet
                && xssfSheet.getCTWorksheet().getPageSetup().isSetUsePrinterDefaults()) {
            xssfSheet.getCTWorksheet().getPageSetup().unsetUsePrinterDefaults();
        }
        sheet.setMargin(PageMargin.LEFT, 0.25);
        sheet.setMargin(PageMargin.RIGHT, 0.25);
        sheet.setMargin(PageMargin.TOP, 0.55);
        sheet.setMargin(PageMargin.BOTTOM, 0.45);
        sheet.setMargin(PageMargin.HEADER, 0.2);
        sheet.setMargin(PageMargin.FOOTER, 0.2);
        sheet.getHeader().setCenter("&B" + session.name().replace("&", "&&") + " 香盤表");
        sheet.getFooter().setCenter("Setlist Studio");
        sheet.getFooter().setRight("ページ &P / &N");

        int sheetIndex = workbook.getSheetIndex(sheet);
        workbook.setPrintArea(
                sheetIndex,
                ORDER_COLUMN,
                performerCountColumn,
                HEADER_ROW,
                totalRowIndex);
    }

    private ExportStyles createStyles(Workbook workbook) {
        Font bodyFont = workbook.createFont();
        bodyFont.setFontName("Yu Gothic");
        bodyFont.setFontHeightInPoints((short) 10);

        Font markFont = workbook.createFont();
        markFont.setFontName("Yu Gothic");
        markFont.setFontHeightInPoints((short) 11);
        markFont.setBold(true);
        markFont.setColor(IndexedColors.DARK_BLUE.getIndex());

        Font headerFont = workbook.createFont();
        headerFont.setFontName("Yu Gothic");
        headerFont.setFontHeightInPoints((short) 10);
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());

        Font totalFont = workbook.createFont();
        totalFont.setFontName("Yu Gothic");
        totalFont.setFontHeightInPoints((short) 10);
        totalFont.setBold(true);
        totalFont.setColor(IndexedColors.DARK_BLUE.getIndex());

        CellStyle header = workbook.createCellStyle();
        header.setFont(headerFont);
        header.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        header.setAlignment(HorizontalAlignment.CENTER);
        header.setVerticalAlignment(VerticalAlignment.CENTER);
        header.setWrapText(true);
        header.setBorderBottom(BorderStyle.MEDIUM);
        header.setBottomBorderColor(IndexedColors.DARK_BLUE.getIndex());

        CellStyle text = createDataStyle(workbook, bodyFont, HorizontalAlignment.LEFT, false);
        CellStyle alternateText = createDataStyle(workbook, bodyFont, HorizontalAlignment.LEFT, true);
        CellStyle center = createDataStyle(workbook, bodyFont, HorizontalAlignment.CENTER, false);
        CellStyle alternateCenter = createDataStyle(workbook, bodyFont, HorizontalAlignment.CENTER, true);
        CellStyle duration = cloneStyle(workbook, center);
        CellStyle alternateDuration = cloneStyle(workbook, alternateCenter);
        short durationFormat = workbook.createDataFormat().getFormat("[m]:ss");
        duration.setDataFormat(durationFormat);
        alternateDuration.setDataFormat(durationFormat);
        CellStyle mark = createDataStyle(workbook, markFont, HorizontalAlignment.CENTER, false);
        CellStyle alternateMark = createDataStyle(workbook, markFont, HorizontalAlignment.CENTER, true);

        CellStyle totalBase = workbook.createCellStyle();
        totalBase.setFont(totalFont);
        totalBase.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        totalBase.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        totalBase.setVerticalAlignment(VerticalAlignment.CENTER);
        totalBase.setBorderTop(BorderStyle.MEDIUM);
        totalBase.setTopBorderColor(IndexedColors.DARK_BLUE.getIndex());
        totalBase.setBorderBottom(BorderStyle.THIN);
        totalBase.setBottomBorderColor(IndexedColors.DARK_BLUE.getIndex());

        CellStyle totalLabel = cloneStyle(workbook, totalBase);
        totalLabel.setAlignment(HorizontalAlignment.LEFT);
        CellStyle totalPerformanceCount = cloneStyle(workbook, totalBase);
        totalPerformanceCount.setAlignment(HorizontalAlignment.CENTER);
        totalPerformanceCount.setDataFormat(workbook.createDataFormat().getFormat("0\"演目\""));
        CellStyle totalDuration = cloneStyle(workbook, totalBase);
        totalDuration.setAlignment(HorizontalAlignment.CENTER);
        totalDuration.setDataFormat(durationFormat);
        CellStyle totalParticipationCount = cloneStyle(workbook, totalBase);
        totalParticipationCount.setAlignment(HorizontalAlignment.CENTER);
        totalParticipationCount.setDataFormat(workbook.createDataFormat().getFormat("0\"回\""));

        return new ExportStyles(
                header,
                text,
                alternateText,
                center,
                alternateCenter,
                duration,
                alternateDuration,
                mark,
                alternateMark,
                totalLabel,
                totalPerformanceCount,
                totalDuration,
                totalParticipationCount);
    }

    private CellStyle createDataStyle(
            Workbook workbook,
            Font font,
            HorizontalAlignment alignment,
            boolean alternate) {
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setAlignment(alignment);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        if (alternate) {
            style.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        return style;
    }

    private CellStyle cloneStyle(Workbook workbook, CellStyle source) {
        CellStyle clone = workbook.createCellStyle();
        clone.cloneStyleFrom(source);
        return clone;
    }

    private record ExportStyles(
            CellStyle header,
            CellStyle text,
            CellStyle alternateText,
            CellStyle center,
            CellStyle alternateCenter,
            CellStyle duration,
            CellStyle alternateDuration,
            CellStyle mark,
            CellStyle alternateMark,
            CellStyle totalLabel,
            CellStyle totalPerformanceCount,
            CellStyle totalDuration,
            CellStyle totalParticipationCount) {
    }

    private record ExportSession(
            String name, List<ExportEntry> entries, List<String> performerNames) {

        private ExportSession {
            entries = List.copyOf(entries);
            performerNames = List.copyOf(performerNames);
        }
    }

    private record ExportEntry(String title, int durationSeconds, List<String> performers) {
    }
}
