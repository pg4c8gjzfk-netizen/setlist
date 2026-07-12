package jp.ac.u_tokai.cc.javaadvanced;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import java.util.UUID;
import java.util.prefs.Preferences;
import javax.swing.JButton;
import javax.swing.UIManager;
import org.junit.BeforeClass;
import org.junit.Test;

/** ライト／ダークテーマの切替と保存を検証します。 */
public class AppThemeTest {

    @BeforeClass
    public static void installApplicationTheme() {
        AppTheme.install();
    }

    @Test
    public void themeButtonSwitchesLookAndFeelAndDynamicColors() {
        AppTheme.Mode originalMode = AppTheme.currentMode();
        try {
            AppTheme.applyMode(AppTheme.Mode.LIGHT, false);
            JButton toggleButton = AppTheme.themeToggleButton();
            int lightPageColor = AppTheme.PAGE_BACKGROUND.getRGB();

            assertEquals("ダーク表示", toggleButton.getText());
            assertTrue(UIManager.getLookAndFeel() instanceof FlatMacLightLaf);

            toggleButton.doClick();

            assertEquals(AppTheme.Mode.DARK, AppTheme.currentMode());
            assertEquals("ライト表示", toggleButton.getText());
            assertTrue(UIManager.getLookAndFeel() instanceof FlatMacDarkLaf);
            assertNotEquals(lightPageColor, AppTheme.PAGE_BACKGROUND.getRGB());
            assertEquals(0x111113, AppTheme.PAGE_BACKGROUND.getRGB() & 0xFFFFFF);
            assertEquals(0xF5F5F7, AppTheme.TEXT_PRIMARY.getRGB() & 0xFFFFFF);
        } finally {
            AppTheme.setMode(originalMode);
        }
    }

    @Test
    public void storedThemeRoundTripsAndInvalidValuesFallBackToLight() throws Exception {
        Preferences preferences = Preferences.userRoot().node(
                "/jp/ac/u_tokai/cc/javaadvanced/tests/" + UUID.randomUUID());
        try {
            AppTheme.writeStoredMode(preferences, AppTheme.Mode.DARK);
            assertEquals(AppTheme.Mode.DARK, AppTheme.readStoredMode(preferences));

            preferences.put("appearance", "UNKNOWN");
            assertEquals(AppTheme.Mode.LIGHT, AppTheme.readStoredMode(preferences));
        } finally {
            preferences.removeNode();
            preferences.flush();
        }
    }
}
