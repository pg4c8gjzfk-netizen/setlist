package jp.ac.u_tokai.cc.javaadvanced;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/** {@link XlsxSetlistProjectWriter} が保存した編集可能XLSXを読み込みます。 */
public final class XlsxSetlistProjectReader {

    /**
     * 編集可能な香盤表XLSXを読み込みます。
     *
     * @param inputFile 入力ファイル
     * @return 復元したプロジェクト
     * @throws IOException 読み込みに失敗した場合
     * @throws IllegalArgumentException 編集可能な香盤表形式でない場合
     */
    public SetlistProject read(File inputFile) throws IOException {
        if (inputFile == null || !inputFile.isFile()) {
            throw new IllegalArgumentException("読み込むXLSXファイルを選択してください。");
        }
        try (FileInputStream input = new FileInputStream(inputFile);
                Workbook workbook = new XSSFWorkbook(input)) {
            Sheet metadata = workbook.getSheet(XlsxSetlistProjectWriter.META_SHEET_NAME);
            validateFormat(metadata);
            MetadataContent metadataContent = readMetadata(metadata);
            return readProjectFromDisplaySheets(workbook, metadataContent);
        }
    }

    private void validateFormat(Sheet metadata) {
        if (metadata == null
                || !XlsxSetlistProjectWriter.FORMAT_KEY.equals(cellText(metadata.getRow(0), 0))
                || !XlsxSetlistProjectWriter.FORMAT_VERSION.equals(cellText(metadata.getRow(0), 1))) {
            throw new IllegalArgumentException("このファイルは再生成可能な香盤表XLSXではありません。");
        }
    }

    private MetadataContent readMetadata(Sheet metadata) {
        List<MetadataRow> rows = new ArrayList<>();
        Map<Integer, String> sessionNames = new HashMap<>();
        for (int rowIndex = 3; rowIndex <= metadata.getLastRowNum(); rowIndex++) {
            Row row = metadata.getRow(rowIndex);
            if (row == null || cellText(row, 0).isBlank()) {
                continue;
            }
            try {
                int sessionIndex = Integer.parseInt(cellText(row, 0));
                String sessionName = cellText(row, 1);
                int entryIndex = Integer.parseInt(cellText(row, 2));
                String previousName = sessionNames.putIfAbsent(sessionIndex, sessionName);
                if (previousName != null && !previousName.equals(sessionName)) {
                    throw new IllegalArgumentException("_setlist_meta シートの公演名が一致しません。");
                }
                if (entryIndex == -1) {
                    continue;
                }
                rows.add(new MetadataRow(
                        sessionIndex,
                        sessionName,
                        entryIndex,
                        UUID.fromString(cellText(row, 3)),
                        UUID.fromString(cellText(row, 4)),
                        parseBoolean(cellText(row, 5)),
                        FixedPosition.valueOf(cellText(row, 6)),
                        Integer.parseInt(cellText(row, 7))));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("_setlist_meta シートの " + (rowIndex + 1) + " 行目が不正です。", exception);
            }
        }
        return new MetadataContent(sessionNames, rows);
    }

    private SetlistProject readProjectFromDisplaySheets(Workbook workbook, MetadataContent metadataContent) {
        Map<Integer, List<MetadataRow>> entriesBySession = new HashMap<>();
        for (MetadataRow metadataRow : metadataContent.rows()) {
            entriesBySession.computeIfAbsent(metadataRow.sessionIndex(), ignored -> new ArrayList<>()).add(metadataRow);
        }

        List<SetlistSession> sessions = new ArrayList<>();
        int displaySessionIndex = 0;
        for (int sessionIndex = 0; sessionIndex < workbook.getNumberOfSheets(); sessionIndex++) {
            Sheet sheet = workbook.getSheetAt(sessionIndex);
            if (XlsxSetlistProjectWriter.META_SHEET_NAME.equals(sheet.getSheetName())) {
                continue;
            }
            if (!metadataContent.sessionNames().containsKey(displaySessionIndex)) {
                throw new IllegalArgumentException("_setlist_meta シートに公演情報がありません。");
            }
            List<MetadataRow> sessionRows = entriesBySession.getOrDefault(displaySessionIndex, List.of());
            List<SetlistEntry> entries = new ArrayList<>();
            sessionRows.stream()
                    .sorted(Comparator.comparingInt(MetadataRow::entryIndex))
                    .forEach(metadata -> entries.add(readEntry(sheet, metadata)));
            String sessionName = metadataContent.sessionNames().get(displaySessionIndex);
            sessions.add(new SetlistSession(sessionName, entries));
            displaySessionIndex++;
        }

        if (sessions.isEmpty() || metadataContent.sessionNames().size() != sessions.size()) {
            throw new IllegalArgumentException("公演シートがありません。");
        }
        SetlistProject project = new SetlistProject(sessions);
        new SetlistRegenerator().validate(project);
        return project;
    }

