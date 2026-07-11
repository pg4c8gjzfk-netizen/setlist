package jp.ac.u_tokai.cc.javaadvanced;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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

        List<ExportSession> sessions = new ArrayList<>();
        for (PerformanceSheet performanceSheet : performanceSheets) {
            List<ExportEntry> entries = performanceSheet.performances().stream()
                    .map(performance -> new ExportEntry(
                            performance.getDisplayTitle(),
                            performance.getDuration(),
                            performance.getPerformers()))
                    .toList();
            sessions.add(new ExportSession(performanceSheet.name(), entries));
        }
        return exportSessions(sessions, outputDirectory.resolve(fileName).toFile());
    }

    /**
     * 編集画面の最新プロジェクトを配布用XLSXへ書き出します。
     *
     * @param project 現在の編集内容
     * @param outputFile 保存先
     * @return 保存したファイル
     * @throws IOException 保存に失敗した場合
     */
    public File export(SetlistProject project, File outputFile) throws IOException {
        Objects.requireNonNull(project, "project must not be null");
        if (project.sessions().isEmpty()) {
            throw new IllegalArgumentException("出力する公演シートがありません。");
        }

        List<ExportSession> sessions = new ArrayList<>();
        for (SetlistSession session : project.sessions()) {
            List<ExportEntry> entries = session.entries().stream()
                    .map(entry -> new ExportEntry(
                            entry.title(), entry.durationSeconds(), entry.performers()))
                    .toList();
            sessions.add(new ExportSession(session.name(), entries));
        }
        return exportSessions(sessions, outputFile);
    }

    private File exportSessions(List<ExportSession> sessions, File outputFile) throws IOException {
        Objects.requireNonNull(outputFile, "outputFile must not be null");
        File absoluteFile = outputFile.getAbsoluteFile();
        File parentDirectory = absoluteFile.getParentFile();
        if (parentDirectory != null) {
            Files.createDirectories(parentDirectory.toPath());
        }

        try (Workbook workbook = new XSSFWorkbook();
                FileOutputStream fos = new FileOutputStream(absoluteFile)) {
            for (ExportSession session : sessions) {
                writeSessionSheet(workbook, session);
            }

            workbook.write(fos);
        }
        System.out.println("[成功] セットリストをExcel形式で保存しました: " + absoluteFile.getAbsolutePath());
        return absoluteFile;
    }

    /**
     * 1公演分のセットリストを1つのシートへ書き込みます。
     */
    private void writeSessionSheet(Workbook workbook, ExportSession session) {
        Sheet sheet = workbook.createSheet(session.name());
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("順番");
        headerRow.createCell(1).setCellValue("演目名");
        headerRow.createCell(2).setCellValue("時間");
        headerRow.createCell(3).setCellValue("出演者");

        for (int i = 0; i < session.entries().size(); i++) {
            Row row = sheet.createRow(i + 1);
            ExportEntry entry = session.entries().get(i);
            row.createCell(0).setCellValue(i + 1);
            row.createCell(1).setCellValue(entry.title());
            row.createCell(2).setCellValue(formatDuration(entry.durationSeconds()));
            row.createCell(3).setCellValue(String.join(", ", entry.performers()));
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

    private record ExportSession(String name, List<ExportEntry> entries) {
    }

    private record ExportEntry(String title, int durationSeconds, List<String> performers) {
    }
}
