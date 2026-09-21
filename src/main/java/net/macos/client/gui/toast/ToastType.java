package net.macos.client.gui.toast;

public enum ToastType {
    SUCCESS("#4ADE80", "✓"),
    INFO("#00D4FF", "i"),
    WARNING("#FBBF24", "!"),
    ERROR("#FF4444", "x");

    public final String colorHex;
    public final String symbol;

    ToastType(String colorHex, String symbol) {
        this.colorHex = colorHex;
        this.symbol = symbol;
    }

    public int getColor() {
        try {
            return 0xFF000000 | Integer.parseInt(colorHex.replace("#", ""), 16);
        } catch (Exception e) {
            return 0xFFFFFFFF;
        }
    }
}