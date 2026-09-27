package net.macos.client.gui.icon;

import net.minecraft.util.Identifier;

/**
 * SF Symbols codepoints. Font is TrueType (glyf+loca) converted from SFSymbolsFallback.otf.
 */
public final class MacIcons {

    public static final Identifier FONT =
            new Identifier("macclient", "mac_icons");

    private MacIcons() {}

    private static String icon(int codePoint) {
        return new String(Character.toChars(codePoint));
    }

    public static final String GENERAL   = icon(0x10035F);
    public static final String EDITOR    = icon(0x1020A);
    public static final String HUD       = icon(0x101A95);
    public static final String VISUALS   = icon(0x10205D);
    public static final String VIEWMODEL = icon(0x10027B);
    public static final String MISC      = icon(0x100360);

    public static final String GAUGE = icon(0x10037E);
    public static final String WIFI  = icon(0x100647);

    public static final String CLOCK        = icon(0x10042B);
    public static final String CLOCK_FILLED = icon(0x10042C);

    public static final String SUN = icon(0x1001AE);
    public static final String MOON = icon(0x1001BA);
    public static final String CLOUD = icon(0x1001C3);
    public static final String RAIN = icon(0x1001C7);
    public static final String HEAVY_RAIN = icon(0x1001C9);
    public static final String THUNDERSTORM = icon(0x1001D3);
    public static final String SNOW = icon(0x1001E5);

    public static final String TARGET = icon(0x100429);
    public static final String HEART = icon(0x1002B4);
    public static final String HEART_FILLED = icon(0x1002B5);
    public static final String SHIELD = icon(0x100666);
    public static final String LIGHTNING = icon(0x1002E5);
    public static final String FIRE = icon(0x10066C);
    public static final String BLOOM = icon(0x1008F3);

    public static final String SEARCH = icon(0x1002AB);
    public static final String CHAT = icon(0x10057B);
    public static final String BELL = icon(0x1002D9);
    public static final String CHECK = icon(0x100062);
    public static final String WARNING = icon(0x1001FE);
    public static final String LOCK = icon(0x1003A0);
    public static final String UNLOCK = icon(0x1003A4);
    public static final String FOLDER = icon(0x100215);
    public static final String PROFILE = icon(0x10026D);
    public static final String KEYBOARD = icon(0x1001F3);
    public static final String QUESTION = icon(0x10014D);
    public static final String CLOSE = icon(0x10017E);

    public static final String PILLS = icon(0x100831);
    public static final String DROPLET = icon(0x101E61);
    public static final String EYE = icon(0x1002ED);
    public static final String EYE_FILLED = icon(0x1002EE);
    public static final String WINDOWS = icon(0x103E7);
}
