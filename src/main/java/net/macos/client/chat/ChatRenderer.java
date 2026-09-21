package net.macos.client.chat;

import net.macos.client.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.OrderedText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatRenderer {

    private static final int PADDING_X = 6;
    private static final int PADDING_Y = 4;
    private static final int LINE_HEIGHT = 10;
    private static final int BOTTOM_MARGIN = 50;
    private static final int LEFT_MARGIN = 6;
    private static final int MAX_LINES = 8;
    private static final int RADIUS = 6;

    public static void renderFromHud(DrawContext ctx, List<ChatHudLine.Visible> messages,
                                      int scrolledLines, int lineHeight, int currentTick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;
        if (messages.isEmpty()) return;

        // === ВАЖНО: фильтруем по возрасту ===
        // Vanilla: сообщение держится 200 тиков (10 сек) с фейдом в последние 40
        List<ChatHudLine.Visible> alive = new ArrayList<>();
        for (ChatHudLine.Visible line : messages) {
            int age = currentTick - line.addedTime();
            if (age > 100) break;   // сообщения идут в порядке: новые первыми, значит дальше только старее
            alive.add(line);
        }
        if (alive.isEmpty()) return;

        int total = Math.min(alive.size(), MAX_LINES);
        List<ChatHudLine.Visible> visible = new ArrayList<>(alive.subList(0, total));
        Collections.reverse(visible);

        // Считаем общий размер бокса
        int longestText = 0;
        for (ChatHudLine.Visible line : visible) {
            int w = mc.textRenderer.getWidth(line.content());
            if (w > longestText) longestText = w;
        }

        int boxW = longestText + PADDING_X * 2;
        int boxH = visible.size() * LINE_HEIGHT + PADDING_Y * 2;

        int screenH = ctx.getScaledWindowHeight();
        int boxX = LEFT_MARGIN;
        int boxY = screenH - BOTTOM_MARGIN - boxH;

        drawPanel(ctx, boxX, boxY, boxW, boxH, RADIUS);

        int accent = parseAccent();
        ctx.fill(boxX, boxY + 3, boxX + 2, boxY + boxH - 3, 0xFF000000 | accent);

        // === Текст построчно с fade ===
        int textX = boxX + PADDING_X + 2;
        int textY = boxY + PADDING_Y;

        for (ChatHudLine.Visible line : visible) {
            OrderedText content = line.content();
            int age = currentTick - line.addedTime();

            // Fade: полностью видно до 160 тиков, потом плавно исчезает к 200
            float alpha = 1f;
            if (age > 60) {
                alpha = Math.max(0f, (100 - age) / 40f);
            }
            
            int a = (int) (alpha * 255);
            if (a < 4) { textY += LINE_HEIGHT; continue; }

            int color = (a << 24) | 0xFFFFFF;
            ctx.drawTextWithShadow(mc.textRenderer, content, textX, textY, color);

            textY += LINE_HEIGHT;
        }
    }

    /** Тёмная скруглённая плашка (заменяет blur) */
    private static void drawPanel(DrawContext ctx, int x, int y, int w, int h, int r) {
        int bg = 0xB014141F;   // 0xB0 = ~69% alpha
        roundedRect(ctx, x, y, w, h, r, bg);
    }

    private static void roundedRect(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0) return;
        r = Math.min(r, Math.min(w / 2, h / 2));
        ctx.fill(x + r, y, x + w - r, y + h, color);
        ctx.fill(x, y + r, x + r, y + h - r, color);
        ctx.fill(x + w - r, y + r, x + w, y + h - r, color);

        int baseA = (color >>> 24) & 0xFF;
        int rgb = color & 0xFFFFFF;
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < r; j++) {
                float dx = r - i - 0.5f;
                float dy = r - j - 0.5f;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                float cov = r - dist + 0.5f;
                if (cov <= 0f) continue;
                if (cov > 1f) cov = 1f;
                int ca = (int)(baseA * cov);
                if (ca <= 0) continue;
                int c = (ca << 24) | rgb;
                ctx.fill(x + i, y + j, x + i + 1, y + j + 1, c);
                ctx.fill(x + w - 1 - i, y + j, x + w - i, y + j + 1, c);
                ctx.fill(x + i, y + h - 1 - j, x + i + 1, y + h - j, c);
                ctx.fill(x + w - 1 - i, y + h - 1 - j, x + w - i, y + h - j, c);
            }
        }
    }

    private static int parseAccent() {
        try {
            return Integer.parseInt(ConfigManager.INSTANCE.accentColor.replace("#", ""), 16);
        } catch (Exception e) {
            return 0x00D4FF;
        }
    }
}