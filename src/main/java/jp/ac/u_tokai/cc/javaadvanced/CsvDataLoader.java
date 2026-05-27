package jp.ac.u_tokai.cc.javaadvanced;

// .csvファイルを取り扱うためのライブラリ
import java.io.BufferedReader;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class CsvDataLoader implements Dataloader {

    @Override
    public Map<String, Performance> load(File file) {
        // 読み込んだデータを格納するための空のMapを用意
        Map<String, Performance> loadedMap = new HashMap<>();

        try (BufferedReader br = Files.newBufferedReader(file.toPath())) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");
                if (data.length >= 5) {
                    String title = data[0];
                    String performer = data[1];
                    int duration = Integer.parseInt(data[2]);
                    int bpm = Integer.parseInt(data[3]);
                    String mood = data[4];

                    Song song = new Song(title, performer, duration, bpm, mood);
                    loadedMap.put(song.getTitle(), song);
                }
            }
        } catch (IOException e) {
            System.out.println("エラー：CSVファイルの読み込みに失敗しました。");
        } catch (NumberFormatException e) {
            System.out.println("エラー：時間やBPMのデータを数値に変換できませんでした。");
        }

        return loadedMap;

    }

}