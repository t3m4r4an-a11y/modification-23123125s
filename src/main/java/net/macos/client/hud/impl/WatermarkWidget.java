package net.macos.client.hud.impl;

import net.macos.client.config.ConfigManager;
import net.macos.client.hud.glass.GlassWidget;
import net.macos.client.hud.glass.PanelStyle;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class WatermarkWidget extends GlassWidget {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    // Иконки-картинки. Оставь null если не нужны, или замени путь
    private static final Identifier ICON_LOGO = null;   // логотип клиента
    private static final Identifier ICON_FPS  = null;   // иконка FPS
    private static final Identifier ICON_TIME = null;   // иконка времени

    private long lastUpdate = 0;
    private String cachedTime = "00:00";

    public WatermarkWidget() {
        super("watermark");
        this.appearSpeed = 3f;
    }

    @Override
    protected void configureStyle(PanelStyle s) {
        s.radius = 8;
        s.bgColor = 0x8C14141F;
        s.borderColor = 0x00000000;
        s.topAccent = false;
        s.gradientTop = 0;
        s.gradientBot = 0;
        s.glowLayers = 0;
    }

    @Override
    protected void measure(float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        updateTime();

        int totalWidth = 8;                          // padding слева
        totalWidth += 3;                             // акцентная полоса + gap
        totalWidth += 18;                            // логотип
        totalWidth += 6;                             // gap
        totalWidth += 18;                            // голова
        totalWidth += 4;                             // gap
        totalWidth += mc.textRenderer.getWidth(mc.getSession().getUsername());
        totalWidth += 8 + 2 + 8;                     // разделитель
        totalWidth += 16 + 4 + mc.textRenderer.getWidth("110 FPS");
        totalWidth += 8 + 2 + 8;
        totalWidth += 16 + 4 + mc.textRenderer.getWidth("0 ms");
        totalWidth += 8 + 2 + 8;
        totalWidth += mc.textRenderer.getWidth("00:00");
        totalWidth += 8;                             // padding справа

        this.w = totalWidth;
        this.h = 22;
    }

    @Override
    protected void renderInner(DrawContext ctx, int mouseX, int mouseY, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        int a = (int) (appearProgress * 255);
        int textColor = (a << 24) | 0xFFFFFF;
        int sepColor = (a << 24) | 0x60FFFFFF;

        int accentRGB = parseAccentColor();

        // === Акцентная полоса слева ===
        ctx.fill(x + 4, y + 4, x + 6, y + h - 4, (a << 24) | accentRGB);

        int curX = x + 10;

        // === Логотип ===
        if (ICON_LOGO != null) {
            ctx.setShaderColor(1f, 1f, 1f, a / 255f);
            ctx.drawTexture(ICON_LOGO, curX, y + (h - 14) / 2, 0, 0, 14, 14, 14, 14);
            ctx.setShaderColor(1f, 1f, 1f, 1f);
        } else {
            ctx.drawTextWithShadow(mc.textRenderer, "M", curX + 4, y + 7, (a << 24) | accentRGB);
        }
        curX += 18 + 6;

        // === Голова игрока ===
        try {
            Identifier skin = mc.player.getSkinTexture();
            ctx.getMatrices().push();
            ctx.getMatrices().translate(curX, y + 4, 0);
            float scale = 14f / 8f;
            ctx.getMatrices().scale(scale, scale, 1f);
            ctx.setShaderColor(1f, 1f, 1f, a / 255f);
            ctx.drawTexture(skin, 0, 0, 8, 8, 8, 8, 64, 64);
            ctx.setShaderColor(1f, 1f, 1f, 1f);
            ctx.getMatrices().pop();
        } catch (Exception ignored) {}
        curX += 18 + 4;

        int textY = y + (h - 8) / 2;

        // === Ник ===
        String nick = mc.getSession().getUsername();
        ctx.drawTextWithShadow(mc.textRenderer, nick, curX, textY, textColor);
        curX += mc.textRenderer.getWidth(nick);

        curX = drawSeparator(ctx, curX, y, sepColor);

        // === FPS ===
        int fps = mc.getCurrentFps();
        int fpsColor = fpsColorFor(fps, a);
        if (ICON_FPS != null) {
            ctx.setShaderColor(1f, 1f, 1f, a / 255f);
            ctx.drawTexture(ICON_FPS, curX, y + (h - 12) / 2, 0, 0, 12, 12, 12, 12);
            ctx.setShaderColor(1f, 1f, 1f, 1f);
            curX += 14;
        }
        String fpsText = fps + " FPS";
        ctx.drawTextWithShadow(mc.textRenderer, fpsText, curX, textY, fpsColor);
        curX += mc.textRenderer.getWidth(fpsText);

        curX = drawSeparator(ctx, curX, y, sepColor);

        // === Ping ===
        int ping = 0;
        if (mc.getNetworkHandler() != null) {
            var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (entry != null) ping = entry.getLatency();
        }
        int pingColor = pingColorFor(ping, a);

        // Кодовая WiFi-иконка
        drawWifiIcon(ctx, curX, y + (h - 12) / 2, ping, a);
        curX += 16;

        String pingText = ping + " ms";
        ctx.drawTextWithShadow(mc.textRenderer, pingText, curX, textY, pingColor);
        curX += mc.textRenderer.getWidth(pingText);

        curX = drawSeparator(ctx, curX, y, sepColor);

        // === Время ===
        if (ICON_TIME != null) {
            ctx.setShaderColor(1f, 1f, 1f, a / 255f);
            ctx.drawTexture(ICON_TIME, curX, y + (h - 12) / 2, 0, 0, 12, 12, 12, 12);
            ctx.setShaderColor(1f, 1f, 1f, 1f);
            curX += 14;
        }
        ctx.drawTextWithShadow(mc.textRenderer, cachedTime, curX, textY, textColor);
    }

    // ============================================================
    // WiFi-иконка из 4 полосок
    // ============================================================
    private void drawWifiIcon(DrawContext ctx, int px, int py, int ping, int alpha) {
        int rgb;
        if (ping < 60) rgb = 0x44FF66;
        else if (ping < 120) rgb = 0xFFDD33;
        else rgb = 0xFF3344;

        int activeBars;
        if (ping < 60) activeBars = 4;
        else if (ping < 120) activeBars = 3;
        else if (ping < 250) activeBars = 2;
        else activeBars = 1;

        int barW = 2;
        int gap = 1;
        int baseY = py + 12;

        int[] heights = { 4, 6, 9, 12 };
        int activeColor = (alpha << 24) | rgb;
        int inactiveColor = ((alpha / 3) << 24) | 0xFFFFFF;

        for (int i = 0; i < 4; i++) {
            int bx = px + i * (barW + gap);
            int by = baseY - heights[i];
            int color = (i < activeBars) ? activeColor : inactiveColor;
            ctx.fill(bx, by, bx + barW, baseY, color);
        }
    }

    private int drawSeparator(DrawContext ctx, int curX, int y, int color) {
        int gap = 8;
        curX += gap;
        ctx.fill(curX, y + h / 2 - 1, curX + 2, y + h / 2 + 1, color);
        curX += 2 + gap;
        return curX;
    }

    private void updateTime() {
        long now = System.currentTimeMillis();
        if (now - lastUpdate > 1000) {
            cachedTime = LocalTime.now().format(TIME_FORMAT);
            lastUpdate = now;
        }
    }

    private static int parseAccentColor() {
        try {
            String hex = ConfigManager.INSTANCE.accentColor.replace("#", "");
            return Integer.parseInt(hex, 16);
        } catch (Exception e) {
            return 0x00D4FF;
        }
    }

    private static int fpsColorFor(int fps, int alpha) {
        int rgb;
        if (fps >= 100) rgb = 0x44FF66;
        else if (fps >= 60) rgb = 0x88FF44;
        else if (fps >= 30) rgb = 0xFFDD33;
        else rgb = 0xFF3344;
        return (alpha << 24) | rgb;
    }

    private static int pingColorFor(int ping, int alpha) {
        int rgb;
        if (ping < 60) rgb = 0x44FF66;
        else if (ping < 120) rgb = 0xFFDD33;
        else rgb = 0xFF3344;
        return (alpha << 24) | rgb;
    }
}