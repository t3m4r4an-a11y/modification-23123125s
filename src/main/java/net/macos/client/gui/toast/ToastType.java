package net.macos.client.gui.toast;

import net.macos.client.gui.icon.MacIcons;

public enum ToastType {
    SUCCESS("#4ADE80", MacIcons.CHECK),
    INFO   ("#00D4FF", MacIcons.BELL),
    WARNING("#FBBF24", MacIcons.WARNING),
    ERROR  ("#FF4444", MacIcons.CLOSE);

    public final String colorHex;
    public final String icon;

    ToastType(String colorHex, String icon) {
        this.colorHex = colorHex;
        this.icon = icon;
    }

    public int getColor() {
        try {
            return 0xFF000000 | Integer.parseInt(colorHex.replace("#", ""), 16);
        } catch (Exception e) {
            return 0xFFFFFFFF;
        }
    }
}