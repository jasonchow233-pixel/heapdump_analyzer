package com.heapdump.analyzer.ui.swing.renderer;

import java.util.function.IntSupplier;

public interface HoverAware {
    void setHoverRowSupplier(IntSupplier supplier);
}
