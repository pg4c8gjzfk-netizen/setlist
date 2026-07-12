package jp.ac.u_tokai.cc.javaadvanced;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.table.JTableHeader;

/** アプリ全体の外観・余白・色・コンポーネント階層を統一します。 */
public final class AppTheme {

    public enum Mode {
        LIGHT,
        DARK
    }

    private static final String THEME_PREFERENCE_KEY = "appearance";
    private static final String BUTTON_STYLE_PROPERTY = "setlist.buttonStyle";
    private static final String THEME_TOGGLE_PROPERTY = "setlist.themeToggle";
    private static final String TABBED_PANE_PROPERTY = "setlist.tabbedPane";

    private static volatile Mode currentMode = loadStoredMode();

    public static final Color PAGE_BACKGROUND = themeColor(0xF5F5F7, 0x111113);
    public static final Color CARD_BACKGROUND = themeColor(0xFFFFFF, 0x1C1C1E);
    public static final Color TEXT_PRIMARY = themeColor(0x1D1D1F, 0xF5F5F7);
    public static final Color TEXT_SECONDARY = themeColor(0x6E6E73, 0xA1A1A6);
    public static final Color ACCENT = themeColor(0x0071E3, 0x0A84FF);
    public static final Color ACCENT_HOVER = themeColor(0x0077ED, 0x409CFF);
    public static final Color ACCENT_PRESSED = themeColor(0x006EDB, 0x0077ED);
    public static final Color BORDER = themeColor(0xD9D9DE, 0x3A3A3C);
    public static final Color SURFACE_SUBTLE = themeColor(0xF8F8FA, 0x242426);
    public static final Color SELECTION = themeColor(0xE8F2FF, 0x173A5E);
    public static final Color DANGER = themeColor(0xD70015, 0xFF6961);
    public static final Color SUCCESS = themeColor(0x248A3D, 0x30D158);
    public static final Color WARNING = themeColor(0xB54708, 0xFF9F0A);

    private static final Color FOCUS = themeColor(0x80B7F0, 0x409CFF);
    private static final Color GRID = themeColor(0xECECF0, 0x38383A);

    private static final Font BASE_FONT = chooseBaseFont();
    private static boolean installed;

    private AppTheme() {
    }

    /** AWT初期化前に必要なmacOS統合プロパティを設定します。 */
    public static void configurePlatformProperties() {
        System.setProperty("apple.awt.application.name", "Setlist Studio");
        System.setProperty("apple.awt.application.appearance", "system");
        System.setProperty("apple.laf.useScreenMenuBar", "true");
    }

    /** macOS系テーマとアプリ固有のUI既定値を一度だけ適用します。 */
    public static synchronized void install() {
        if (installed) {
            return;
        }
        configurePlatformProperties();
        installLookAndFeel();
        configureUiDefaults();
        installed = true;
    }

    /** 現在選択されている配色を返します。 */
    public static Mode currentMode() {
        return currentMode;
    }

    /** 現在の配色がダークかを返します。 */
    public static boolean isDark() {
        return currentMode == Mode.DARK;
    }

    /** 配色を即時切替し、次回起動用に保存します。 */
    public static void setMode(Mode mode) {
        applyMode(mode, true);
    }

    static synchronized void applyMode(Mode mode, boolean persist) {
        Mode requestedMode = Objects.requireNonNull(mode, "mode must not be null");
        if (persist) {
            writeStoredMode(themePreferences(), requestedMode);
        }
        if (currentMode == requestedMode) {
            return;
        }
        currentMode = requestedMode;
        if (!installed) {
            return;
        }

        installLookAndFeel();
        configureUiDefaults();
        for (Window window : Window.getWindows()) {
            refreshThemeStyles(window);
        }
        FlatLaf.updateUI();
        for (Window window : Window.getWindows()) {
            window.repaint();
        }
        AppLog.info("表示テーマを" + (isDark() ? "ダーク" : "ライト") + "へ切り替えました。");
    }

    private static void installLookAndFeel() {
        boolean installedSuccessfully = isDark()
                ? FlatMacDarkLaf.setup()
                : FlatMacLightLaf.setup();
        if (!installedSuccessfully) {
            AppLog.warn("表示テーマを適用できませんでした: " + currentMode);
        }
    }

