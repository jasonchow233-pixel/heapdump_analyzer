package com.heapdump.analyzer.ui.swing;

import javax.swing.*;
import javax.swing.text.*;

public class HighlightTextPane extends JTextPane {

    private final SimpleAttributeSet keyStyle;
    private final SimpleAttributeSet valueStyle;
    private final SimpleAttributeSet normalStyle;
    private final SimpleAttributeSet separatorStyle;

    public HighlightTextPane() {
        super();
        setEditable(false);
        setBackground(ThemeConfig.getCardBackground());

        keyStyle = new SimpleAttributeSet();
        StyleConstants.setFontFamily(keyStyle, "Monospaced");
        StyleConstants.setFontSize(keyStyle, 12);
        StyleConstants.setBold(keyStyle, true);
        StyleConstants.setForeground(keyStyle, ThemeConfig.ACCENT);

        valueStyle = new SimpleAttributeSet();
        StyleConstants.setFontFamily(valueStyle, "Monospaced");
        StyleConstants.setFontSize(valueStyle, 12);
        StyleConstants.setForeground(valueStyle, ThemeConfig.SEVERITY_LOW);

        separatorStyle = new SimpleAttributeSet();
        StyleConstants.setFontFamily(separatorStyle, "Monospaced");
        StyleConstants.setFontSize(separatorStyle, 12);
        StyleConstants.setForeground(separatorStyle, ThemeConfig.getMutedTextColor());

        normalStyle = new SimpleAttributeSet();
        StyleConstants.setFontFamily(normalStyle, "Monospaced");
        StyleConstants.setFontSize(normalStyle, 12);
        StyleConstants.setForeground(normalStyle, ThemeConfig.getTextColor());

        setEditorKit(new WrapEditorKit());
    }

    public void setHighlightedText(String text) {
        StyledDocument doc = getStyledDocument();
        try {
            doc.remove(0, doc.getLength());
            if (text == null || text.isEmpty()) return;

            String[] lines = text.split("\n");
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];

                if (line.contains(":") || line.contains("=")) {
                    int sepIndex = findSeparator(line);
                    if (sepIndex > 0) {
                        String key = line.substring(0, sepIndex);
                        String sep = String.valueOf(line.charAt(sepIndex));
                        String value = line.substring(sepIndex + 1);

                        doc.insertString(doc.getLength(), key, keyStyle);
                        doc.insertString(doc.getLength(), sep, separatorStyle);
                        doc.insertString(doc.getLength(), value, valueStyle);
                    } else {
                        doc.insertString(doc.getLength(), line, normalStyle);
                    }
                } else {
                    doc.insertString(doc.getLength(), line, normalStyle);
                }

                if (i < lines.length - 1) {
                    doc.insertString(doc.getLength(), "\n", normalStyle);
                }
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private int findSeparator(String line) {
        int colonIdx = line.indexOf(":");
        int equalIdx = line.indexOf("=");

        if (colonIdx < 0) return equalIdx;
        if (equalIdx < 0) return colonIdx;
        return Math.min(colonIdx, equalIdx);
    }

    public String getPlainText() {
        try {
            return getStyledDocument().getText(0, getStyledDocument().getLength());
        } catch (BadLocationException e) {
            return "";
        }
    }

    static class WrapEditorKit extends StyledEditorKit {
        private final ViewFactory defaultFactory = new WrapViewFactory();

        @Override
        public ViewFactory getViewFactory() {
            return defaultFactory;
        }

        @Override
        public Object clone() {
            return new WrapEditorKit();
        }
    }

    static class WrapViewFactory implements ViewFactory {
        @Override
        public View create(Element elem) {
            String kind = elem.getName();
            if (kind != null) {
                if (kind.equals(AbstractDocument.ContentElementName)) {
                    return new WrapView(elem);
                } else if (kind.equals(AbstractDocument.ParagraphElementName)) {
                    return new ParagraphView(elem);
                } else if (kind.equals(AbstractDocument.SectionElementName)) {
                    return new BoxView(elem, View.Y_AXIS);
                } else if (kind.equals(StyleConstants.ComponentElementName)) {
                    return new ComponentView(elem);
                } else if (kind.equals(StyleConstants.IconElementName)) {
                    return new IconView(elem);
                }
            }
            return new LabelView(elem);
        }
    }

    static class WrapView extends LabelView {
        public WrapView(Element elem) {
            super(elem);
        }

        @Override
        public float getMinimumSpan(int axis) {
            if (axis == View.X_AXIS) return 0;
            return super.getMinimumSpan(axis);
        }

        @Override
        public float getPreferredSpan(int axis) {
            if (axis == View.X_AXIS) return 0;
            return super.getPreferredSpan(axis);
        }

        @Override
        public float getMaximumSpan(int axis) {
            if (axis == View.X_AXIS) return Float.MAX_VALUE;
            return super.getMaximumSpan(axis);
        }

        @Override
        public int getBreakWeight(int axis, float pos, float len) {
            if (axis == View.X_AXIS) return View.ExcellentBreakWeight;
            return super.getBreakWeight(axis, pos, len);
        }

        @Override
        public View breakView(int axis, int p0, float pos, float len) {
            if (axis == View.X_AXIS) {
                checkPainter();
                int p1 = getGlyphPainter().getBoundedPosition(this, p0, pos, len);
                if (p0 == getStartOffset() && p1 == getEndOffset()) return this;
                return createFragment(p0, p1);
            }
            return this;
        }
    }
}
