package jp.ac.u_tokai.cc.javaadvanced;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/** 編集可能な {@link SetlistProject} をXLSXへ保存します。 */
public final class XlsxSetlistProjectWriter {

    static final String META_SHEET_NAME = "_setlist_meta";
    static final String FORMAT_KEY = "format";
    static final String FORMAT_VERSION = "setlist-project-v1";
    static final int DISPLAY_COLUMN_COUNT = 6;
    static final int ENTRY_ID_COLUMN = 6;
    static final int SOURCE_PERFORMANCE_ID_COLUMN = 7;
    static final int FIXED_COLUMN = 8;
    static final int FIXED_POSITION_COLUMN = 9;
    static final int FIXED_INDEX_COLUMN = 10;
    static final int META_PERFORMER_LIST_START_COLUMN = 8;

    /**
     * プロジェクトを指定ファイルへ保存します。
     *
     * @param project 保存するプロジェクト
     * @param outputFile 保存先のXLSXファイル
     * @throws IOException 保存に失敗した場合
     */
    public void write(SetlistProject project, File outputFile) throws IOException {
        new SetlistRegenerator().validate(project);
        if (outputFile == null) {
            throw new IllegalArgumentException("outputFile must not be null");
        }
        File absoluteFile = outputFile.getAbsoluteFile();
        File parentDirectory = absoluteFile.getParentFile();
        if (parentDirectory != null) {
            Files.createDirectories(parentDirectory.toPath());
        }

        try (Workbook workbook = new XSSFWorkbook();
                FileOutputStream output = new FileOutputStream(absoluteFile)) {
            CellStyle headerStyle = createHeaderStyle(workbook);
            Set<String> usedSheetNames = new HashSet<>();
            for (int sessionIndex = 0; sessionIndex < project.sessions().size(); sessionIndex++) {
                writeSessionSheet(workbook, project.sessions().get(sessionIndex), sessionIndex, headerStyle, usedSheetNames);
            }
            writeMetadataSheet(workbook, project, headerStyle);
            workbook.write(output);
        }
    }

