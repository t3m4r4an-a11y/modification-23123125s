package net.macos.client.gui;

import net.macos.client.config.ConfigManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.awt.Color;

public class ColorPickerScreen extends Screen {

    private static final int PANEL_W = 280;
    private static final int PANEL_H = 340;
    private static final int SV_SIZE = 200;
    private static final int HUE_H = 16;

    private int panelX, panelY;
    private int svX, svY, hueX, hueY;

    // Текущее состояние
    private float hue = 0.5f;       // 0..1
    private float saturation = 1f;  // 0..1
    private float brightness = 1f;  // 0..1

    // Оригинал — для кнопки Cancel
    private final int originalColor;
    private final Screen parent;

    // Drag
    private boolean draggingSV = false;
    private boolean draggingHue = false;

    public ColorPickerScreen(Screen parent, int initialColor) {
        super(Text.literal("Color Picker"));
        this.parent = parent;
        this.originalColor = initialColor;

        float[] hsb = Color.RGBtoHSB(
            (initialColor >> 16) & 0xFF,
            (initialColor >> 8) & 0xFF,
            initialColor & 0xFF,
            null);
        this.hue = hsb[0];
        this.saturation = hsb[1];
        this.brightness = hsb[2];
    }

    @Override
    protected void init() {
        super.init();
        panelX = (width - PANEL_W) / 2;
        panelY = (height - PANEL_H) / 2;
        svX = panelX + 40;
        svY = panelY + 50;
        hueX = svX;
        hueY = svY + SV_SIZE + 20;
    }

    private int getCurrentColor() {
        int rgb = Color.HSBtoRGB(hue, saturation, brightness);
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }

    private String getCurrentHex() {
        return String.format("#%06X", getCurrentColor() & 0xFFFFFF);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Затемнение фона
        context.fill(0, 0, width, height, 0xC0101018);

        int accent = 0xFF00D4FF;

        // ===== Панель =====
        context.fill(panelX, panelY, panelX + PANEL_W, panelY + PANEL_H, 0xF01A1A2E);
        context.fill(panelX, panelY, panelX + PANEL_W, panelY + 1, accent);
        context.fill(panelX, panelY + PANEL_H - 1, panelX + PANEL_W, panelY + PANEL_H, accent);
        context.fill(panelX, panelY, panelX + 1, panelY + PANEL_H, accent);
        context.fill(panelX + PANEL_W - 1, panelY, panelX + PANEL_W, panelY + PANEL_H, accent);

        // Заголовок
        context.drawTextWithShadow(textRenderer, "Color Picker", panelX + 12, panelY + 10, 0xFFFFFFFF);
        context.fill(panelX + 8, panelY + 26, panelX + PANEL_W - 8, panelY + 27, 0x40FFFFFF);

        // ===== SV-квадрат (насыщенность × яркость) =====
        // Горизонтальный градиент: от белого (s=0) к чистому цвету (s=1)
        for (int i = 0; i < SV_SIZE; i++) {
            float s = i / (float) SV_SIZE;
            int col = 0xFF000000 | (Color.HSBtoRGB(hue, s, 1f) & 0xFFFFFF);
            context.fill(svX + i, svY, svX + i + 1, svY + SV_SIZE, col);
        }
        // Вертикальный градиент: от прозрачного (v=1) к чёрному (v=0)
        for (int i = 0; i < SV_SIZE; i++) {
            float v = 1f - (i / (float) SV_SIZE);
            int alpha = (int)((1f - v) * 255);
            int col = (alpha << 24); // чёрный с прозрачностью
            context.fill(svX, svY + i, svX + SV_SIZE, svY + i + 1, col);
        }

        // Курсор SV
        int cursorX = svX + (int)(saturation * SV_SIZE);
        int cursorY = svY + (int)((1f - brightness) * SV_SIZE);
        // Обводка
        context.fill(cursorX - 5, cursorY - 5, cursorX + 5, cursorY + 5, 0xFF000000);
        context.fill(cursorX - 4, cursorY - 4, cursorX + 4, cursorY + 4, 0xFFFFFFFF);
        context.fill(cursorX - 3, cursorY - 3, cursorX + 3, cursorY + 3,
            getCurrentColor() == 0xFF000000 ? 0xFFFF0000 : getCurrentColor());

        // ===== Hue-слайдер =====
        for (int i = 0; i < SV_SIZE; i++) {
            float h = i / (float) SV_SIZE;
            int col = 0xFF000000 | (Color.HSBtoRGB(h, 1f, 1f) & 0xFFFFFF);
            context.fill(hueX + i, hueY, hueX + i + 1, hueY + HUE_H, col);
        }
        // Курсор Hue
        int hueCursorX = hueX + (int)(hue * SV_SIZE);
        context.fill(hueCursorX - 2, hueY - 2, hueCursorX + 2, hueY + HUE_H + 2, 0xFFFFFFFF);

        // ===== Превью цвета + HEX =====
        int previewY = hueY + HUE_H + 16;
        int previewColor = getCurrentColor();
        context.fill(svX, previewY, svX + 40, previewY + 24, previewColor);
        context.fill(svX, previewY, svX + 40, previewY + 1, 0x40FFFFFF);
        context.fill(svX, previewY + 23, svX + 40, previewY + 24, 0x40FFFFFF);

        context.drawTextWithShadow(textRenderer, getCurrentHex(), svX + 50, previewY + 8, 0xFFFFFFFF);

        // ===== Кнопки OK / Cancel =====
        int btnY = panelY + PANEL_H - 34;
        int btnW = 80;
        int btnH = 22;
        int okX = panelX + PANEL_W - btnW - 12;
        int cancelX = okX - btnW - 8;

        // Cancel
        boolean cancelHover = mouseX >= cancelX && mouseX <= cancelX + btnW
                          && mouseY >= btnY && mouseY <= btnY + btnH;
        context.fill(cancelX, btnY, cancelX + btnW, btnY + btnH, cancelHover ? 0x60FFFFFF : 0x30FFFFFF);
        String cancelText = "Cancel";
        int ctW = textRenderer.getWidth(cancelText);
        context.drawTextWithShadow(textRenderer, cancelText,
            cancelX + (btnW - ctW) / 2, btnY + 7, 0xFFFFFFFF);

        // OK
        boolean okHover = mouseX >= okX && mouseX <= okX + btnW
                       && mouseY >= btnY && mouseY <= btnY + btnH;
        context.fill(okX, btnY, okX + btnW, btnY + btnH, okHover ? 0xFF00E5FF : accent);
        String okText = "OK";
        int okW = textRenderer.getWidth(okText);
        context.drawTextWithShadow(textRenderer, okText,
            okX + (btnW - okW) / 2, btnY + 7, 0xFF000000);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);

