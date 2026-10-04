package net.macos.client.gui;

import net.macos.client.config.ConfigManager;

/**
 * Центральное место для цветов/радиусов "стекла". Всё, что раньше
 * хардкодило 0x14141F / 0x20FFFFFF и т.п., должно брать цвет отсюда —
 * тогда весь визуал клиента правится в одном файле.
 */
public final class GlassTheme {

    private GlassTheme() {}

    public static int panelBg()       { return 0xB014141F; }
    public static int panelBorder()   { return 0x20FFFFFF; }
    public static int panelRadius()   { return ConfigManager.INSTANCE.cornerRadius; }

    public static int textPrimary()   { return 0xFFFFFFFF; }
    public static int textSecondary() { return 0xB0FFFFFF; }
    public static int textMuted()     { return 0x80FFFFFF; }

    public static int good()   { return 0xFF4ADE80; }
    public static int warn()   { return 0xFFFBBF24; }
    public static int danger() { return 0xFFFF4444; }
    public static int info()   { return 0xFF00D4FF; }

    public static int accentRGB() {
        try {
            return Integer.parseInt(ConfigManager.INSTANCE.accentColor.replace("#", ""), 16) & 0xFFFFFF;
        } catch (Exception e) {
            return 0x00D4FF;
        }
    }

    public static int accent() {
        return 0xFF000000 | accentRGB();
    }

    /** Полупрозрачный фон ячейки (armor/potion HUD) — специально не слишком плотный, чтобы блюр под ним был виден. */
    public static int cellBg(int alpha) {
        int a = Math.min(90, Math.max(20, alpha));
        return (a << 24) | 0x14141F;
    }
}