package net.macos.client.hud.impl;

import net.macos.client.hud.glass.GlassWidget;
import net.macos.client.hud.glass.PanelStyle;
import net.macos.client.hud.glass.GlassRenderer;
import net.macos.client.utils.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.Deque;

public class KeystrokesWidget extends GlassWidget {

    private static final int KEY_SIZE = 18;
    private static final int KEY_GAP = 3;
    private static final int PADDING = 6;

    private final Deque<Long> leftClicks = new ArrayDeque<>();
    private final Deque<Long> rightClicks = new ArrayDeque<>();
    private boolean wasLeftPressed = false;
    private boolean wasRightPressed = false;

    public KeystrokesWidget() {
        super("keystrokes");
        this.appearSpeed = 4f;
    }

    @Override
    protected void configureStyle(PanelStyle s) {
        s.radius = 8;
        s.bgColor = 0x00000000;
        s.borderColor = 0x00000000;
        s.topAccent = false;
        s.gradientTop = 0;
        s.gradientBot = 0;
    }

    @Override
    protected void measure(float delta) {
        // Ширина: 4 клавиши WASD по 18 + gap + паддинг с обеих сторон
        // W сверху по центру A/S/D, потом LMB/RMB справа
        int width = PADDING * 2
                  + 3 * KEY_SIZE + 2 * KEY_GAP           // A S D
                  + KEY_GAP * 2 + (KEY_SIZE + 12);       // gap + LMB/RMB (ширина = KEY_SIZE+12)
        this.w = width;
        this.h = PADDING * 2 + 2 * KEY_SIZE + KEY_GAP;
    }

    @Override
    protected void renderInner(DrawContext ctx, int mouseX, int mouseY, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options == null) return;

        updateCps(mc);

        Color accent = RenderUtils.hexToColor(
            net.macos.client.config.ConfigManager.INSTANCE.accentColor);

        int bgNormal = (int) (0x8C << 24) | 0x14141F;
        int borderNormal = 0x20FFFFFF;
        int textNormal = applyAlpha(0xFFFFFFFF);
        int pressedBg = (200 << 24) | (accent.getRGB() & 0xFFFFFF);
        int pressedText = 0xFF000000;

        // Позиции внутри панели
        int wX = x + PADDING + KEY_SIZE + KEY_GAP;
        int wY = y + PADDING;

        int aX = x + PADDING;
        int sX = aX + KEY_SIZE + KEY_GAP;
        int dX = sX + KEY_SIZE + KEY_GAP;
        int asdY = wY + KEY_SIZE + KEY_GAP;

        boolean w = mc.options.forwardKey.isPressed();
        boolean a = mc.options.leftKey.isPressed();
        boolean s = mc.options.backKey.isPressed();
        boolean d = mc.options.rightKey.isPressed();

        drawKey(ctx, mc, wX, wY, "W", w, bgNormal, borderNormal, textNormal, pressedBg, pressedText);
        drawKey(ctx, mc, aX, asdY, "A", a, bgNormal, borderNormal, textNormal, pressedBg, pressedText);
        drawKey(ctx, mc, sX, asdY, "S", s, bgNormal, borderNormal, textNormal, pressedBg, pressedText);
        drawKey(ctx, mc, dX, asdY, "D", d, bgNormal, borderNormal, textNormal, pressedBg, pressedText);

        // LMB / RMB
        int mouseXBase = dX + KEY_SIZE + KEY_GAP * 2;
        boolean left = mc.options.attackKey.isPressed();
        boolean right = mc.options.useKey.isPressed();

        int lmbCps = leftClicks.size();
        int rmbCps = rightClicks.size();

        drawMouseKey(ctx, mc, mouseXBase, wY, "LMB", lmbCps, left, bgNormal, borderNormal, textNormal, pressedBg, pressedText);
        drawMouseKey(ctx, mc, mouseXBase, asdY, "RMB", rmbCps, right, bgNormal, borderNormal, textNormal, pressedBg, pressedText);
    }

    private void updateCps(MinecraftClient mc) {
        boolean left = mc.options.attackKey.isPressed();
        boolean right = mc.options.useKey.isPressed();
        long now = System.currentTimeMillis();

        if (left && !wasLeftPressed) leftClicks.addLast(now);
        if (right && !wasRightPressed) rightClicks.addLast(now);

        wasLeftPressed = left;
        wasRightPressed = right;

        while (!leftClicks.isEmpty() && now - leftClicks.peekFirst() > 1000) leftClicks.pollFirst();
        while (!rightClicks.isEmpty() && now - rightClicks.peekFirst() > 1000) rightClicks.pollFirst();
    }

    private void drawKey(DrawContext ctx, MinecraftClient mc,
                         int kx, int ky, String label, boolean pressed,
                         int bgNormal, int borderNormal, int textNormal,
                         int pressedBg, int pressedText) {

        int bg = pressed ? pressedBg : bgNormal;
        int fg = pressed ? pressedText : textNormal;

        // Скруглённый фон через GlassRenderer
        GlassRenderer.roundedRect(ctx, kx, ky, KEY_SIZE, KEY_SIZE, 4, applyAlpha(bg));

        // Бордер
        ctx.fill(kx + 1, ky, kx + KEY_SIZE - 1, ky + 1, applyAlpha(borderNormal));
        ctx.fill(kx + 1, ky + KEY_SIZE - 1, kx + KEY_SIZE - 1, ky + KEY_SIZE, applyAlpha(borderNormal));

        int tw = mc.textRenderer.getWidth(label);
        int tx = kx + (KEY_SIZE - tw) / 2;
        int ty = ky + (KEY_SIZE - 8) / 2;
        ctx.drawTextWithShadow(mc.textRenderer, label, tx, ty, fg);
    }

    private void drawMouseKey(DrawContext ctx, MinecraftClient mc,
                              int kx, int ky, String label, int cps, boolean pressed,
                              int bgNormal, int borderNormal, int textNormal,
                              int pressedBg, int pressedText) {

        int w = KEY_SIZE + 12;
        int bg = pressed ? pressedBg : bgNormal;
        int fg = pressed ? pressedText : textNormal;

        GlassRenderer.roundedRect(ctx, kx, ky, w, KEY_SIZE, 4, applyAlpha(bg));
        ctx.fill(kx + 1, ky, kx + w - 1, ky + 1, applyAlpha(borderNormal));
        ctx.fill(kx + 1, ky + KEY_SIZE - 1, kx + w - 1, ky + KEY_SIZE, applyAlpha(borderNormal));

        String text = label + " " + cps;
        int tw = mc.textRenderer.getWidth(text);
        int tx = kx + (w - tw) / 2;
        int ty = ky + (KEY_SIZE - 8) / 2;
        ctx.drawTextWithShadow(mc.textRenderer, text, tx, ty, fg);
    }
}