package jp.ac.u_tokai.cc.javaadvanced;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 生成されたセットリストをCSVファイルとして保存するクラス。
 */
public class CsvDataExporter implements DataExporter {

    /**
     * 生成されたセットリストをCSVファイルに書き出します。
     * Excelで文字化けしにくいようにBOM付きUTF-8で保存します。
     *
     * @param sessions 生成されたセットリスト
     * @param fileName 出力するファイル名（例: "output_setlist.csv"）
     */
    @Override
    public void export(List<List<Performance>> sessions, String fileName) {
        File outputDir = new File("Data");
        if (!outputDir.exists() && !outputDir.mkdir()) {
            System.out.println("[エラー] Dataフォルダを作成できませんでした。");
            return;
        }

        File outputFile = new File(outputDir, fileName);

        try (FileOutputStream fos = new FileOutputStream(outputFile);
                OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
                BufferedWriter bw = new BufferedWriter(osw)) {
            fos.write(0xef);
            fos.write(0xbb);
            fos.write(0xbf);

            bw.write("公演,順番,演目名,時間,出演者");
            bw.newLine();

            for (int i = 0; i < sessions.size(); i++) {
                writeSessionRows(bw, sessions.get(i), i + 1);
            }

            System.out.println("[成功] セットリストをCSV形式で保存しました: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            System.out.println("[エラー] CSVファイルの保存中にエラーが発生しました。");
            e.printStackTrace();
        }
    }

    /**
     * 1公演分のセットリストをCSV行として書き込みます。
     */
    private void writeSessionRows(BufferedWriter writer, List<Performance> session, int sessionNumber) throws IOException {
        String sessionName = "第" + sessionNumber + "公演";
        for (int i = 0; i < session.size(); i++) {
            Performance performance = session.get(i);
            writer.write(toCsvLine(
                    sessionName,
                    String.valueOf(i + 1),
                    performance.getDisplayTitle(),
                    formatDuration(performance.getDuration()),
                    String.join(" ", performance.getPerformers())));
            writer.newLine();
        }
    }

    /**
     * CSVの1行を作成します。
     */
    private String toCsvLine(String... values) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                line.append(',');
            }
            line.append(escapeCsv(values[i]));
        }
        return line.toString();
    }

    /**
     * カンマやダブルクォーテーションを含む値をCSV向けにエスケープします。
     */
    private String escapeCsv(String value) {
        String safeValue = value == null ? "" : value;
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
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