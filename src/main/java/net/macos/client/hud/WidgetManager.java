package net.macos.client.hud;

import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

public class WidgetManager {

    public static final WidgetManager INSTANCE = new WidgetManager();

    private final List<HudWidget> widgets = new ArrayList<>();

    public void register(HudWidget widget) {
        widgets.add(widget);
    }

    public void renderAll(DrawContext ctx, int mouseX, int mouseY,
                          float delta, boolean mouseDown) {
        for (HudWidget w : widgets) {
            ctx.getMatrices().push();
            try {
                w.render(ctx, mouseX, mouseY, delta, mouseDown);
            } catch (Throwable t) {
                // Silently isolate widget render failure so other widgets stay visible
            } finally {
                ctx.getMatrices().pop();
                com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            }
        }
    }

    public List<HudWidget> all() {
        return widgets;
    }

    @SuppressWarnings("unchecked")
    public <T extends HudWidget> T get(String key) {
        for (HudWidget w : widgets) {
            if (w.getKey().equals(key)) return (T) w;
        }
        return null;
    }
}