package jp.ac.u_tokai.cc.javaadvanced;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** 新規香盤表の作成前に入力する必須設定です。 */
record NewSetlistSettings(String fileName, int performanceCount) {

    static final int MIN_PERFORMANCE_COUNT = 1;
    static final int MAX_PERFORMANCE_COUNT = 30;
    static final String DEFAULT_FILE_NAME = "新しい香盤表.xlsx";

    private static final Pattern INVALID_FILE_NAME = Pattern.compile("[<>:\"/\\\\|?*\\p{Cntrl}]");
    private static final Set<String> RESERVED_WINDOWS_NAMES = Set.of(
            "CON", "PRN", "AUX", "NUL",
            "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
            "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9");

    NewSetlistSettings {
        fileName = normalizeFileName(fileName);
        if (performanceCount < MIN_PERFORMANCE_COUNT || performanceCount > MAX_PERFORMANCE_COUNT) {
            throw new IllegalArgumentException(
                    "公演数は" + MIN_PERFORMANCE_COUNT + "〜" + MAX_PERFORMANCE_COUNT + "の範囲で入力してください。");
        }
    }

    static String normalizeFileName(String value) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("ファイル名を入力してください。");
        }
        if (INVALID_FILE_NAME.matcher(normalized).find()) {
            throw new IllegalArgumentException("ファイル名に使用できない文字が含まれています。");
        }
        if (!normalized.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            normalized += ".xlsx";
        }
        String baseName = normalized.substring(0, normalized.length() - ".xlsx".length()).strip();
        if (baseName.isEmpty() || baseName.endsWith(".")) {
            throw new IllegalArgumentException("有効なファイル名を入力してください。");
        }
        String deviceName = baseName.split("\\.", 2)[0].strip();
        if (RESERVED_WINDOWS_NAMES.contains(deviceName.toUpperCase(Locale.ROOT))) {
            throw new IllegalArgumentException("そのファイル名はWindowsで使用できません。");
        }
        return normalized;
    }

    static String distributionFileName(String editableFileName) {
        String normalized = normalizeFileName(editableFileName);
        return normalized.substring(0, normalized.length() - ".xlsx".length()) + "_配布用.xlsx";
    }
}
