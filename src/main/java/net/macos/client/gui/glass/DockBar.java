package net.macos.client.gui.glass;

import net.macos.client.hud.glass.PanelStyle;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Bottom macOS-style dock for menu tabs.
 */
public final class DockBar {

    public record Tab(String id, String label) {}

    private final List<Tab> tabs;
    private String activeId;
    private int x, y, w, h;
    private int tabW;

    public DockBar(List<Tab> tabs, String activeId) {
        this.tabs = tabs;
        this.activeId = activeId;
    }

    public void setActive(String id) {
        this.activeId = id;
    }

    public String getActive() {
        return activeId;
    }

    public void layoutBottomCenter(int screenW, int screenH, int height, int margin) {
        this.h = height;
        this.w = Math.min(screenW - margin * 2, tabs.size() * 100 + 24);
        this.tabW = Math.max(64, (w - 16) / Math.max(1, tabs.size()));
        this.x = (screenW - w) / 2;
        this.y = screenH - h - margin;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return w; }
    public int getHeight() { return h; }

    public void render(DrawContext ctx, int mouseX, int mouseY, int accent) {
        GlassSurface.drawDock(ctx, x, y, w, h);

        var tr = MinecraftClient.getInstance().textRenderer;
        int tx = x + 8;
        int ty = y + (h - 10) / 2;

        for (Tab tab : tabs) {
            boolean active = tab.id().equals(activeId);
            boolean hover = mouseX >= tx && mouseX < tx + tabW && mouseY >= y && mouseY < y + h;

            if (active) {
                int pad = 4;
                ctx.fill(tx + pad, y + pad, tx + tabW - pad, y + h - pad, accent);
            } else if (hover) {
                ctx.fill(tx + 4, y + 4, tx + tabW - 4, y + h - 4, 0x28FFFFFF);
            }

            String label = tab.label();
            int tw = tr.getWidth(label);
            int color = active ? 0xFF101018 : 0xFFE8EEF8;
            ctx.drawText(tr, Text.literal(label), tx + (tabW - tw) / 2, ty, color, false);
            tx += tabW;
        }
    }

    /** @return tab id if a tab was clicked, otherwise null */
    public String click(double mouseX, double mouseY) {
        if (mouseX < x || mouseX >= x + w || mouseY < y || mouseY >= y + h) {
            return null;
        }
        int idx = (int) ((mouseX - x - 8) / tabW);
        if (idx < 0 || idx >= tabs.size()) {
            return null;
        }
        activeId = tabs.get(idx).id();
        return activeId;
    }
}
