package net.macos.client.hud.glass;

public class PanelStyle {

    public int radius = 8;
    public int bgColor = 0xC014141F;
    public int borderColor = 0x20FFFFFF;
    public boolean topAccent = true;
    public int accentColor = 0xFF00D4FF;
    public int glowLayers = 0;
    public int glowColor = 0x3000D4FF;

    // Градиент: 0 = выкл
    public int gradientTop = 0;   // например 0x10FFFFFF
    public int gradientBot = 0;   // например 0x20000000

    public static PanelStyle defaultPanel() {
        PanelStyle s = new PanelStyle();
        s.gradientTop = 0x14FFFFFF;
        s.gradientBot = 0x18000000;
        return s;
    }

    public static PanelStyle tooltip() {
        PanelStyle s = new PanelStyle();
        s.radius = 4;
        s.bgColor = 0xF01A1A2E;
        s.borderColor = 0x40FFFFFF;
        s.topAccent = false;
        return s;
    }

    public static PanelStyle toast() {
        PanelStyle s = new PanelStyle();
        s.radius = 10;
        s.bgColor = 0xE01A1A2E;
        s.glowLayers = 6;
        s.gradientTop = 0x18FFFFFF;
        return s;
    }

    public static PanelStyle dock() {
        PanelStyle s = new PanelStyle();
        s.radius = 12;
        s.bgColor = 0xD0181828;
        s.borderColor = 0x30FFFFFF;
        s.topAccent = false;
        s.gradientTop = 0x10FFFFFF;
        return s;
    }
}