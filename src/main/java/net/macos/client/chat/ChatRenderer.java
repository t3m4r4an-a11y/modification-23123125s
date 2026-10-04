package net.macos.client.chat;

import net.macos.client.gui.font.AetherionFont;
import net.macos.client.hud.glass.GlassRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.text.OrderedText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatRenderer {

    // ============================================================
    // LAYOUT
    // ============================================================

    private static final int PADDING_X    = 12;
    private static final int PADDING_Y    = 10;
    private static final int LINE_HEIGHT  = 12;
    private static final int RADIUS       = 10;
    private static final int LEFT_MARGIN  = 8;

    private static final int CLOSED_MAX_LINES     = 10;
    private static final int OPEN_MAX_LINES       = 20;
    private static final int CLOSED_BOTTOM_MARGIN = 60;
    private static final int OPEN_BOTTOM_MARGIN   = 48;

    // ============================================================
    // COLORS
    // ============================================================

    private static final int C_PANEL_BG     = 0xA8121820;
    private static final int C_PANEL_BORDER = 0x18FFFFFF;
    private static final int C_SPECULAR     = 0x1AFFFFFF;

    // ============================================================
    // RENDER
    // ============================================================

    public static void renderFromHud(DrawContext ctx, List<ChatHudLine.Visible> messages,
                                     int scrolledLines, int lineHeight, int currentTick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;
        if (messages.isEmpty()) return;

        boolean chatOpen = mc.currentScreen instanceof ChatScreen;

        // --- Собираем пул сообщений (новые — первыми, как в vanilla) ---
        List<ChatHudLine.Visible> pool = new ArrayList<>();
        for (ChatHudLine.Visible line : messages) {
            int age = currentTick - line.addedTime();
            // Когда чат закрыт — фильтруем по возрасту (fade zone)
            // Когда открыт — показываем всё
            if (!chatOpen && age > 200) break;
            pool.add(line);
        }
        if (pool.isEmpty()) return;

        // --- Нарезка видимого окна ---
        int maxLines = chatOpen ? OPEN_MAX_LINES : CLOSED_MAX_LINES;
        int start = chatOpen
                ? Math.max(0, Math.min(scrolledLines, pool.size() - 1))
                : 0;
        int end = Math.min(pool.size(), start + maxLines);

        List<ChatHudLine.Visible> visible = new ArrayList<>(pool.subList(start, end));
        Collections.reverse(visible); // oldest at top, newest at bottom

        // --- Измеряем ширину ---
        int longestText = 0;
        for (ChatHudLine.Visible line : visible) {
            int w = AetherionFont.width(line.content());
            if (w > longestText) longestText = w;
        }

        int boxW = longestText + PADDING_X * 2;
        int boxH = visible.size() * LINE_HEIGHT + PADDING_Y * 2;

        int screenH = ctx.getScaledWindowHeight();
        int bottomMargin = chatOpen ? OPEN_BOTTOM_MARGIN : CLOSED_BOTTOM_MARGIN;
        int boxX = LEFT_MARGIN;
        int boxY = screenH - bottomMargin - boxH;

        // --- Панель ---
        int panelAlpha = chatOpen ? 0xD0 : 0xA8;
        int panelBg = (panelAlpha << 24) | (C_PANEL_BG & 0xFFFFFF);
        GlassRenderer.roundedRect(ctx, boxX, boxY, boxW, boxH, RADIUS, panelBg);
        GlassRenderer.specular(ctx, boxX, boxY, boxW, boxH, RADIUS, C_SPECULAR);
        drawBorderRounded(ctx, boxX, boxY, boxW, boxH, RADIUS, C_PANEL_BORDER);

        // --- Строки ---
        int textX = boxX + PADDING_X;
        int textY = boxY + PADDING_Y;

        for (int i = 0; i < visible.size(); i++) {
            ChatHudLine.Visible line = visible.get(i);
            int age = currentTick - line.addedTime();

            // Fade только когда чат закрыт
            float alpha = 1f;
            if (!chatOpen && age > 160) {
                alpha = Math.max(0f, (200 - age) / 40f);
            }

            // Самое новое сообщение — чуть ярче (только когда чат закрыт)
            boolean isNewest = !chatOpen && (i == visible.size() - 1);
            if (isNewest) alpha = Math.min(1f, alpha * 1.15f);

            int a = (int) (alpha * 255);
            if (a < 4) {
                textY += LINE_HEIGHT;
                continue;
            }

            int baseColor = isNewest ? 0xFFFFFF : 0xE8E8EE;
            int color = (a << 24) | baseColor;

            AetherionFont.draw(ctx, line.content(), textX, textY, color);
            textY += LINE_HEIGHT;
        }

        // --- Индикатор скролла ---
        if (chatOpen && scrolledLines > 0) {
            String scrollHint = "↑ " + scrolledLines;
            int hintW = AetherionFont.width(scrollHint);
            AetherionFont.draw(ctx, scrollHint,
                    boxX + boxW - hintW - PADDING_X, boxY + 4, 0x80FFFFFF);
        }
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private static void drawBorderRounded(DrawContext ctx, int x, int y, int w, int h,
                                          int radius, int color) {
        int a = (color >>> 24) & 0xFF;
        if (a <= 0) return;
        ctx.fill(x + radius, y, x + w - radius, y + 1, color);
        ctx.fill(x + radius, y + h - 1, x + w - radius, y + h, color);
        ctx.fill(x, y + radius, x + 1, y + h - radius, color);
        ctx.fill(x + w - 1, y + radius, x + w, y + h - radius, color);

        for (int i = 0; i < radius; i++) {
            float dx = radius - i - 0.5f;
            for (int j = 0; j < radius; j++) {
                float dy = radius - j - 0.5f;
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                if (d >= radius - 1.5f && d <= radius + 0.5f) {
                    ctx.fill(x + i, y + j, x + i + 1, y + j + 1, color);
                    ctx.fill(x + w - 1 - i, y + j, x + w - i, y + j + 1, color);
                    ctx.fill(x + i, y + h - 1 - j, x + i + 1, y + h - j, color);
                    ctx.fill(x + w - 1 - i, y + h - 1 - j, x + w - i, y + h - j, color);
                }
            }
        }
    }
}