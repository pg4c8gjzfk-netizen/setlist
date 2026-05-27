package jp.ac.u_tokai.cc.javaadvanced;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

// Apache POI　のライブラリ群
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * .xlsxファイルから演目データを読み込む専用クラス
 */

public class DataLoader_Excel implements DataLoader {

    @Override
    public Map<String, Performance> load(File file) {
        Map<String, Performance> loadedMap = new HashMap<>();

        // try-with-resourcesでファイルを安全に開く
        try (FileInputStream fis = new FileInputStream(file);
                Workbook workbook = new XSSFWorkbook(fis)) {

            // １つめのシートを取得
            Sheet sheet = workbook.getSheetAt(0);

            // セルのデータを文字列としてきれいに取得するための便利ツール
            DataFormatter formatter = new DataFormatter();

            // シート内のすべての行(Row)をループ処理
            for (Row row : sheet) {
                // データが5列以上あるかチェック
                if (row.getLastCellNum() >= 5) {
                    try {
                        String title = formatter.formatCellValue(row.getCell(0));
                        String performer = formatter.formatCellValue(row.getCell(1));
                        int duration = Integer.parseInt(formatter.formatCellValue(row.getCell(2)));
                        int bpm = Integer.parseInt(formatter.formatCellValue(row.getCell(3)));
                        String mood = formatter.formatCellValue(row.getCell(4));

                        Song song = new Song(title, performer, duration, bpm, mood);
                        loadedMap.put(song.getTitle(), song);
                    } catch (NumberFormatException e) {
                        // ヘッダー行(見出し)などで数字に変換できない行はスキップ
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("エラー：.xlsxファイルの読み込みに失敗しました。");

        }

        return loadedMap;

    }

}