    private SetlistEntry readEntry(Sheet sheet, MetadataRow metadata) {
        Row row = sheet.getRow(metadata.entryIndex() + 1);
        if (row == null) {
            throw new IllegalArgumentException(sheet.getSheetName() + " の " + (metadata.entryIndex() + 2) + " 行目がありません。");
        }
        verifyHiddenMetadata(row, metadata, sheet.getSheetName());
        return new SetlistEntry(
                metadata.entryId(),
                metadata.sourcePerformanceId(),
                requiredCellText(row, 1, "曲名"),
                parseDuration(requiredCellText(row, 2, "時間")),
                parsePerformers(requiredCellText(row, 3, "出演者")),
                metadata.fixed(),
                metadata.fixedPosition(),
                metadata.fixedIndex());
    }

    private void verifyHiddenMetadata(Row row, MetadataRow metadata, String sheetName) {
        if (!metadata.entryId().toString().equals(cellText(row, XlsxSetlistProjectWriter.ENTRY_ID_COLUMN))
                || !metadata.sourcePerformanceId().toString().equals(cellText(row, XlsxSetlistProjectWriter.SOURCE_PERFORMANCE_ID_COLUMN))
                || metadata.fixed() != parseBoolean(cellText(row, XlsxSetlistProjectWriter.FIXED_COLUMN))
                || !metadata.fixedPosition().name().equals(cellText(row, XlsxSetlistProjectWriter.FIXED_POSITION_COLUMN))
                || metadata.fixedIndex() != Integer.parseInt(cellText(row, XlsxSetlistProjectWriter.FIXED_INDEX_COLUMN))) {
            throw new IllegalArgumentException(sheetName + " の非表示メタデータが _setlist_meta と一致しません。");
        }
    }

    private String requiredCellText(Row row, int column, String columnName) {
        String value = cellText(row, column);
        if (value.isBlank()) {
            throw new IllegalArgumentException(columnName + " は空欄にできません。");
        }
        return value;
    }

    private int parseDuration(String value) {
        if (!value.matches("\\d+:\\d{2}")) {
            throw new IllegalArgumentException("時間は m:ss 形式で入力してください。");
        }
        String[] values = value.split(":", -1);
        int minutes = Integer.parseInt(values[0]);
        int seconds = Integer.parseInt(values[1]);
        if (seconds > 59) {
            throw new IllegalArgumentException("秒は 0〜59 の範囲で入力してください。");
        }
        return Math.addExact(Math.multiplyExact(minutes, 60), seconds);
    }

    private List<String> parsePerformers(String value) {
        List<String> performers = List.of(value.split("[,、]", -1)).stream()
                .map(String::trim)
                .filter(candidate -> !candidate.isEmpty())
                .toList();
        if (performers.isEmpty()) {
            throw new IllegalArgumentException("出演者名は空欄にできません。");
        }
        return performers;
    }

    private boolean parseBoolean(String value) {
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        throw new IllegalArgumentException("固定状態が不正です。");
    }

    private String cellText(Row row, int column) {
        if (row == null) {
            return "";
        }
        Cell cell = row.getCell(column);
        return cell == null ? "" : new DataFormatter().formatCellValue(cell).trim();
    }

    private record MetadataRow(
            int sessionIndex,
            String sessionName,
            int entryIndex,
            UUID entryId,
            UUID sourcePerformanceId,
            boolean fixed,
            FixedPosition fixedPosition,
            int fixedIndex) {
    }

    private record MetadataContent(Map<Integer, String> sessionNames, List<MetadataRow> rows) {
    }
}
