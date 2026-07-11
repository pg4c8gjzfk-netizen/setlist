package jp.ac.u_tokai.cc.javaadvanced;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * 生成されたセットリストをExcelファイル（.xlsx）として保存するクラス。
 */
public class XlsxSetlistExporter {

    private final Path outputDirectory;

    /** 通常の出力先である Data/output を使用します。 */
    public XlsxSetlistExporter() {
        this(Path.of("Data", "output"));
    }

    /** テストなどで出力先を差し替えます。 */
    XlsxSetlistExporter(Path outputDirectory) {
        this.outputDirectory = Objects.requireNonNull(
                outputDirectory, "outputDirectory must not be null");
    }

    /**
     * 生成されたセットリストをExcelファイルに書き出します。
     * 公演ごとにシートを分け、後から実際の表計算ソフトで確認しやすい形で保存します。
     *
     * @param performanceSheets 元シート名と所属を保持した生成結果
     * @param fileName 出力するファイル名（例: "output_setlist.xlsx"）
     * @return 保存したファイル
     * @throws IOException 保存に失敗した場合
     */
    public File export(List<PerformanceSheet> performanceSheets, String fileName) throws IOException {
        Objects.requireNonNull(performanceSheets, "performanceSheets must not be null");
        if (performanceSheets.isEmpty()) {
            throw new IllegalArgumentException("出力する公演シートがありません。");
        }
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("出力ファイル名を指定してください。");
        }

        Files.createDirectories(outputDirectory);
        File outputFile = outputDirectory.resolve(fileName).toFile();

        try (Workbook workbook = new XSSFWorkbook();
                FileOutputStream fos = new FileOutputStream(outputFile)) {
            for (PerformanceSheet performanceSheet : performanceSheets) {
                writeSessionSheet(workbook, performanceSheet);
            }

            workbook.write(fos);
        }
        System.out.println("[成功] セットリストをExcel形式で保存しました: " + outputFile.getAbsolutePath());
        return outputFile;
    }

    /**
     * 1公演分のセットリストを1つのシートへ書き込みます。
     */
    private void writeSessionSheet(Workbook workbook, PerformanceSheet performanceSheet) {
        Sheet sheet = workbook.createSheet(performanceSheet.name());
        List<Performance> performances = performanceSheet.performances();
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("順番");
        headerRow.createCell(1).setCellValue("演目名");
        headerRow.createCell(2).setCellValue("時間");
        headerRow.createCell(3).setCellValue("出演者");

        for (int i = 0; i < performances.size(); i++) {
            Row row = sheet.createRow(i + 1);
            Performance performance = performances.get(i);
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
