package net.macos.client.gui;

import net.macos.client.config.ConfigManager;
import net.macos.client.utils.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import java.awt.Color;

public abstract class GlassPanel {
    protected int x, y, width, height;
    protected String title;
    protected boolean isDragging = false;
    protected int dragOffsetX, dragOffsetY;

    // Прозрачность панели (0..1) — для плавного скрытия
    protected float panelAlpha = 1f;

    public GlassPanel(int x, int y, int width, int height, String title) {
        this.x = x; this.y = y; this.width = width; this.height = height; this.title = title;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public void applyPosition(int newX, int newY) {
        this.x = newX;
        this.y = newY;
        savePosition();
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // ⚠️ ВАЖНО: сначала вызываем renderContent — он обновит panelAlpha.
        // Не проверяем alpha до этого, иначе застрянем в 0 навсегда.
        renderContent(context, mouseX, mouseY, delta);

        // Теперь рисуем панель, если она видима
        if (panelAlpha > 0.01f) {
            int a = (int)(panelAlpha * 255);

            Color accent = RenderUtils.hexToColor(ConfigManager.INSTANCE.accentColor);
            Color bg = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(),
                (int)(40 * panelAlpha));

            RenderUtils.drawRoundedRect(context, x, y, width, height,
                ConfigManager.INSTANCE.cornerRadius, bg);

            int borderColor = (a << 24) | (accent.getRGB() & 0xFFFFFF);
            context.fill(x, y, x + width, y + 1, borderColor);
            context.fill(x, y, x + 1, y + height, borderColor);
            context.fill(x + width - 1, y, x + width, y + height, borderColor);
            context.fill(x, y + height - 1, x + width, y + height, borderColor);
        }

        // Drag-логика (только если панель видна)
        if (panelAlpha > 0.5f) {
            if (isMouseOver(mouseX, mouseY)) {
                if (MinecraftClient.getInstance().options.useKey.isPressed()) {
                    isDragging = true;
                    dragOffsetX = mouseX - x;
                    dragOffsetY = mouseY - y;
                }
            }
            if (isDragging && !MinecraftClient.getInstance().options.useKey.isPressed()) {
                isDragging = false;
                savePosition();
            }
            if (isDragging) {
                x = mouseX - dragOffsetX;
                y = mouseY - dragOffsetY;
            }
        }
    }

    protected abstract void renderContent(DrawContext context, int mouseX, int mouseY, float delta);
    protected abstract void savePosition();

    protected boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}