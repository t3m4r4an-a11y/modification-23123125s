package net.macos.client.gui;

import java.util.List;

public interface BlurableScreen {
    /**
     * @return список регионов {x, y, w, h} в screen-координатах, или null = блюрить весь экран
     */
    List<int[]> getBlurRegions();
}