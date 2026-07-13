package jp.ac.u_tokai.cc.javaadvanced;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Excelファイル（.xlsx）から演目データを読み込むクラス。
 */
public class XlsxPerformanceReader {
    private static final int FIRST_PERFORMER_COLUMN = 2;
    private static final int MAX_REPORTED_PROBLEMS = 10;

    /**
     * Excelの各シートを独立した演目グループとして読み込みます。
     *
     * <p>同じ曲が複数シートに存在しても、別シートの演目として両方を保持します。</p>
     *
     * @param file 読み込み対象のExcelファイル
     * @return 元シート名と演目一覧
     */
    public List<PerformanceSheet> loadSheets(File file) {
        validateInputFile(file);
        List<PerformanceSheet> performanceSheets = new ArrayList<>();
        List<String> problems = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(file);
                Workbook workbook = new XSSFWorkbook(fis)) {
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                String sheetName = sheet.getSheetName();
                if (shouldSkipSheet(sheetName)) {
                    continue;
                }
                LoadedSheet loadedSheet = loadSheet(sheet, problems);
                performanceSheets.add(new PerformanceSheet(
                        sheetName, loadedSheet.performances(), loadedSheet.performerNames()));
            }
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof XlsxImportException importException) {
                throw importException;
            }
            throw new XlsxImportException(
                    "XLSXファイルを読み込めませんでした: " + readableMessage(exception), exception);
        }

        if (!problems.isEmpty()) {
            throw new XlsxImportException(formatProblems(problems));
        }

        return List.copyOf(performanceSheets);
    }

    /**
     * 1シート分の演目データを読み込みます。
     */
    private LoadedSheet loadSheet(Sheet sheet, List<String> problems) {
        String sheetName = sheet.getSheetName();
        List<Performance> performances = new ArrayList<>();

        DataFormatter formatter = new DataFormatter();
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            problems.add(problem(
                    sheetName,
                    0,
                    "見出し行がありません。1行目に曲名・時間・出演者・人数の見出しを入力してください。"));
            return new LoadedSheet(List.of(), List.of());
        }

        Map<Integer, String> performerNameMap = readPerformerNames(headerRow, formatter);
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }

            String title = formatter.formatCellValue(row.getCell(0)).trim();
            if (title.equals("合計")) {
                break;
            }
            if (title.isEmpty()) {
                if (hasInputValues(row, formatter)) {
                    problems.add(problem(sheetName, rowIndex, "曲名が空欄です。"));
                }
                continue;
            }

            try {
                Performance performance = createPerformance(row, title, performerNameMap, formatter);
                performances.add(performance);
            } catch (IllegalArgumentException exception) {
                problems.add(problem(sheetName, rowIndex, exception.getMessage()));
            }
        }
        return new LoadedSheet(
                List.copyOf(performances),
                List.copyOf(performerNameMap.values()));
    }

    /**
     * 読み飛ばすシートかどうかを判定します。
     */
    private boolean shouldSkipSheet(String sheetName) {
        return sheetName.contains("メモ") || sheetName.contains("予備");
    }

    /**
     * ヘッダー行から出演者名と列番号の対応を読み取ります。
     */
    private Map<Integer, String> readPerformerNames(Row headerRow, DataFormatter formatter) {
        Map<Integer, String> performerNameMap = new LinkedHashMap<>();
        int lastPerformerColumn = headerRow.getLastCellNum() - 1;
        for (int columnIndex = FIRST_PERFORMER_COLUMN; columnIndex < lastPerformerColumn; columnIndex++) {
            String name = formatter.formatCellValue(headerRow.getCell(columnIndex)).trim();
            if (!name.isEmpty()) {
                performerNameMap.put(columnIndex, name);
            }
        }
        return performerNameMap;
    }

    /**
     * Excelの1行から演目オブジェクトを作成します。
     */
    private Performance createPerformance(
            Row row,
            String title,
            Map<Integer, String> performerNameMap,
            DataFormatter formatter) {
        int duration = parseDuration(formatter.formatCellValue(row.getCell(1)).trim());
        List<String> performers = readPerformers(row, performerNameMap, formatter);
        return new Song(title, performers, duration, 0, "未指定");
    }

    /**
     * 出演者欄の丸印から出演者一覧を作成します。
     */
    private List<String> readPerformers(Row row, Map<Integer, String> performerNameMap, DataFormatter formatter) {
        List<String> performers = new ArrayList<>();
        for (Map.Entry<Integer, String> entry : performerNameMap.entrySet()) {
            String mark = formatter.formatCellValue(row.getCell(entry.getKey())).trim();
            if (isCircleMark(mark)) {
                performers.add(entry.getValue());
            }
        }
        return performers;
    }

    /**
     * 出演を示す丸印かどうかを判定します。
     */
    private boolean isCircleMark(String mark) {
        return mark.equals("◯") || mark.equals("○") || mark.equals("〇") || mark.equalsIgnoreCase("O");
    }

    /**
     * m:ssまたは秒数の文字列を秒数へ変換します。
     */
    private int parseDuration(String durationText) {
        if (durationText.isEmpty()) {
            throw new IllegalArgumentException("時間が空欄です。m:ss形式で入力してください。");
        }

        String cleanTime = durationText.replace("\"", "").replace("：", ":");
        if (cleanTime.contains(":")) {
            if (!cleanTime.matches("\\d+:\\d{2}")) {
                throw new IllegalArgumentException("時間「" + durationText + "」はm:ss形式ではありません。");
            }
            String[] timeParts = cleanTime.split(":", -1);
            int minutes = parseNonNegativeNumber(timeParts[0], durationText);
            int seconds = parseNonNegativeNumber(timeParts[1], durationText);
            if (seconds > 59) {
                throw new IllegalArgumentException("時間「" + durationText + "」の秒は0〜59で入力してください。");
            }
            try {
                return Math.addExact(Math.multiplyExact(minutes, 60), seconds);
            } catch (ArithmeticException exception) {
                throw new IllegalArgumentException("時間「" + durationText + "」が大きすぎます。");
            }
        }
        return parseNonNegativeNumber(cleanTime, durationText);
    }

    private int parseNonNegativeNumber(String value, String originalText) {
        try {
            int number = Integer.parseInt(value);
            if (number < 0) {
                throw new NumberFormatException();
            }
            return number;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("時間「" + originalText + "」はm:ss形式ではありません。");
        }
    }

    private boolean hasInputValues(Row row, DataFormatter formatter) {
        for (int columnIndex = 1; columnIndex < row.getLastCellNum(); columnIndex++) {
            if (!formatter.formatCellValue(row.getCell(columnIndex)).trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private String problem(String sheetName, int zeroBasedRowIndex, String detail) {
        return "シート「" + sheetName + "」の" + (zeroBasedRowIndex + 1) + "行目: " + detail;
    }

    private String formatProblems(List<String> problems) {
        StringBuilder message = new StringBuilder("XLSXの入力内容に問題があります。修正してから再読込してください。\n");
        int reportCount = Math.min(problems.size(), MAX_REPORTED_PROBLEMS);
        for (int index = 0; index < reportCount; index++) {
            message.append("・").append(problems.get(index)).append('\n');
        }
        if (problems.size() > reportCount) {
            message.append("・ほか").append(problems.size() - reportCount).append("件");
        }
        return message.toString().trim();
    }

    private void validateInputFile(File file) {
        if (file == null || !file.isFile()) {
            throw new XlsxImportException("読み込むXLSXファイルが見つかりません。");
        }
    }

    private String readableMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    private record LoadedSheet(List<Performance> performances, List<String> performerNames) {
    }

    /** 入力XLSXを安全に読み込めない場合の例外です。 */
    public static final class XlsxImportException extends IllegalArgumentException {
        private static final long serialVersionUID = 1L;

        XlsxImportException(String message) {
            super(message);
        }

        XlsxImportException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}
