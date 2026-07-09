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
 * 生成されたセットリストをExcelファイル（.xlsx）として保存するクラス。
 */
public class ExcelDataExporter implements DataExporter {

    /**
     * 生成されたセットリストをExcelファイルに書き出します。
     * 公演ごとにシートを分け、後から実際の表計算ソフトで確認しやすい形で保存します。
     *
     * @param sessions 生成されたセットリスト
     * @param fileName 出力するファイル名（例: "output_setlist.xlsx"）
     */
    @Override
    public void export(List<List<Performance>> sessions, String fileName) {
        File outputDir = new File("Data");
        if (!outputDir.exists() && !outputDir.mkdir()) {
            System.out.println("[エラー] Dataフォルダを作成できませんでした。");
            return;
        }

        File outputFile = new File(outputDir, fileName);

        try (Workbook workbook = new XSSFWorkbook();
                FileOutputStream fos = new FileOutputStream(outputFile)) {
            for (int i = 0; i < sessions.size(); i++) {
                writeSessionSheet(workbook, sessions.get(i), i + 1);
            }

            workbook.write(fos);
            System.out.println("[成功] セットリストをExcel形式で保存しました: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            System.out.println("[エラー] Excelファイルの保存中にエラーが発生しました。");
            e.printStackTrace();
        }
    }

    /**
     * 1公演分のセットリストを1つのシートへ書き込みます。
     */
    private void writeSessionSheet(Workbook workbook, List<Performance> session, int sessionNumber) {
        Sheet sheet = workbook.createSheet("第" + sessionNumber + "公演");
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("順番");
        headerRow.createCell(1).setCellValue("演目名");
        headerRow.createCell(2).setCellValue("時間");
        headerRow.createCell(3).setCellValue("出演者");

        for (int i = 0; i < session.size(); i++) {
            Row row = sheet.createRow(i + 1);
            Performance performance = session.get(i);
            row.createCell(0).setCellValue(i + 1);
            row.createCell(1).setCellValue(performance.getDisplayTitle());
            row.createCell(2).setCellValue(formatDuration(performance.getDuration()));
            row.createCell(3).setCellValue(String.join(", ", performance.getPerformers()));
        }

        for (int col = 0; col < 4; col++) {
            sheet.autoSizeColumn(col);
        }
    }

    /**
     * 秒数を分:秒の形式に変換します。
     */
    private String formatDuration(int durationSeconds) {
        int minutes = durationSeconds / 60;
        int seconds = durationSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}