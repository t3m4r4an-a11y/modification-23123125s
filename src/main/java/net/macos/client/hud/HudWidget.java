package net.macos.client.hud;

import net.macos.client.MacClient;
import net.macos.client.config.ConfigManager;
import net.macos.client.config.WidgetPos;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public abstract class HudWidget {

    protected final String key;
    protected int x, y;
    protected int w = 0, h = 0;
    protected float appearProgress = 0f;

    private boolean dragging = false;
    private int dragOffX, dragOffY;
    private static HudWidget activeDrag = null;
    
    protected float appearSpeed = 6f;

    protected HudWidget(String key) {
        this.key = key;
        WidgetPos pos = ConfigManager.INSTANCE.getWidget(key);
        this.x = pos.x;
        this.y = pos.y;
    }

    public final String getKey() { return key; }
    public final int getX() { return x; }
    public final int getY() { return y; }
    public final int getWidth() { return w; }
    public final int getHeight() { return h; }
    public final float getAppearProgress() { return appearProgress; }

    public void setPosition(int nx, int ny) {
        this.x = nx;
        this.y = ny;
        WidgetPos p = ConfigManager.INSTANCE.getWidget(key);
        p.x = nx;
        p.y = ny;
    }

    protected boolean shouldBeVisible() {
        return ConfigManager.INSTANCE.getWidget(key).enabled;
    }

    protected void measure(float delta) {}

    protected abstract void renderContent(DrawContext ctx, int mouseX, int mouseY, float delta);

    /** Полный вызов снаружи. mouseDown — ЛКМ зажата. */
        public final void render(DrawContext ctx, int mouseX, int mouseY,
                             float tickDelta, boolean mouseDown) {
        float frameDelta = MinecraftClient.getInstance().getLastFrameDuration();

        float target = shouldBeVisible() ? 1f : 0f;
        float t = Math.min(1f, frameDelta * appearSpeed);
        appearProgress += (target - appearProgress) * t;
        if (Math.abs(appearProgress - target) < 0.003f) appearProgress = target;

        if (appearProgress < 0.01f) return;

        measure(frameDelta);
        handleDrag(mouseX, mouseY, mouseDown);
        renderContent(ctx, mouseX, mouseY, frameDelta);
    }

    private void handleDrag(int mx, int my, boolean mouseDown) {
        if (!MacClient.hudEditorOpen) return;
        if (w <= 0 || h <= 0) return;

        boolean inside = mx >= x && mx <= x + w && my >= y && my <= y + h;

        // Начать drag можно только если никто другой не тащится
        if (inside && mouseDown && !dragging && activeDrag == null) {
            dragging = true;
            activeDrag = this;
            dragOffX = mx - x;
            dragOffY = my - y;
        }

        // Отпустили ЛКМ — сохраняем и освобождаем lock
        if (!mouseDown && dragging) {
            dragging = false;
            if (activeDrag == this) activeDrag = null;

            WidgetPos p = ConfigManager.INSTANCE.getWidget(key);
            p.x = x;
            p.y = y;
            ConfigManager.save();
        }

        // Тащим только если Я — activeDrag
        if (dragging && mouseDown && activeDrag == this) {
            x = mx - dragOffX;
            y = my - dragOffY;

            if (ConfigManager.INSTANCE.hudEditorSnap) {
                int grid = Math.max(2, ConfigManager.INSTANCE.hudEditorGrid);
                x = Math.round((float) x / grid) * grid;
                y = Math.round((float) y / grid) * grid;
            }
        }
    }

    protected int applyAlpha(int color) {
        int baseA = (color >>> 24) & 0xFF;
        int a = (int) (baseA * appearProgress);
        return (a << 24) | (color & 0xFFFFFF);
    }

    public int[] getBlurRegion() {
        if (appearProgress < 0.5f) return null;
        if (w <= 0 || h <= 0) return null;
        return new int[]{ x, y, w, h };
    }
}