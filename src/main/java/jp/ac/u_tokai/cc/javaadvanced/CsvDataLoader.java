package jp.ac.u_tokai.cc.javaadvanced;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CSVファイルから演目データを読み込むクラス。
 */
public class CsvDataLoader implements DataLoader {

    /**
     * CSVファイルからデータを読み込み、Mapに格納して返します。
     *
     * @param file 読み込む対象のCSVファイル
     * @return タイトルをキー、演目を値とするMap
     */
    @Override
    public Map<String, Performance> load(File file) {
        Map<String, Performance> loadedMap = new HashMap<>();

        try (BufferedReader br = Files.newBufferedReader(file.toPath())) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("公演,") || line.startsWith("title,")) {
                    continue;
                }
                addLine(loadedMap, line);
            }
        } catch (IOException e) {
            System.out.println("エラー：CSVファイルの読み込みに失敗しました。");
        }

        return loadedMap;
    }

    /**
     * CSVの1行を演目として追加します。
     */
    private void addLine(Map<String, Performance> loadedMap, String line) {
        String[] data = line.split(",", -1);
        if (data.length < 5) {
            return;
        }

        try {
            String title = data[0].trim();
            List<String> performers = new ArrayList<>();
            performers.add(data[1].trim());
            int duration = parseDuration(data[2].trim());
            int bpm = Integer.parseInt(data[3].trim());
            String mood = data[4].trim();
            Song song = new Song(title, performers, duration, bpm, mood);
            loadedMap.put(song.getTitle(), song);
        } catch (NumberFormatException e) {
            System.out.println("エラー：時間やBPMのデータを数値に変換できませんでした。行: " + line);
        }
    }

    /**
     * m:ssまたは秒数の文字列を秒数へ変換します。
     */
    private int parseDuration(String durationText) {
        if (durationText.contains(":")) {
            String[] parts = durationText.split(":");
            return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        }
        return Integer.parseInt(durationText);
    }
}