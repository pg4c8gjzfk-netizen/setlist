package jp.ac.u_tokai.cc.javaadvanced;

import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** 障害調査用ログが実際にUTF-8ファイルへ残ることを確認します。 */
public class AppLogTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void writesMessagesAndExceptionDetailsToConfiguredDirectory() throws Exception {
        Path logDirectory = temporaryFolder.newFolder("logs").toPath();
        try {
            AppLog.initialize(logDirectory, false);

            AppLog.info("起動確認");
            AppLog.error("出力確認", new IllegalStateException("原因確認"));

            Path logFile = AppLog.logFile();
            assertTrue(Files.isRegularFile(logFile));
            String content = Files.readString(logFile, StandardCharsets.UTF_8);
            assertTrue(content.contains("[INFO]"));
            assertTrue(content.contains("起動確認"));
            assertTrue(content.contains("[ERROR]"));
            assertTrue(content.contains("IllegalStateException"));
            assertTrue(content.contains("原因確認"));
        } finally {
            AppLog.resetForTests();
        }
    }
}
