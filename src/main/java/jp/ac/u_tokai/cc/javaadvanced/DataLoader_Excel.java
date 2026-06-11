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

    /**
     * Excelファイルからデータを読み込み、Mapに格納して返します。
     * 重複データの排除や、シートごとのリネーム処理を含みます。
     * 
     * @param file 読み込み対象のExcelファイル
     * @return タイトルをキー、演目を値とするMap
     */

    @Override
    public Map<String, Performance> load(File file) {
        Map<String, Performance> loadedMap = new HashMap<>();

        // try-with-resourcesでファイルを安全に開く
        try (FileInputStream fis = new FileInputStream(file);
                Workbook workbook = new XSSFWorkbook(fis)) {

            // 【追加】Excelファイルの中にあるシートの総数を取得
            int numberOfSheets = workbook.getNumberOfSheets();
            System.out.println("👀 [システム] Excelファイルから " + numberOfSheets + " 枚のシートを発見しました。");

            // 【追加】全シートを順番に処理するループ
            for (int s = 0; s < numberOfSheets; s++) {
                Sheet sheet = workbook.getSheetAt(s);
                String sheetName = sheet.getSheetName();

                // 【安全装置】シート名に「メモ」や「予備」が含まれていたら読み飛ばす
                if (sheetName.contains("メモ") || sheetName.contains("予備")) {
                    System.out.println("   ➡ シート「" + sheetName + "」はスキップします。");
                    continue;
                }

                System.out.println("   ➡ シート「" + sheetName + "」の読み込みを開始します...");

                // セルのデータを文字列としてきれいに取得するための便利ツール
                DataFormatter formatter = new DataFormatter();

                // ヘッダーから演者名と列番号を記憶
                Row headerRow = sheet.getRow(0);
                if (headerRow == null) {
                    // 【変更】複数シート対応のため、return(完全終了)ではなくcontinue(次のシートへ)に変更
                    System.out.println("[デバッグ] シート「" + sheetName + "」の1行目が完全に存在しません！シートが空っぽの可能性大です。");
                    continue;
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
                int maxRowNumber = 50; // ※演目が多い場合に備えて20から50に変更しています

                // lastRowNumを使わず、指定した maxRowNumber 未満までループする
                for (int r = 1; r < maxRowNumber; r++) {
                    Row row = sheet.getRow(r);
                    if (row == null)
                        break; // 行自体が存在しなければ終了

                    // 1列目(インデックス0): 曲名
                    String title = formatter.formatCellValue(row.getCell(0));

                    // 【安全装置1】「合計」行に到達したら、演目ではないのでストップ！
                    if (title.equals("合計")) {
                        System.out.println("👀 [デバッグ] シート「" + sheetName + "」で「合計」行を検知したため、読み込みを終了します。");
                        break; // ループを抜け出す（終了）
                    }

                    // 【安全装置2】空欄が来たらスキップし続けるのではなく、即座にストップ！
                    if (title == null || title.isEmpty()) {
                        System.out.println("👀 [デバッグ] シート「" + sheetName + "」の" + (r + 1) + "行目が空欄のため、読み込みを終了します。");
                        break; // continue（スキップ）ではなく break（終了）にする
                    }

                    try {
                        // 2列目(インデックス1): 時間
                        String durationStr = formatter.formatCellValue(row.getCell(1)).trim();
                        int duration = 0;

                        if (!durationStr.isEmpty()) {
                            String cleanTime = durationStr.replace("\"", "").replace("：", ":");

                            if (cleanTime.contains(":")) {
                                String[] timeParts = cleanTime.split(":");
                                int min = Integer.parseInt(timeParts[0]);
                                int sec = Integer.parseInt(timeParts[1]);
                                duration = (min * 60) + sec;
                            } else {
                                duration = Integer.parseInt(cleanTime);
                            }
                        }

                        // 3列目以降をチェックし、「◯」があれば演者を追加
                        List<String> performers = new ArrayList<>();
                        for (int c = 2; c < lastColIndex; c++) {
                            String mark = formatter.formatCellValue(row.getCell(c)).trim();
                            // 記号の表記ゆれ対策
                            if (mark.equals("◯") || mark.equals("○") || mark.equals("〇")
                                    || mark.equalsIgnoreCase("O")) {
                                if (performanceMap.containsKey(c)) {
                                    performers.add(performanceMap.get(c));
                                }
                            }
                        }

                        // ダミーデータ（BPM、雰囲気）
                        int bpm = 0;
                        String mood = "未指定";

                        // 【追加】全く同じメンバーの同じ演目かチェックするロジック
                        boolean isDuplicate = false;

                        // 順番を気にせずメンバーを比較するために HashSet を使います
                        java.util.Set<String> newPerformersSet = new java.util.HashSet<>(performers);

                        // 既に登録されている全演目をチェック
                        for (Performance existing : loadedMap.values()) {
                            // タイトルが同じ、または派生タイトル（"(_" で始まる）かチェック
                            if (existing.getTitle().equals(title) || existing.getTitle().startsWith(title + "(_")) {

                                java.util.Set<String> existingPerformersSet = new java.util.HashSet<>(
                                        existing.getPerformers());

                                // メンバーが完全に一致した場合は重複（同じ演目）とみなす
                                if (existingPerformersSet.equals(newPerformersSet)) {
                                    isDuplicate = true;
                                    break; // 見つかったらこれ以上探す必要はないのでループを抜ける
                                }
                            }
                        }

                        // メンバーが一致した（重複した）場合は登録せずにスキップ
                        if (isDuplicate) {
                            System.out.println(
                                    "👀 [デバッグ] シート「" + sheetName + "」の「" + title + "」は、既に同じメンバーで登録済みのためスキップしました。");
                        } else {
                            // メンバーが違う場合、または完全に新曲の場合は登録・リネーム処理へ
                            String finalTitle = title;

                            // もし辞書にすでに同じ名前が登録されていたら、後ろに (_シート名) を付ける
                            if (loadedMap.containsKey(finalTitle)) {
                                finalTitle = title + "(_" + sheetName + ")";

                                // 万が一、同シート内に同じ曲名が複数あった場合の対策
                                int version = 1;
                                String tempTitle = finalTitle;
                                while (loadedMap.containsKey(tempTitle)) {
                                    version++;
                                    tempTitle = finalTitle + " (ver" + version + ")";
                                }
                                finalTitle = tempTitle;
                            }

                            // Songを作成してMapに登録
                            Song song = new Song(finalTitle, performers, duration, bpm, mood);
                            loadedMap.put(song.getTitle(), song);
                        }

                    } catch (NumberFormatException e) {
                        // 時間が数字でない行はスキップ
                        String errDuration = formatter.formatCellValue(row.getCell(1));
                        System.out.println("👀 [デバッグ] " + (r + 1) + "行目の「" + title + "」をスキップしました！ 時間枠の [" + errDuration
                                + "] が純粋な数字ではないためです。");
                    }
                }
            } // ◀◀ 全シートを回る for ループの終わり

        } catch (

        Exception e) {
            System.out.println("エラー：.xlsxファイルの読み込みに失敗しました。");
            e.printStackTrace();

        }

        return loadedMap;

    }

}