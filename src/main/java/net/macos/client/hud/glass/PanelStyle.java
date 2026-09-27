package net.macos.client.hud.glass;

/**
 * Visual style for Liquid Glass panels (iOS-like).
 * Values are defaults; widgets can tweak via configureStyle().
 */
public class PanelStyle {

    public int radius = 12;
    /** Base fill — more transparent when blur is under the panel. */
    public int bgColor = 0x99101828;
    public int borderColor = 0x28FFFFFF;
    public boolean topAccent = false;
    public int accentColor = 0xFF00D4FF;
    public int glowLayers = 0;
    public int glowColor = 0x2800D4FF;

    /** Soft top specular (Liquid Glass highlight). 0 = off */
    public int gradientTop = 0x22FFFFFF;
    /** Soft bottom depth. 0 = off */
    public int gradientBot = 0x14000000;

    /** Extra inner highlight line under the top edge */
    public boolean specular = true;
    public int specularColor = 0x40FFFFFF;

    public static PanelStyle defaultPanel() {
        return new PanelStyle();
    }

    public static PanelStyle tooltip() {
        PanelStyle s = new PanelStyle();
        s.radius = 8;
        s.bgColor = 0xCC121820;
        s.borderColor = 0x35FFFFFF;
        s.topAccent = false;
        s.specular = true;
        s.gradientTop = 0x18FFFFFF;
        s.gradientBot = 0x10000000;
        return s;
    }

    public static PanelStyle toast() {
        PanelStyle s = new PanelStyle();
        s.radius = 14;
        s.bgColor = 0xB0141824;
        s.glowLayers = 4;
        s.glowColor = 0x2200D4FF;
        s.gradientTop = 0x28FFFFFF;
        s.specular = true;
        return s;
    }

    public static PanelStyle dock() {
        PanelStyle s = new PanelStyle();
        s.radius = 16;
        s.bgColor = 0xA0121824;
        s.borderColor = 0x30FFFFFF;
        s.topAccent = false;
        s.specular = true;
        s.gradientTop = 0x1AFFFFFF;
        s.gradientBot = 0x12000000;
        return s;
    }
}