    private static void configureUiDefaults() {

        UIManager.put("defaultFont", BASE_FONT);
        UIManager.put("Component.arc", 14);
        UIManager.put("Button.arc", 16);
        UIManager.put("TextComponent.arc", 12);
        UIManager.put("ComboBox.arc", 12);
        UIManager.put("Spinner.arc", 12);
        UIManager.put("Component.focusWidth", 2);
        UIManager.put("Component.innerFocusWidth", 0);
        UIManager.put("Component.focusColor", FOCUS);
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("TabbedPane.tabHeight", 38);
        UIManager.put("TabbedPane.tabArc", 12);
        UIManager.put("TabbedPane.selectedBackground", CARD_BACKGROUND);
        UIManager.put("TabbedPane.hoverColor", GRID);
        UIManager.put("Table.rowHeight", 38);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.gridColor", GRID);
        UIManager.put("Table.selectionBackground", SELECTION);
        UIManager.put("Table.selectionForeground", TEXT_PRIMARY);
        UIManager.put("TableHeader.background", SURFACE_SUBTLE);
        UIManager.put("TableHeader.foreground", TEXT_SECONDARY);
        UIManager.put("TableHeader.height", 38);
        UIManager.put("OptionPane.messageFont", BASE_FONT.deriveFont(14f));
    }

    /** ページ背景用パネルを作成します。 */
    public static JPanel page(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(PAGE_BACKGROUND);
        return panel;
    }

    /** 角丸と薄い境界を持つカードを作成します。 */
    public static JPanel card(LayoutManager layout) {
        return new RoundedCardPanel(layout);
    }

    public static JLabel title(String text) {
        JLabel label = new JLabel(text);
        label.setFont(BASE_FONT.deriveFont(Font.BOLD, 30f));
        label.setForeground(TEXT_PRIMARY);
        return label;
    }

