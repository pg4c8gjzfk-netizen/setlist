package jp.ac.u_tokai.cc.javaadvanced;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
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
    private static final int MAX_ROW_NUMBER = 50;

    /**
     * Excelの各シートを独立した演目グループとして読み込みます。
     *
     * <p>同じ曲が複数シートに存在しても、別シートの演目として両方を保持します。</p>
     *
     * @param file 読み込み対象のExcelファイル
     * @return 元シート名と演目一覧
     */
    public List<PerformanceSheet> loadSheets(File file) {
        List<PerformanceSheet> performanceSheets = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(file);
                Workbook workbook = new XSSFWorkbook(fis)) {
            System.out.println("[システム] Excelファイルから " + workbook.getNumberOfSheets() + " 枚のシートを発見しました。");

            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                String sheetName = sheet.getSheetName();
                if (shouldSkipSheet(sheetName)) {
                    System.out.println("   シート「" + sheetName + "」はスキップします。");
                    continue;
                }
                List<Performance> sheetPerformances = loadSheet(sheet);
                if (!sheetPerformances.isEmpty()) {
                    performanceSheets.add(new PerformanceSheet(sheetName, sheetPerformances));
                }
            }
        } catch (Exception e) {
            System.out.println("エラー：.xlsxファイルの読み込みに失敗しました。");
            e.printStackTrace();
        }

        return List.copyOf(performanceSheets);
    }

    /**
     * 1シート分の演目データを読み込みます。
     */
    private List<Performance> loadSheet(Sheet sheet) {
        String sheetName = sheet.getSheetName();
        List<Performance> performances = new ArrayList<>();

        System.out.println("   シート「" + sheetName + "」の読み込みを開始します...");
        DataFormatter formatter = new DataFormatter();
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            System.out.println("[デバッグ] シート「" + sheetName + "」の1行目が存在しないためスキップします。");
            return performances;
        }

        Map<Integer, String> performerNameMap = readPerformerNames(headerRow, formatter);
        for (int rowIndex = 1; rowIndex < MAX_ROW_NUMBER; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                break;
            }

            String title = formatter.formatCellValue(row.getCell(0)).trim();
            if (title.equals("合計") || title.isEmpty()) {
                break;
            }

            try {
                Performance performance = createPerformance(row, title, performerNameMap, formatter);
                performances.add(performance);
            } catch (NumberFormatException e) {
                String durationText = formatter.formatCellValue(row.getCell(1));
                System.out.println("[デバッグ] " + (rowIndex + 1) + "行目の「" + title + "」をスキップしました。時間欄: " + durationText);
            }
        }
        return List.copyOf(performances);
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
        Map<Integer, String> performerNameMap = new HashMap<>();
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
            return 0;
        }

        String cleanTime = durationText.replace("\"", "").replace("：", ":");
        if (cleanTime.contains(":")) {
            String[] timeParts = cleanTime.split(":");
            int minutes = Integer.parseInt(timeParts[0]);
            int seconds = Integer.parseInt(timeParts[1]);
            return (minutes * 60) + seconds;
        }
        return Integer.parseInt(cleanTime);
    }

}