        // SV квадрат
        if (mx >= svX && mx <= svX + SV_SIZE && my >= svY && my <= svY + SV_SIZE) {
            draggingSV = true;
            updateSV(mx, my);
            return true;
        }

        // Hue
        if (mx >= hueX && mx <= hueX + SV_SIZE && my >= hueY - 4 && my <= hueY + HUE_H + 4) {
            draggingHue = true;
            updateHue(mx);
            return true;
        }

        // Кнопки
        int btnY = panelY + PANEL_H - 34;
        int btnW = 80;
        int btnH = 22;
        int okX = panelX + PANEL_W - btnW - 12;
        int cancelX = okX - btnW - 8;

        if (mx >= cancelX && mx <= cancelX + btnW && my >= btnY && my <= btnY + btnH) {
            this.close();
            return true;
        }
        if (mx >= okX && mx <= okX + btnW && my >= btnY && my <= btnY + btnH) {
            // Сохраняем в конфиг
            ConfigManager.INSTANCE.accentColor = getCurrentHex();
            ConfigManager.save();
            this.close();
            return true;
        }

        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (button != 0) return super.mouseDragged(mx, my, button, dx, dy);

        if (draggingSV) {
            updateSV(mx, my);
            return true;
        }
        if (draggingHue) {
            updateHue(mx);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        draggingSV = false;
        draggingHue = false;
        return super.mouseReleased(mx, my, button);
    }

    private void updateSV(double mx, double my) {
        saturation = (float)((mx - svX) / SV_SIZE);
        brightness = 1f - (float)((my - svY) / SV_SIZE);
        saturation = Math.max(0f, Math.min(1f, saturation));
        brightness = Math.max(0f, Math.min(1f, brightness));
    }

    private void updateHue(double mx) {
        hue = (float)((mx - hueX) / SV_SIZE);
        hue = Math.max(0f, Math.min(1f, hue));
    }

    @Override
    public void close() {
        if (parent != null) {
            client.setScreen(parent);
        } else {
            super.close();
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}