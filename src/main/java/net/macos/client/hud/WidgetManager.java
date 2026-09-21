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
            w.render(ctx, mouseX, mouseY, delta, mouseDown);
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