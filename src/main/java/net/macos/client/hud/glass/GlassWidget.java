package net.macos.client.hud.glass;

import net.macos.client.config.ConfigManager;
import net.macos.client.gui.GlassTheme;
import net.macos.client.gui.glass.GlassSurface;
import net.macos.client.hud.HudWidget;
import net.minecraft.client.gui.DrawContext;

/**
 * HUD widget with Liquid Glass chrome (backdrop blur + panel).
 */
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

        if (appearProgress > 0.05f) {
            GlassSurface.draw(ctx, x, y, w, h, style);
        }

        renderInner(ctx, mouseX, mouseY, delta);
    }

    protected abstract void renderInner(DrawContext ctx, int mouseX, int mouseY, float delta);

    private int parseAccentColor() {
        return GlassTheme.accent();
    }
}
