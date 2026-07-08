package com.heapdump.analyzer.ui.swing;

import cn.wanghw.Severity;

import javax.swing.*;
import java.awt.*;

public class ThemeConfig {

    // === 字体体系 ===
    public static final Font FONT_HEADING;
    public static final Font FONT_SUBTITLE;
    public static final Font FONT_BODY;
    public static final Font FONT_CAPTION;
    public static final Font FONT_MICRO;
    public static final Font FONT_MONO;

    // 当前字体缩放因子（默认为1.0，可通过Ctrl+/Ctrl-调整）
    private static float fontScaleFactor = 1.0f;

    static {
        Font baseFont = UIManager.getFont("Label.font");
        if (baseFont == null) baseFont = new Font(Font.SANS_SERIF, Font.PLAIN, 13);  // 调小基础字体

        // 字体大小调整
        FONT_HEADING  = baseFont.deriveFont(Font.BOLD, 15f);
        FONT_SUBTITLE = baseFont.deriveFont(Font.BOLD, 13f);
        FONT_BODY     = baseFont.deriveFont(Font.PLAIN, 13f);
        FONT_CAPTION  = baseFont.deriveFont(Font.PLAIN, 12f);
        FONT_MICRO    = baseFont.deriveFont(Font.PLAIN, 10f);

        Font monoFont;
        try {
            monoFont = new Font("JetBrains Mono", Font.PLAIN, 12);  // 调小等宽字体
            if (!monoFont.getFamily().equals("JetBrains Mono")) {
                monoFont = new Font("Monospaced", Font.PLAIN, 12);
            }
        } catch (Exception e) {
            monoFont = new Font("Monospaced", Font.PLAIN, 12);
        }
        FONT_MONO = monoFont;
    }

    /**
     * 获取当前字体缩放因子
     */
    public static float getFontScaleFactor() {
        return fontScaleFactor;
    }

    /**
     * 设置字体缩放因子并更新所有字体
     */
    public static void setFontScaleFactor(float factor) {
        fontScaleFactor = Math.max(0.5f, Math.min(2.0f, factor));  // 限制在0.5-2.0范围
    }

    /**
     * 缩放字体
     */
    public static Font scaleFont(Font baseFont) {
        return baseFont.deriveFont(baseFont.getSize() * fontScaleFactor);
    }

    // === 严重度颜色 (浅色主题，WCAG AA 友好) ===
    public static final Color SEVERITY_CRITICAL = new Color(179, 38, 30);   // #B3261E
    public static final Color SEVERITY_HIGH     = new Color(220, 107, 0);   // #DC6B00
    public static final Color SEVERITY_MEDIUM   = new Color(199, 153, 0);   // #C79900
    public static final Color SEVERITY_LOW      = new Color(0, 110, 28);    // #006E1C
    public static final Color SEVERITY_INFO     = new Color(0, 99, 155);    // #00639B

    // === 强调色 ===
    public static final Color ACCENT = new Color(11, 94, 162); // #0B5EA2

    // === 间距常量 ===
    public static final int GAP_TINY   = 4;
    public static final int GAP_SMALL  = 8;
    public static final int GAP_MEDIUM = 12;
    public static final int GAP_LARGE  = 16;

    public static Color getSeverityColor(Severity severity) {
        switch (severity) {
            case CRITICAL: return SEVERITY_CRITICAL;
            case HIGH:     return SEVERITY_HIGH;
            case MEDIUM:   return SEVERITY_MEDIUM;
            case LOW:      return SEVERITY_LOW;
            case INFO:     return SEVERITY_INFO;
            default:       return Color.GRAY;
        }
    }

    public static Color getBackground() {
        Color c = UIManager.getColor("Panel.background");
        return c != null ? c : new Color(30, 30, 46);
    }

    public static Color getCardBackground() {
        Color bg = getBackground();
        boolean dark = isDarkTheme(bg);
        return dark ? brighten(bg, 12) : brighten(bg, -6);
    }

    public static Color getBorderColor() {
        Color c = UIManager.getColor("Component.borderColor");
        return c != null ? c : new Color(69, 71, 90);
    }

    public static Color getTextColor() {
        Color c = UIManager.getColor("Label.foreground");
        return c != null ? c : new Color(205, 214, 244);
    }

    public static Color getMutedTextColor() {
        Color c = getTextColor();
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), 150);
    }

    public static Color getAlternateRowColor() {
        Color bg = getBackground();
        boolean dark = isDarkTheme(bg);
        return dark ? brighten(bg, 6) : brighten(bg, -6);
    }

    public static Color blend(Color base, Color overlay, float ratio) {
        int r = (int) (base.getRed()   + (overlay.getRed()   - base.getRed())   * ratio);
        int g = (int) (base.getGreen() + (overlay.getGreen() - base.getGreen()) * ratio);
        int b = (int) (base.getBlue()  + (overlay.getBlue()  - base.getBlue())  * ratio);
        return new Color(clamp(r), clamp(g), clamp(b));
    }

    public static void installUIDefaults() {
        UIManager.put("Component.arc", 8);
        UIManager.put("Component.arrowType", "chevron");
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("Button.arc", 8);
        UIManager.put("Table.arc", 4);
        UIManager.put("Table.rowHeight", 24);
        UIManager.put("Table.alternateRowColor", getAlternateRowColor());
        UIManager.put("SplitPane.dividerSize", 6);
        UIManager.put("ScrollPane.showButtons", true);
        UIManager.put("ScrollBar.showButtons", false);
    }

    private static boolean isDarkTheme(Color bg) {
        return (bg.getRed() * 0.299 + bg.getGreen() * 0.587 + bg.getBlue() * 0.114) < 128;
    }

    private static Color brighten(Color c, int amount) {
        return new Color(clamp(c.getRed() + amount), clamp(c.getGreen() + amount), clamp(c.getBlue() + amount));
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
