package jp.ac.u_tokai.cc.javaadvanced;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
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
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.table.JTableHeader;

/** アプリ全体の外観・余白・色・コンポーネント階層を統一します。 */
public final class AppTheme {

    public static final Color PAGE_BACKGROUND = new Color(0xF5F5F7);
    public static final Color CARD_BACKGROUND = Color.WHITE;
    public static final Color TEXT_PRIMARY = new Color(0x1D1D1F);
    public static final Color TEXT_SECONDARY = new Color(0x6E6E73);
    public static final Color ACCENT = new Color(0x0071E3);
    public static final Color ACCENT_HOVER = new Color(0x0077ED);
    public static final Color ACCENT_PRESSED = new Color(0x006EDB);
    public static final Color BORDER = new Color(0xD9D9DE);
    public static final Color SURFACE_SUBTLE = new Color(0xF8F8FA);
    public static final Color SELECTION = new Color(0xE8F2FF);
    public static final Color DANGER = new Color(0xD70015);

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
        FlatMacLightLaf.setup();

        UIManager.put("defaultFont", BASE_FONT);
        UIManager.put("Component.arc", 14);
        UIManager.put("Button.arc", 16);
        UIManager.put("TextComponent.arc", 12);
        UIManager.put("ComboBox.arc", 12);
        UIManager.put("Spinner.arc", 12);
        UIManager.put("Component.focusWidth", 2);
        UIManager.put("Component.innerFocusWidth", 0);
        UIManager.put("Component.focusColor", new Color(0x80B7F0));
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("TabbedPane.tabHeight", 38);
        UIManager.put("TabbedPane.tabArc", 12);
        UIManager.put("TabbedPane.selectedBackground", CARD_BACKGROUND);
        UIManager.put("TabbedPane.hoverColor", new Color(0xECECF0));
        UIManager.put("Table.rowHeight", 38);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.gridColor", new Color(0xECECF0));
        UIManager.put("Table.selectionBackground", SELECTION);
        UIManager.put("Table.selectionForeground", TEXT_PRIMARY);
        UIManager.put("TableHeader.background", SURFACE_SUBTLE);
        UIManager.put("TableHeader.foreground", TEXT_SECONDARY);
        UIManager.put("TableHeader.height", 38);
        UIManager.put("OptionPane.messageFont", BASE_FONT.deriveFont(14f));
        installed = true;
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

    public static void stylePrimary(JButton button) {
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE,
                FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        button.putClientProperty(FlatClientProperties.STYLE,
                "arc: 16; minimumWidth: 96; borderWidth: 0; focusWidth: 2;"
                        + "background: #0071E3; foreground: #FFFFFF;"
                        + "hoverBackground: #0077ED; pressedBackground: #006EDB;"
                        + "margin: 8,18,8,18");
    }

    public static void styleSecondary(JButton button) {
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE,
                FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        button.putClientProperty(FlatClientProperties.STYLE,
                "arc: 16; minimumWidth: 88; borderWidth: 1; focusWidth: 2;"
                        + "background: #FFFFFF; foreground: #1D1D1F; borderColor: #D2D2D7;"
                        + "hoverBackground: #F5F5F7; pressedBackground: #EDEDF0;"
                        + "margin: 8,15,8,15");
    }

    public static void styleQuiet(JButton button) {
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE,
                FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        button.putClientProperty(FlatClientProperties.STYLE,
                "arc: 14; minimumWidth: 72; borderWidth: 0; focusWidth: 2;"
                        + "background: #F2F2F5; foreground: #1D1D1F;"
                        + "hoverBackground: #E8E8ED; pressedBackground: #DDDDE2;"
                        + "margin: 7,13,7,13");
    }

    public static void styleDanger(JButton button) {
        styleQuiet(button);
        button.putClientProperty(FlatClientProperties.STYLE,
                "arc: 14; minimumWidth: 72; borderWidth: 0; focusWidth: 2;"
                        + "background: #FFF1F0; foreground: #D70015;"
                        + "hoverBackground: #FFE5E3; pressedBackground: #FFD7D4;"
                        + "margin: 7,13,7,13");
    }

    /** 表とヘッダーを読みやすい密度へ整えます。 */
    public static void styleTable(JTable table) {
        table.setRowHeight(38);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(new Color(0xECECF0));
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

        private final Color accentColor;

        private RoundedBadgeLabel(String text, Color accentColor) {
            super(text);
            this.accentColor = accentColor;
            setFont(BASE_FONT.deriveFont(Font.BOLD, 12f));
            setForeground(accentColor);
            setOpaque(false);
            setBorder(new EmptyBorder(8, 25, 8, 13));
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
