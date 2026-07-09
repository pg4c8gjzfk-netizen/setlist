package jp.ac.u_tokai.cc.javaadvanced;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.util.List;

/**
 * 生成されたセットリストをCSVファイルとして出力するクラス
 */
public class DataExporter_csv {

    /**
     * 生成されたセットリスト（二次元リスト）をCSVファイルに書き出します。
     * Excelで文字化けしないようにBOM付きUTF-8で保存します。
     * 
     * @param sessions 生成されたセットリスト
     * @param fileName 出力するファイル名（例: "output_setlist.csv"）
     */
    public void export(List<List<Performance>> sessions, String fileName) {
        // Dataフォルダの中に出力する
        File outputDir = new File("Data");
        if (!outputDir.exists()) {
            outputDir.mkdir();
        }

        File outputFile = new File(outputDir, fileName);

        // Excel文字化け対策のため、BOM（Byte Order Mark）を付けてUTF-8で書き込む
        try (FileOutputStream fos = new FileOutputStream(outputFile);
             OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
             BufferedWriter bw = new BufferedWriter(osw)) {

            // BOMの書き込み（Excelで開いた際の文字化けを防ぐ）
            fos.write(0xef);
            fos.write(0xbb);
            fos.write(0xbf);

            // ヘッダー行
            bw.write("公演,順番,演目名,時間,出演者");
            bw.newLine();

            // データの書き込み
            for (int i = 0; i < sessions.size(); i++) {
                String sessionName = "第" + (i + 1) + "公演";
                List<Performance> session = sessions.get(i);

                for (int j = 0; j < session.size(); j++) {
                    Performance p = session.get(j);
                    int order = j + 1;
                    
                    // ベースの曲名（"(_" などを除去した表示用名）
                    String displayTitle = p.getTitle().split("\\(_")[0];
                    
                    // 時間のフォーマット
                    int m = p.getDuration() / 60;
                    int s = p.getDuration() % 60;
                    String formattedTime = String.format("%d:%02d", m, s);
                    
                    // 出演者をスペース区切りの文字列に変換
                    String performersStr = String.join(" ", p.getPerformers());

                    // カンマが含まれる場合に備えてダブルクォーテーションで囲む
                    bw.write(String.format("\"%s\",\"%d\",\"%s\",\"%s\",\"%s\"", 
                            sessionName, order, displayTitle, formattedTime, performersStr));
                    bw.newLine();
                }
            }
            
            System.out.println("✅ [成功] セットリストをExcelで開けるCSV形式で保存しました: " + outputFile.getAbsolutePath());

        } catch (IOException e) {
            System.out.println("❌ [エラー] CSVファイルの保存中にエラーが発生しました。");
            e.printStackTrace();
        }
    }
}
