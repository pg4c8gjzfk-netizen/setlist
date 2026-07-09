package jp.ac.u_tokai.cc.javaadvanced;

import java.io.File;
import java.util.Locale;
import java.util.Objects;

/**
 * 入力ファイルの拡張子に対応するデータローダーを生成します。
 */
public final class PerformanceReaderFactory {

    private PerformanceReaderFactory() {
        // ファクトリークラスのためインスタンス化しません。
    }

    /**
     * ファイル拡張子に応じたデータローダーを作成します。
     *
     * @param file 読み込み対象のファイル
     * @return 対応するデータローダー
     * @throws IllegalArgumentException 対応していない拡張子の場合
     */
    public static PerformanceDataReader create(File file) {
        Objects.requireNonNull(file, "file must not be null");

        String fileName = file.getName().toLowerCase(Locale.ROOT);
        if (fileName.endsWith(".xlsx")) {
            return new XlsxPerformanceReader();
        }
        if (fileName.endsWith(".csv")) {
            return new CsvPerformanceReader();
        }

        throw new IllegalArgumentException(
                "対応していないデータファイル形式です: " + file.getName());
    }
}
