package jp.ac.u_tokai.cc.javaadvanced;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * 生成されたセットリストをExcelファイル（.xlsx）として出力するクラス
 */
public class DataExporter_Excel {

    /**
     * 生成されたセットリストをExcelファイルに書き出します。
     * 公演（セッション）ごとにシートを分けて見やすく出力します。
     * 
     * @param sessions 生成されたセットリスト
     * @param fileName 出力するファイル名（例: "output_setlist.xlsx"）
     */
    public void export(List<List<Performance>> sessions, String fileName) {
        // Dataフォルダの中に出力する
        File outputDir = new File("Data");
        if (!outputDir.exists()) {
            outputDir.mkdir();
        }

        File outputFile = new File(outputDir, fileName);

        // Apache POIを使って新しいワークブック（Excelファイル）を作成
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(outputFile)) {

            // 各公演ごとに新しいシートを作成する
            for (int i = 0; i < sessions.size(); i++) {
                String sessionName = "第" + (i + 1) + "公演";
                Sheet sheet = workbook.createSheet(sessionName);

                // 1行目：ヘッダー行の作成
                Row headerRow = sheet.createRow(0);
                headerRow.createCell(0).setCellValue("順番");
                headerRow.createCell(1).setCellValue("演目名");
                headerRow.createCell(2).setCellValue("時間");
                headerRow.createCell(3).setCellValue("出演者");

                List<Performance> session = sessions.get(i);
                
                // 2行目以降：データの書き込み
                for (int j = 0; j < session.size(); j++) {
                    Row row = sheet.createRow(j + 1);
                    Performance p = session.get(j);

                    int order = j + 1;
                    // ベースの曲名（"(_" などを除去した表示用名）
                    String displayTitle = p.getTitle().split("\\(_")[0];
                    
                    // 時間のフォーマット
                    int m = p.getDuration() / 60;
                    int s = p.getDuration() % 60;
                    String formattedTime = String.format("%d:%02d", m, s);
                    
                    // 出演者をカンマ区切りの文字列に変換
                    String performersStr = String.join(", ", p.getPerformers());

                    // セルに値をセット
                    row.createCell(0).setCellValue(order);
                    row.createCell(1).setCellValue(displayTitle);
                    row.createCell(2).setCellValue(formattedTime);
                    row.createCell(3).setCellValue(performersStr);
                }
                
                // 列幅の自動調整（見やすくするため）
                for (int col = 0; col < 4; col++) {
                    sheet.autoSizeColumn(col);
                }
            }

            // ファイルに書き出し
            workbook.write(fos);
            System.out.println("✅ [成功] セットリストをExcel形式（.xlsx）で保存しました: " + outputFile.getAbsolutePath());

        } catch (IOException e) {
            System.out.println("❌ [エラー] Excelファイルの保存中にエラーが発生しました。");
            e.printStackTrace();
        }
    }
}