    private void writeSessionSheet(
            Workbook workbook,
            SetlistSession session,
            int sessionIndex,
            CellStyle headerStyle,
            Set<String> usedSheetNames) {
        Sheet sheet = workbook.createSheet(createUniqueSheetName(session.name(), sessionIndex, usedSheetNames));
        Row header = sheet.createRow(0);
        String[] headers = {
                "順番", "曲名", "時間", "出演者", "固定", "固定位置",
                "演目ID", "元演目ID", "固定状態", "固定位置コード", "固定インデックス"
        };
        for (int column = 0; column < headers.length; column++) {
            Cell cell = header.createCell(column);
            cell.setCellValue(headers[column]);
            cell.setCellStyle(headerStyle);
        }
        for (int entryIndex = 0; entryIndex < session.entries().size(); entryIndex++) {
            SetlistEntry entry = session.entries().get(entryIndex);
            Row row = sheet.createRow(entryIndex + 1);
            row.createCell(0).setCellValue(entryIndex + 1);
            row.createCell(1).setCellValue(entry.title());
            row.createCell(2).setCellValue(formatDuration(entry.durationSeconds()));
            row.createCell(3).setCellValue(String.join(", ", entry.performers()));
            row.createCell(4).setCellValue(entry.fixed());
            row.createCell(5).setCellValue(entry.fixedPosition().toString());
            row.createCell(ENTRY_ID_COLUMN).setCellValue(entry.id().toString());
            row.createCell(SOURCE_PERFORMANCE_ID_COLUMN).setCellValue(entry.sourcePerformanceId().toString());
            row.createCell(FIXED_COLUMN).setCellValue(entry.fixed());
            row.createCell(FIXED_POSITION_COLUMN).setCellValue(entry.fixedPosition().name());
            row.createCell(FIXED_INDEX_COLUMN).setCellValue(entry.fixedIndex());
        }

        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new CellRangeAddress(0, Math.max(0, session.entries().size()), 0, DISPLAY_COLUMN_COUNT - 1));
        sheet.setColumnWidth(0, 8 * 256);
        sheet.setColumnWidth(1, 28 * 256);
        sheet.setColumnWidth(2, 10 * 256);
        sheet.setColumnWidth(3, 28 * 256);
        sheet.setColumnWidth(4, 10 * 256);
        sheet.setColumnWidth(5, 16 * 256);
        for (int column = ENTRY_ID_COLUMN; column <= FIXED_INDEX_COLUMN; column++) {
            sheet.setColumnHidden(column, true);
        }
    }

    private void writeMetadataSheet(Workbook workbook, SetlistProject project, CellStyle headerStyle) {
        Sheet metadata = workbook.createSheet(META_SHEET_NAME);
        Row formatRow = metadata.createRow(0);
        formatRow.createCell(0).setCellValue(FORMAT_KEY);
        formatRow.createCell(1).setCellValue(FORMAT_VERSION);

        Row header = metadata.createRow(2);
        String[] headers = {
                "公演番号", "公演名", "行番号", "演目ID", "元演目ID", "固定", "固定位置", "固定インデックス"
        };
        for (int column = 0; column < headers.length; column++) {
            Cell cell = header.createCell(column);
            cell.setCellValue(headers[column]);
            cell.setCellStyle(headerStyle);
        }
        Cell performerHeader = header.createCell(META_PERFORMER_LIST_START_COLUMN);
        performerHeader.setCellValue("演者一覧（右方向）");
        performerHeader.setCellStyle(headerStyle);

        int rowIndex = 3;
        for (int sessionIndex = 0; sessionIndex < project.sessions().size(); sessionIndex++) {
            SetlistSession session = project.sessions().get(sessionIndex);
            Row sessionRow = metadata.createRow(rowIndex++);
            sessionRow.createCell(0).setCellValue(sessionIndex);
            sessionRow.createCell(1).setCellValue(session.name());
            sessionRow.createCell(2).setCellValue(-1);
            for (int performerIndex = 0; performerIndex < session.performerNames().size(); performerIndex++) {
                sessionRow.createCell(META_PERFORMER_LIST_START_COLUMN + performerIndex)
                        .setCellValue(session.performerNames().get(performerIndex));
            }
            for (int entryIndex = 0; entryIndex < session.entries().size(); entryIndex++) {
                SetlistEntry entry = session.entries().get(entryIndex);
                Row row = metadata.createRow(rowIndex++);
                row.createCell(0).setCellValue(sessionIndex);
                row.createCell(1).setCellValue(session.name());
                row.createCell(2).setCellValue(entryIndex);
                row.createCell(3).setCellValue(entry.id().toString());
                row.createCell(4).setCellValue(entry.sourcePerformanceId().toString());
                row.createCell(5).setCellValue(entry.fixed());
                row.createCell(6).setCellValue(entry.fixedPosition().name());
                row.createCell(7).setCellValue(entry.fixedIndex());
            }
        }
        workbook.setSheetHidden(workbook.getSheetIndex(metadata), true);
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private String createUniqueSheetName(String sessionName, int sessionIndex, Set<String> usedSheetNames) {
        String baseName = WorkbookUtil.createSafeSheetName(sessionName);
        if (baseName == null || baseName.trim().isEmpty()) {
            baseName = "公演" + (sessionIndex + 1);
        }
        String candidate = baseName;
        int suffix = 2;
        while (!usedSheetNames.add(candidate)) {
            String suffixText = " (" + suffix++ + ")";
            candidate = baseName.substring(0, Math.min(baseName.length(), 31 - suffixText.length())) + suffixText;
        }
        return candidate;
    }

    private String formatDuration(int durationSeconds) {
        return String.format("%d:%02d", durationSeconds / 60, durationSeconds % 60);
    }
}
