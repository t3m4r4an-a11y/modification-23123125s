package net.macos.client.hud.impl;

import net.macos.client.gui.font.AetherionFont;
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

        int mouseXBase = dX + KEY_SIZE + KEY_GAP * 2;
        boolean left = mc.options.attackKey.isPressed();
        boolean right = mc.options.useKey.isPressed();
        int lmbCps = leftClicks.size();
        int rmbCps = rightClicks.size();

        // PASS 1: Backgrounds & Borders
        drawKeyBg(ctx, wX, wY, KEY_SIZE, w, bgNormal, borderNormal, pressedBg);
        drawKeyBg(ctx, aX, asdY, KEY_SIZE, a, bgNormal, borderNormal, pressedBg);
        drawKeyBg(ctx, sX, asdY, KEY_SIZE, s, bgNormal, borderNormal, pressedBg);
        drawKeyBg(ctx, dX, asdY, KEY_SIZE, d, bgNormal, borderNormal, pressedBg);
        drawKeyBg(ctx, mouseXBase, wY, KEY_SIZE + 12, left, bgNormal, borderNormal, pressedBg);
        drawKeyBg(ctx, mouseXBase, asdY, KEY_SIZE + 12, right, bgNormal, borderNormal, pressedBg);

        ctx.draw();

        // PASS 2: Text Labels
        drawKeyLabel(ctx, wX, wY, KEY_SIZE, "W", w, textNormal, pressedText);
        drawKeyLabel(ctx, aX, asdY, KEY_SIZE, "A", a, textNormal, pressedText);
        drawKeyLabel(ctx, sX, asdY, KEY_SIZE, "S", s, textNormal, pressedText);
        drawKeyLabel(ctx, dX, asdY, KEY_SIZE, "D", d, textNormal, pressedText);
        drawKeyLabel(ctx, mouseXBase, wY, KEY_SIZE + 12, "LMB " + lmbCps, left, textNormal, pressedText);
        drawKeyLabel(ctx, mouseXBase, asdY, KEY_SIZE + 12, "RMB " + rmbCps, right, textNormal, pressedText);
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

    private void drawKeyBg(DrawContext ctx, int kx, int ky, int kw, boolean pressed,
                           int bgNormal, int borderNormal, int pressedBg) {
        int bg = pressed ? pressedBg : bgNormal;
        GlassRenderer.roundedRect(ctx, kx, ky, kw, KEY_SIZE, 4, applyAlpha(bg));
        GlassRenderer.roundedBorder(ctx, kx, ky, kw, KEY_SIZE, 4, applyAlpha(borderNormal));
    }

    private void drawKeyLabel(DrawContext ctx, int kx, int ky, int kw, String label, boolean pressed,
                              int textNormal, int pressedText) {
        int fg = pressed ? pressedText : textNormal;
        int tw = AetherionFont.width(label);
        int tx = kx + (kw - tw) / 2;
        int ty = ky + (KEY_SIZE - 8) / 2;
        AetherionFont.draw(ctx, label, tx, ty, fg);
    }
}