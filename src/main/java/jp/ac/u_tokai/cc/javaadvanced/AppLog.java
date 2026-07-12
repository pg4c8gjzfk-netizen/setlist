package jp.ac.u_tokai.cc.javaadvanced;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/** ユーザーが障害内容を確認できる、依存ライブラリに左右されないアプリログです。 */
final class AppLog {

    private static final Object LOCK = new Object();
    private static final long MAX_LOG_BYTES = 2L * 1024L * 1024L;
    private static final int ARCHIVE_COUNT = 3;
    private static final String LOG_FILE_NAME = "setlist-studio.log";

    private static Path logFile;
    private static Thread.UncaughtExceptionHandler previousUncaughtExceptionHandler;
    private static boolean uncaughtExceptionHandlerInstalled;

    private AppLog() {
    }

    /** 通常の保存先配下へログを初期化し、未処理例外も記録します。 */
    static void initialize() {
        Path logDirectory = new AppFileLocations()
                .outputDirectory()
                .toPath()
                .resolve("logs");
        initialize(logDirectory, true);
        info("アプリケーションを起動しました。Java="
                + System.getProperty("java.version", "unknown")
                + ", OS=" + System.getProperty("os.name", "unknown"));
    }

    /** テスト用にログ保存先と未処理例外ハンドラーの有無を指定します。 */
    static void initialize(Path logDirectory, boolean installUncaughtExceptionHandler) {
        synchronized (LOCK) {
            try {
                Files.createDirectories(logDirectory);
                logFile = logDirectory.resolve(LOG_FILE_NAME).toAbsolutePath().normalize();
            } catch (Exception exception) {
                logFile = null;
                System.err.println("ログ保存先を準備できませんでした: " + exception.getMessage());
            }
            if (installUncaughtExceptionHandler && !uncaughtExceptionHandlerInstalled) {
                previousUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
                Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
                    error("未処理の例外が発生しました。thread=" + thread.getName(), throwable);
                    if (previousUncaughtExceptionHandler != null) {
                        previousUncaughtExceptionHandler.uncaughtException(thread, throwable);
                    }
                });
                uncaughtExceptionHandlerInstalled = true;
            }
        }
    }

    static void info(String message) {
        write("INFO", message, null);
    }

    static void warn(String message) {
        write("WARN", message, null);
    }

    static void error(String message, Throwable throwable) {
        write("ERROR", message, throwable);
    }

    static Path logFile() {
        synchronized (LOCK) {
            return logFile;
        }
    }

    static void resetForTests() {
        synchronized (LOCK) {
            logFile = null;
            if (uncaughtExceptionHandlerInstalled) {
                Thread.setDefaultUncaughtExceptionHandler(previousUncaughtExceptionHandler);
                previousUncaughtExceptionHandler = null;
                uncaughtExceptionHandlerInstalled = false;
            }
        }
    }

    private static void write(String level, String message, Throwable throwable) {
        synchronized (LOCK) {
            if (logFile == null) {
                return;
            }
            try {
                rotateIfNeeded();
                StringBuilder entry = new StringBuilder()
                        .append(OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                        .append(" [").append(level).append("] [")
                        .append(Thread.currentThread().getName()).append("] ")
                        .append(message == null ? "" : message)
                        .append(System.lineSeparator());
                if (throwable != null) {
                    StringWriter stackTrace = new StringWriter();
                    throwable.printStackTrace(new PrintWriter(stackTrace));
                    entry.append(stackTrace).append(System.lineSeparator());
                }
                Files.writeString(
                        logFile,
                        entry,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND);
            } catch (Exception exception) {
                System.err.println("ログを書き込めませんでした: " + exception.getMessage());
            }
        }
    }

    private static void rotateIfNeeded() throws Exception {
        if (!Files.isRegularFile(logFile) || Files.size(logFile) < MAX_LOG_BYTES) {
            return;
        }
        for (int archive = ARCHIVE_COUNT; archive >= 1; archive--) {
            Path source = archive == 1
                    ? logFile
                    : archivePath(archive - 1);
            if (Files.exists(source)) {
                Files.move(
                        source,
                        archivePath(archive),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static Path archivePath(int archiveNumber) {
        return logFile.resolveSibling(LOG_FILE_NAME + "." + archiveNumber);
    }
}
