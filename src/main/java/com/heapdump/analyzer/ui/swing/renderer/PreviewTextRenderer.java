package com.heapdump.analyzer.ui.swing.renderer;

import com.heapdump.analyzer.ui.swing.ThemeConfig;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.function.IntSupplier;

public class PreviewTextRenderer extends DefaultTableCellRenderer implements HoverAware {

    private static final int MAX_PREVIEW_LENGTH = 80;
    private IntSupplier hoverRowSupplier = () -> -1;

    public PreviewTextRenderer() {
        setFont(ThemeConfig.FONT_MONO);
    }

    @Override
    public void setHoverRowSupplier(IntSupplier supplier) {
        this.hoverRowSupplier = supplier != null ? supplier : () -> -1;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int col) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);

        String text = value != null ? value.toString() : "";
        if (text.length() > MAX_PREVIEW_LENGTH) {
            setText(text.substring(0, MAX_PREVIEW_LENGTH) + "...");
            setToolTipText(text);
        } else {
            setText(text);
            setToolTipText(text.length() > 30 ? text : null);
        }

        setFont(ThemeConfig.FONT_MONO);

        boolean hovered = row == hoverRowSupplier.getAsInt();
        if (hovered && !isSelected) {
            setBackground(ThemeConfig.getAlternateRowColor());
        }

        return this;
    }
}
