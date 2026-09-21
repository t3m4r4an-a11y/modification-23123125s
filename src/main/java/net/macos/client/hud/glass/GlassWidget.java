package net.macos.client.hud.glass;

import net.macos.client.config.ConfigManager;
import net.macos.client.hud.HudWidget;
import net.macos.client.render.BlurRenderer;
import net.minecraft.client.gui.DrawContext;

public abstract class GlassWidget extends HudWidget {

    protected PanelStyle style = PanelStyle.defaultPanel();

    protected GlassWidget(String key) {
        super(key);
    }

    protected void configureStyle(PanelStyle s) {}

    @Override
    protected final void renderContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
        configureStyle(style);
        style.accentColor = parseAccentColor();

        // === Блюр под панелью ===
        if (ConfigManager.INSTANCE.enableGlassBlur && appearProgress > 0.9f) {
            int inset = Math.max(0, style.radius - 2);
            int bw = w - inset * 2;
            int bh = h - inset * 2;
            if (bw > 0 && bh > 0) {
                BlurRenderer.drawBlurredRegion(ctx, x + inset, y + inset, bw, bh);
            }
        }

        // === Панель поверх блюра (чуть прозрачнее, чтобы блюр просвечивал) ===
        int oldBg = style.bgColor;
        if (ConfigManager.INSTANCE.enableGlassBlur) {
            int bgA = (style.bgColor >>> 24) & 0xFF;
            int newA = Math.max(30, bgA / 4);   // сильно прозрачнее, но не в ноль
            style.bgColor = (newA << 24) | (style.bgColor & 0xFFFFFF);
        }

        GlassRenderer.drawPanel(ctx, x, y, w, h, style);

        style.bgColor = oldBg;

        renderInner(ctx, mouseX, mouseY, delta);
    }

    protected abstract void renderInner(DrawContext ctx, int mouseX, int mouseY, float delta);

    private int parseAccentColor() {
        try {
            String hex = ConfigManager.INSTANCE.accentColor.replace("#", "");
            return 0xFF000000 | Integer.parseInt(hex, 16);
        } catch (Exception e) {
            return 0xFF00D4FF;
        }
    }
}