    public static JLabel heading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(BASE_FONT.deriveFont(Font.BOLD, 18f));
        label.setForeground(TEXT_PRIMARY);
        return label;
    }

    public static JLabel body(String text) {
        JLabel label = new JLabel(text);
        label.setFont(BASE_FONT.deriveFont(Font.PLAIN, 14f));
        label.setForeground(TEXT_SECONDARY);
        return label;
    }

    public static JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(BASE_FONT.deriveFont(Font.BOLD, 12f));
        label.setForeground(TEXT_SECONDARY);
        return label;
    }

    /** 状態を短く示す角丸ピルを作成します。 */
    public static JLabel statusPill(String text, Color accentColor) {
        return new RoundedBadgeLabel(text, accentColor);
    }

    /** 既存の状態ピルを、状態変化に合わせて更新します。 */
    public static void updateStatusPill(JLabel label, String text, Color accentColor) {
        label.setText(text);
        label.setForeground(accentColor);
        if (label instanceof RoundedBadgeLabel badgeLabel) {
            badgeLabel.setAccentColor(accentColor);
        }
        label.revalidate();
        label.repaint();
    }

    public static JButton primaryButton(String text) {
        JButton button = new JButton(text);
        stylePrimary(button);
        return button;
    }

    public static JButton secondaryButton(String text) {
        JButton button = new JButton(text);
        styleSecondary(button);
        return button;
    }

    public static JButton quietButton(String text) {
        JButton button = new JButton(text);
        styleQuiet(button);
        return button;
    }

    /** 現在と反対の配色へ切り替えるボタンを作成します。 */
    public static JButton themeToggleButton() {
        JButton button = quietButton("");
        button.putClientProperty(THEME_TOGGLE_PROPERTY, Boolean.TRUE);
        updateThemeToggleButton(button);
        button.addActionListener(event -> {
            setMode(isDark() ? Mode.LIGHT : Mode.DARK);
            updateThemeToggleButton(button);
        });
        return button;
    }

    public static void stylePrimary(JButton button) {
        button.putClientProperty(BUTTON_STYLE_PROPERTY, "primary");
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE,
                FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        button.putClientProperty(FlatClientProperties.STYLE,
                "arc: 16; minimumWidth: 96; borderWidth: 0; focusWidth: 2;"
                        + (isDark()
                                ? "background: #0A84FF; foreground: #FFFFFF;"
                                        + "hoverBackground: #409CFF; pressedBackground: #0077ED;"
                                : "background: #0071E3; foreground: #FFFFFF;"
                                        + "hoverBackground: #0077ED; pressedBackground: #006EDB;")
                        + "margin: 8,18,8,18");
    }

    public static void styleSecondary(JButton button) {
        button.putClientProperty(BUTTON_STYLE_PROPERTY, "secondary");
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE,
                FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        button.putClientProperty(FlatClientProperties.STYLE,
                "arc: 16; minimumWidth: 88; borderWidth: 1; focusWidth: 2;"
                        + (isDark()
                                ? "background: #2C2C2E; foreground: #F5F5F7; borderColor: #48484A;"
                                        + "hoverBackground: #3A3A3C; pressedBackground: #444446;"
                                : "background: #FFFFFF; foreground: #1D1D1F; borderColor: #D2D2D7;"
                                        + "hoverBackground: #F5F5F7; pressedBackground: #EDEDF0;")
                        + "margin: 8,15,8,15");
    }

    public static void styleQuiet(JButton button) {
        button.putClientProperty(BUTTON_STYLE_PROPERTY, "quiet");
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE,
                FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        button.putClientProperty(FlatClientProperties.STYLE,
                "arc: 14; minimumWidth: 72; borderWidth: 0; focusWidth: 2;"
                        + (isDark()
                                ? "background: #2C2C2E; foreground: #F5F5F7;"
                                        + "hoverBackground: #3A3A3C; pressedBackground: #48484A;"
                                : "background: #F2F2F5; foreground: #1D1D1F;"
                                        + "hoverBackground: #E8E8ED; pressedBackground: #DDDDE2;")
                        + "margin: 7,13,7,13");
    }

    public static void styleDanger(JButton button) {
        button.putClientProperty(BUTTON_STYLE_PROPERTY, "danger");
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE,
                FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        button.putClientProperty(FlatClientProperties.STYLE,
                "arc: 14; minimumWidth: 72; borderWidth: 0; focusWidth: 2;"
                        + (isDark()
                                ? "background: #3A2022; foreground: #FF6961;"
                                        + "hoverBackground: #4A2528; pressedBackground: #562C30;"
                                : "background: #FFF1F0; foreground: #D70015;"
                                        + "hoverBackground: #FFE5E3; pressedBackground: #FFD7D4;")
                        + "margin: 7,13,7,13");
    }

    /** 公演タブへ現在の配色に合う選択・ホバー色を設定します。 */
    public static void styleTabs(JTabbedPane tabs) {
        tabs.putClientProperty(TABBED_PANE_PROPERTY, Boolean.TRUE);
        tabs.putClientProperty(
                FlatClientProperties.STYLE,
                "tabArc: 12; tabHeight: 40;"
                        + (isDark()
                                ? "selectedBackground: #1C1C1E; hoverColor: #38383A"
                                : "selectedBackground: #FFFFFF; hoverColor: #ECECF0"));
    }

    /** 表とヘッダーを読みやすい密度へ整えます。 */
    public static void styleTable(JTable table) {
        table.setRowHeight(38);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(GRID);
        table.setIntercellSpacing(new java.awt.Dimension(0, 1));
        table.setSelectionBackground(SELECTION);
        table.setSelectionForeground(TEXT_PRIMARY);
        table.setFont(BASE_FONT.deriveFont(14f));
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setFont(BASE_FONT.deriveFont(Font.BOLD, 12f));
        header.setForeground(TEXT_SECONDARY);
        header.setBackground(SURFACE_SUBTLE);
        header.setReorderingAllowed(false);
    }

    /** 境界のないスクロール領域を作成します。 */
    public static JScrollPane scroll(Component component) {
        JScrollPane scrollPane = new JScrollPane(component);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(CARD_BACKGROUND);
        return scrollPane;
    }

    public static void stylePreview(JTextArea textArea) {
        textArea.setFont(BASE_FONT.deriveFont(14f));
        textArea.setForeground(TEXT_PRIMARY);
        textArea.setBackground(SURFACE_SUBTLE);
        textArea.setMargin(new Insets(18, 20, 18, 20));
        textArea.setCaretColor(ACCENT);
    }

    public static EmptyBorder pagePadding() {
        return new EmptyBorder(26, 30, 26, 30);
    }

    /** macOSではCommand、その他ではCtrlとなる標準ショートカットを登録します。 */
    public static void bindMenuShortcut(
            JRootPane rootPane, String actionName, int keyCode, Runnable action) {
        int menuMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        KeyStroke keyStroke = KeyStroke.getKeyStroke(keyCode, menuMask);
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, actionName);
        rootPane.getActionMap().put(actionName, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                action.run();
            }
        });
    }

    static Mode readStoredMode(Preferences preferences) {
        String storedValue = preferences.get(THEME_PREFERENCE_KEY, Mode.LIGHT.name());
        try {
            return Mode.valueOf(storedValue);
        } catch (IllegalArgumentException exception) {
            return Mode.LIGHT;
        }
    }

    static void writeStoredMode(Preferences preferences, Mode mode) {
        try {
            preferences.put(THEME_PREFERENCE_KEY, mode.name());
            preferences.flush();
        } catch (BackingStoreException | RuntimeException exception) {
            AppLog.warn("表示テーマの設定を保存できませんでした: " + exception.getMessage());
        }
    }

    private static Mode loadStoredMode() {
        try {
            return readStoredMode(themePreferences());
        } catch (RuntimeException exception) {
            return Mode.LIGHT;
        }
    }

    private static Preferences themePreferences() {
        return Preferences.userNodeForPackage(AppTheme.class).node("appearance");
    }

    private static void updateThemeToggleButton(JButton button) {
        boolean dark = isDark();
        button.setText(dark ? "ライト表示" : "ダーク表示");
        button.setToolTipText(dark
                ? "画面をライトテーマへ切り替えます"
                : "画面をダークテーマへ切り替えます");
        button.getAccessibleContext().setAccessibleName(button.getText());
        button.getAccessibleContext().setAccessibleDescription(button.getToolTipText());
    }

    private static void refreshThemeStyles(Component component) {
        if (component instanceof JButton button) {
            Object style = button.getClientProperty(BUTTON_STYLE_PROPERTY);
            if ("primary".equals(style)) {
                stylePrimary(button);
            } else if ("secondary".equals(style)) {
                styleSecondary(button);
            } else if ("danger".equals(style)) {
                styleDanger(button);
            } else if ("quiet".equals(style)) {
                styleQuiet(button);
            }
            if (Boolean.TRUE.equals(button.getClientProperty(THEME_TOGGLE_PROPERTY))) {
                updateThemeToggleButton(button);
            }
        }
        if (component instanceof JTabbedPane tabs
                && Boolean.TRUE.equals(tabs.getClientProperty(TABBED_PANE_PROPERTY))) {
            styleTabs(tabs);
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                refreshThemeStyles(child);
            }
        }
    }

    private static Color themeColor(int lightRgb, int darkRgb) {
        return new ThemeColor(lightRgb, darkRgb);
    }

    private static Font chooseBaseFont() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        List<String> candidates = osName.contains("mac")
                ? List.of("Hiragino Sans", ".AppleSystemUIFont", "Noto Sans CJK JP", Font.SANS_SERIF)
                : List.of("Yu Gothic UI", "Yu Gothic", "Meiryo UI", "Meiryo", "Noto Sans CJK JP", Font.SANS_SERIF);
        Set<String> availableFamilies = Arrays.stream(
                        GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames())
                .map(name -> name.toLowerCase(java.util.Locale.ROOT))
                .collect(Collectors.toSet());
        for (String candidate : candidates) {
            if (availableFamilies.contains(candidate.toLowerCase(java.util.Locale.ROOT))) {
                return new Font(candidate, Font.PLAIN, 14);
            }
        }
        return new Font(Font.DIALOG, Font.PLAIN, 14);
    }

    /** 同じColor参照のまま、現在の配色に応じたRGB値を返します。 */
    private static final class ThemeColor extends Color {

        private static final long serialVersionUID = 1L;

        private final int lightRgb;
        private final int darkRgb;

        private ThemeColor(int lightRgb, int darkRgb) {
            super(lightRgb);
            this.lightRgb = lightRgb;
            this.darkRgb = darkRgb;
        }

        @Override
        public int getRGB() {
            int rgb = isDark() ? darkRgb : lightRgb;
            return 0xFF000000 | rgb;
        }
    }

    /** 余白を内包し、アンチエイリアス付きで描画するカードです。 */
    private static final class RoundedCardPanel extends JPanel {

        private static final int ARC = 24;

        private RoundedCardPanel(LayoutManager layout) {
            super(layout);
            setOpaque(false);
            setBorder(new EmptyBorder(20, 22, 20, 22));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D graphics2D = (Graphics2D) graphics.create();
            try {
                graphics2D.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics2D.setColor(CARD_BACKGROUND);
                graphics2D.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC, ARC);
                graphics2D.setColor(BORDER);
                graphics2D.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC, ARC);
            } finally {
                graphics2D.dispose();
            }
            super.paintComponent(graphics);
        }
    }

    private static final class RoundedBadgeLabel extends JLabel {

        private Color accentColor;

        private RoundedBadgeLabel(String text, Color accentColor) {
            super(text);
            this.accentColor = accentColor;
            setFont(BASE_FONT.deriveFont(Font.BOLD, 12f));
            setForeground(accentColor);
            setOpaque(false);
            setBorder(new EmptyBorder(8, 25, 8, 13));
        }

        private void setAccentColor(Color accentColor) {
            this.accentColor = accentColor;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D graphics2D = (Graphics2D) graphics.create();
            try {
                graphics2D.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics2D.setColor(CARD_BACKGROUND);
                graphics2D.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                        getHeight(), getHeight());
                graphics2D.setColor(BORDER);
                graphics2D.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                        getHeight(), getHeight());
                graphics2D.setColor(accentColor);
                graphics2D.fillOval(12, (getHeight() - 6) / 2, 6, 6);
            } finally {
                graphics2D.dispose();
            }
            super.paintComponent(graphics);
        }
    }
}
