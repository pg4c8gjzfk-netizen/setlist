package jp.ac.u_tokai.cc.javaadvanced;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Excelファイル（.xlsx）から演目データを読み込むクラス。
 */
public class ExcelDataLoader implements DataLoader {
    private static final int FIRST_PERFORMER_COLUMN = 2;
    private static final int MAX_ROW_NUMBER = 50;

    /**
     * Excelファイルからデータを読み込み、Mapに格納して返します。
     * 複数シートを処理し、同じ演目名でも出演者が異なる場合は別演目として扱います。
     *
     * @param file 読み込み対象のExcelファイル
     * @return タイトルをキー、演目を値とするMap
     */
    @Override
    public Map<String, Performance> load(File file) {
        Map<String, Performance> loadedMap = new HashMap<>();

        try (FileInputStream fis = new FileInputStream(file);
                Workbook workbook = new XSSFWorkbook(fis)) {
            System.out.println("[システム] Excelファイルから " + workbook.getNumberOfSheets() + " 枚のシートを発見しました。");

            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                loadSheet(sheet, loadedMap);
            }
        } catch (Exception e) {
            System.out.println("エラー：.xlsxファイルの読み込みに失敗しました。");
            e.printStackTrace();
        }

        return loadedMap;
    }

    /**
     * 1シート分の演目データを読み込みます。
     */
    private void loadSheet(Sheet sheet, Map<String, Performance> loadedMap) {
        String sheetName = sheet.getSheetName();
        if (shouldSkipSheet(sheetName)) {
            System.out.println("   シート「" + sheetName + "」はスキップします。");
            return;
        }

        System.out.println("   シート「" + sheetName + "」の読み込みを開始します...");
        DataFormatter formatter = new DataFormatter();
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            System.out.println("[デバッグ] シート「" + sheetName + "」の1行目が存在しないためスキップします。");
            return;
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
                addPerformance(loadedMap, performance, sheetName);
            } catch (NumberFormatException e) {
                String durationText = formatter.formatCellValue(row.getCell(1));
                System.out.println("[デバッグ] " + (rowIndex + 1) + "行目の「" + title + "」をスキップしました。時間欄: " + durationText);
            }
        }
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

    /**
     * 重複を確認しながら演目を登録します。
     */
    private void addPerformance(Map<String, Performance> loadedMap, Performance performance, String sheetName) {
        if (isDuplicate(loadedMap, performance)) {
            System.out.println("[デバッグ] 「" + performance.getTitle() + "」は同じ出演者で登録済みのためスキップしました。");
            return;
        }

        String finalTitle = performance.getTitle();
        if (loadedMap.containsKey(finalTitle)) {
            finalTitle = createUniqueTitle(loadedMap, performance.getTitle(), sheetName);
        }

        Song song = new Song(finalTitle, performance.getPerformers(), performance.getDuration(), 0, "未指定");
        loadedMap.put(song.getTitle(), song);
    }

    /**
     * タイトルと出演者が同じ演目がすでに登録済みか判定します。
     */
    private boolean isDuplicate(Map<String, Performance> loadedMap, Performance performance) {
        Set<String> newPerformersSet = new HashSet<>(performance.getPerformers());
        for (Performance existing : loadedMap.values()) {
            if (existing.getDisplayTitle().equals(performance.getDisplayTitle())) {
                Set<String> existingPerformersSet = new HashSet<>(existing.getPerformers());
                if (existingPerformersSet.equals(newPerformersSet)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 同じ曲名で出演者が違う演目に一意なタイトルを付けます。
     */
    private String createUniqueTitle(Map<String, Performance> loadedMap, String title, String sheetName) {
        String baseTitle = title + "(_" + sheetName + ")";
        String candidate = baseTitle;
        int version = 1;
        while (loadedMap.containsKey(candidate)) {
            version++;
            candidate = baseTitle + " (ver" + version + ")";
        }
        return candidate;
    }
}