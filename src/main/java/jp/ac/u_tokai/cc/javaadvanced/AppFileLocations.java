package jp.ac.u_tokai.cc.javaadvanced;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;

/** 入出力ファイル選択で使用する、作業ディレクトリに依存しない場所を管理します。 */
final class AppFileLocations {

    static final String OUTPUT_DIRECTORY_NAME = "Setlist Studio";

    private final File defaultDirectory;
    private File inputDirectory;
    private File outputDirectory;

    /** ユーザーのDocumentsフォルダを初期位置として使用します。 */
    AppFileLocations() {
        this(resolveDocumentsDirectory());
    }

    /** テストなどで初期位置を指定します。 */
    AppFileLocations(File defaultDirectory) {
        this.defaultDirectory = normalizeDirectory(defaultDirectory);
        this.inputDirectory = this.defaultDirectory;
        this.outputDirectory = new File(this.defaultDirectory, OUTPUT_DIRECTORY_NAME).getAbsoluteFile();
    }

    File inputDirectory() {
        return existingDirectoryOrDefault(inputDirectory);
    }

    File outputDirectory() {
        try {
            Files.createDirectories(outputDirectory.toPath());
            return outputDirectory;
        } catch (IOException | SecurityException exception) {
            return defaultDirectory;
        }
    }

    File defaultOutputFile(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("fileName must not be blank");
        }
        return new File(outputDirectory(), fileName);
    }

    void rememberInputFile(File inputFile) {
        inputDirectory = parentDirectory(inputFile, inputDirectory);
    }

    void rememberOutputFile(File outputFile) {
        outputDirectory = parentDirectory(outputFile, outputDirectory);
    }

    private File parentDirectory(File file, File fallback) {
        Objects.requireNonNull(file, "file must not be null");
        File parent = file.getAbsoluteFile().getParentFile();
        return parent != null && parent.isDirectory() ? parent : fallback;
    }

    private File existingDirectoryOrDefault(File directory) {
        return directory != null && directory.isDirectory() ? directory : defaultDirectory;
    }

    private static File resolveDocumentsDirectory() {
        File userHome = new File(System.getProperty("user.home", ".")).getAbsoluteFile();
        File documents = new File(userHome, "Documents");
        return documents.isDirectory() ? documents : userHome;
    }

    private static File normalizeDirectory(File directory) {
        Objects.requireNonNull(directory, "defaultDirectory must not be null");
        File absoluteDirectory = directory.getAbsoluteFile();
        if (!absoluteDirectory.isDirectory()) {
            throw new IllegalArgumentException("defaultDirectory must be an existing directory");
        }
        return absoluteDirectory;
    }
}
