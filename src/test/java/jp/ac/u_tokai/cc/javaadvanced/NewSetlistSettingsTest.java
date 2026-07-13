package jp.ac.u_tokai.cc.javaadvanced;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

/** 新規作成時のファイル名と公演数の検証テストです。 */
public class NewSetlistSettingsTest {

    @Test
    public void normalizesFileNameAndBuildsDistributionName() {
        NewSetlistSettings settings = new NewSetlistSettings("  春公演  ", 3);

        assertEquals("春公演.xlsx", settings.fileName());
        assertEquals(3, settings.performanceCount());
        assertEquals("春公演_配布用.xlsx", NewSetlistSettings.distributionFileName(settings.fileName()));
    }

    @Test
    public void rejectsBlankInvalidAndReservedFileNames() {
        assertInvalidFileName("", "ファイル名");
        assertInvalidFileName("春/公演.xlsx", "使用できない文字");
        assertInvalidFileName("CON.xlsx", "Windows");
        assertInvalidFileName("COM1.backup.xlsx", "Windows");
    }

    @Test
    public void rejectsPerformanceCountOutsideSupportedRange() {
        try {
            new NewSetlistSettings("春公演.xlsx", 0);
            fail("0公演で作成できてしまいました。");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("公演数"));
        }
        try {
            new NewSetlistSettings("春公演.xlsx", NewSetlistSettings.MAX_PERFORMANCE_COUNT + 1);
            fail("上限を超える公演数で作成できてしまいました。");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("公演数"));
        }
    }

    private void assertInvalidFileName(String fileName, String expectedMessage) {
        try {
            new NewSetlistSettings(fileName, 1);
            fail("不正なファイル名で作成できてしまいました: " + fileName);
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains(expectedMessage));
        }
    }
}
