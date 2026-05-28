package jp.ac.u_tokai.cc.javaadvanced;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
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

            // ヘッダーから演者名と列番号を記憶
            Row headerRow = sheet.getRow(0);
            if (headerRow == null){
                System.out.println("[デバッグ] 1行目が完全に存在しません！シートが空っぽの可能性大です。");
                return loadedMap;
            }

            // a1のセルを読めているかチェック
            String a1 = formatter.formatCellValue(headerRow.getCell(0));
            System.out.println("[デバッグ] ExcelのA1セルの中身は: [" + a1 + "] ");

            Map<Integer, String> performanceMap = new HashMap<>();
            int lastColIndex = headerRow.getLastCellNum() - 1; // 最終列は合計出演者数のため

            // 3列目から、最終列の１つ前までループ
            for (int i = 2; i < lastColIndex; i++) {
                String name = formatter.formatCellValue(headerRow.getCell(i));
                if (name != null && !name.trim().isEmpty()) {
                    performanceMap.put(i, name.trim()); // 列番号＝演者名として記憶
                }
            }

            // ２行目から最終行の１つ前までデータを読み込む
            int maxRowNumber = 20; 
            
            // lastRowNumを使わず、指定した maxRowNumber 未満までループする
            for (int r = 1; r < maxRowNumber; r++) {
                Row row = sheet.getRow(r);
                if (row == null) break; // 行自体が存在しなければ終了

                // 1列目(インデックス0): 曲名
                String title = formatter.formatCellValue(row.getCell(0));
                
                // 【安全装置1】「合計」行に到達したら、演目ではないのでストップ！
                if (title.equals("合計")) {
                    System.out.println("👀 [デバッグ] 「合計」行を検知したため、読み込みを終了します。");
                    break; // ループを抜け出す（終了）
                }

                // 【安全装置2】空欄が来たらスキップし続けるのではなく、即座にストップ！
                if (title == null || title.isEmpty()) {
                    System.out.println("👀 [デバッグ] " + (r + 1) + "行目が空欄のため、読み込みを終了します。");
                    break; // continue（スキップ）ではなく break（終了）にする
                }

                try {
                    // 2列目(インデックス1): 時間
                    String durationStr = formatter.formatCellValue(row.getCell(1)).trim();
                    int duration = 0;
                    
                    if (!durationStr.isEmpty()) {
                        String cleanTime = durationStr.replace("\"", "").replace("：",":");

                        if (cleanTime.contains(":")) {
                            String[] timeParts = cleanTime.split(":");
                            int min = Integer.parseInt(timeParts[0]);
                            int sec = Integer.parseInt(timeParts[1]);
                            duration = (min * 60) + sec;
                        } else{
                            duration = Integer.parseInt(cleanTime);
                        }
                    }

                    // 3列目以降をチェックし、「◯」があれば演者を追加
                    List<String> performers = new ArrayList<>();
                    for (int c = 2; c < lastColIndex; c++) {
                        String mark = formatter.formatCellValue(row.getCell(c)).trim();
                        // 記号の表記ゆれ対策
                        if (mark.equals("◯") || mark.equals("○") || mark.equals("〇") || mark.equalsIgnoreCase("O")) {
                            if (performanceMap.containsKey(c)) {
                                performers.add(performanceMap.get(c));
                            }
                        }
                    }

                    // ダミーデータ（BPM、雰囲気）
                    int bpm = 0;
                    String mood = "未指定";

                    Song song = new Song(title, performers, duration, bpm, mood);
                    loadedMap.put(song.getTitle(), song);
                    
                } catch (NumberFormatException e) {
                    // 時間が数字でない行はスキップ
                    String errDuration = formatter.formatCellValue(row.getCell(1));
                    System.out.println("👀 [デバッグ] " + (r + 1) + "行目の「" + title + "」をスキップしました！ 時間枠の [" + errDuration + "] が純粋な数字ではないためです。");
                }
            }

        } catch (

        Exception e) {
            System.out.println("エラー：.xlsxファイルの読み込みに失敗しました。");
            e.printStackTrace();

        }

        return loadedMap;

    }

}
