package com.heapdump.analyzer.ui.swing.renderer;

import cn.wanghw.SensitivityCategory;
import com.heapdump.analyzer.ui.swing.ThemeConfig;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.function.IntSupplier;

public class CategoryChipRenderer extends JPanel implements TableCellRenderer, HoverAware {

    private final JLabel label;
    private IntSupplier hoverRowSupplier = () -> -1;

    public CategoryChipRenderer() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        setOpaque(false);
        label = new JLabel();
        label.setFont(ThemeConfig.FONT_CAPTION);
        label.setOpaque(true);
        label.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        add(label);
    }

    @Override
    public void setHoverRowSupplier(IntSupplier supplier) {
        this.hoverRowSupplier = supplier != null ? supplier : () -> -1;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int col) {
        String text = value != null ? value.toString() : "";
        label.setText(text);

        SensitivityCategory cat = SensitivityCategory.fromString(text);
        Color chipColor;
        try {
            chipColor = Color.decode(cat.getBackgroundColor());
        } catch (Exception e) {
            chipColor = ThemeConfig.SEVERITY_INFO;
        }
        Color chipBg = ThemeConfig.blend(table.getBackground(), chipColor, 0.25f);

        boolean hovered = row == hoverRowSupplier.getAsInt();
        Color bg = table.getBackground();
        if (hovered && !isSelected) {
            bg = ThemeConfig.getAlternateRowColor();
        }

        if (isSelected) {
            setBackground(table.getSelectionBackground());
            label.setForeground(table.getSelectionForeground());
            label.setBackground(chipBg);
        } else {
            setBackground(bg);
            label.setForeground(chipColor);
            label.setBackground(chipBg);
        }

        return this;
    }
}
