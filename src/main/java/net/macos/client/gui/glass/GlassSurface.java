package net.macos.client.gui.glass;

import net.macos.client.config.ConfigManager;
import net.macos.client.hud.glass.GlassRenderer;
import net.macos.client.hud.glass.PanelStyle;
import net.macos.client.render.GlassBackdrop;
import net.minecraft.client.gui.DrawContext;

/**
 * Unified Liquid Glass surface: backdrop blur + chrome (fill/border/specular).
 * Use for HUD widgets and menu panels.
 */
public final class GlassSurface {

    private GlassSurface() {}

    public static void draw(DrawContext ctx, int x, int y, int w, int h, PanelStyle style) {
        if (w <= 0 || h <= 0) {
            return;
        }

        PanelStyle s = style != null ? style : PanelStyle.defaultPanel();

        if (ConfigManager.INSTANCE.enableGlassBlur) {
            int inset = Math.max(0, s.radius / 4);
            int bx = x + inset;
            int by = y + inset;
            int bw = w - inset * 2;
            int bh = h - inset * 2;
            if (bw > 0 && bh > 0) {
                GlassBackdrop.draw(ctx, bx, by, bw, bh);
            }
        }

        GlassRenderer.drawPanel(ctx, x, y, w, h, s);
    }

    public static void drawDock(DrawContext ctx, int x, int y, int w, int h) {
        draw(ctx, x, y, w, h, PanelStyle.dock());
    }
}
