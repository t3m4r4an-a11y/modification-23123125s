package net.macos.client.module.hud;

import net.macos.client.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CustomTabList {

    private static final int HEAD_SIZE = 16;
    private static final int ROW_HEIGHT = 20;
    private static final int COL_WIDTH = 140;
    private static final int PADDING = 8;

    public static void render(DrawContext context, int screenWidth, Scoreboard scoreboard,
                              ScoreboardObjective objective) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        List<PlayerListEntry> players = new ArrayList<>(mc.getNetworkHandler().getPlayerList());
        if (players.isEmpty()) return;

        // Сортировка: сначала по спектаторам, потом по имени
        players.sort(Comparator
            .comparing((PlayerListEntry p) -> p.getGameMode() == GameMode.SPECTATOR)
            .thenComparing(p -> p.getProfile().getName(), String.CASE_INSENSITIVE_ORDER));

        // Раскладка: разбиваем на колонки
        int total = players.size();
        int maxRows = 20;
        int rows = Math.min(maxRows, total);
        int cols = (int)Math.ceil(total / (double)maxRows);

        int panelW = cols * COL_WIDTH + PADDING * 2;
        int panelH = rows * ROW_HEIGHT + PADDING * 2 + 16; // 16 — заголовок

        int panelX = (screenWidth - panelW) / 2;
        int panelY = 6;

        // Фон панели
        int bg = 0xC014141F;
        drawRounded(context, panelX, panelY, panelW, panelH, 6, bg);

        // Верхняя accent-полоска
        Color accent = parseColor(ConfigManager.INSTANCE.accentColor);
        int accentRGB = (0xFF << 24) | (accent.getRGB() & 0xFFFFFF);
        context.fill(panelX, panelY, panelX + panelW, panelY + 1, accentRGB);

        // Заголовок — количество игроков
        String header = "Players: " + total;
        int headerW = mc.textRenderer.getWidth(header);
        context.drawTextWithShadow(mc.textRenderer, header,
            panelX + (panelW - headerW) / 2,
            panelY + 4, 0xFFFFFFFF);

        // Разделитель под заголовком
        context.fill(panelX + 6, panelY + 15, panelX + panelW - 6, panelY + 16, 0x40FFFFFF);

        // Рисуем игроков построчно
        int startY = panelY + 16 + PADDING / 2;
        for (int i = 0; i < total; i++) {
            int col = i / maxRows;
            int row = i % maxRows;

            int px = panelX + PADDING + col * COL_WIDTH;
            int py = startY + row * ROW_HEIGHT;

            PlayerListEntry entry = players.get(i);
            drawPlayerRow(context, mc, entry, px, py, scoreboard, objective);
        }
    }

    private static void drawPlayerRow(DrawContext context, MinecraftClient mc, PlayerListEntry entry,
                                      int x, int y, Scoreboard scoreboard, ScoreboardObjective objective) {
        // === Голова ===
        Identifier skin = entry.getSkinTexture();
        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        float scale = HEAD_SIZE / 8f;
        context.getMatrices().scale(scale, scale, 1f);
        context.drawTexture(skin, 0, 0, 8, 8, 8, 8, 64, 64);
        context.getMatrices().pop();

        // === Имя ===
        Text displayName = entry.getDisplayName();
        String name = displayName != null ? displayName.getString() : entry.getProfile().getName();

        // Цвет имени из команды
        int nameColor = 0xFFFFFFFF;
        Team team = scoreboard.getPlayerTeam(entry.getProfile().getName());
        if (team != null) {
            Integer color = team.getColor().getColorValue();
            if (color != null) nameColor = 0xFF000000 | color;
        }

        // Спектатор — полупрозрачный
        if (entry.getGameMode() == GameMode.SPECTATOR) {
            nameColor = (nameColor & 0x00FFFFFF) | 0x80 << 24;
        }

        // Обрезаем если длинное
        int maxNameW = COL_WIDTH - HEAD_SIZE - 40;
        if (mc.textRenderer.getWidth(name) > maxNameW) {
            while (mc.textRenderer.getWidth(name + "...") > maxNameW && name.length() > 3) {
                name = name.substring(0, name.length() - 1);
            }
            name = name + "...";
        }

        context.drawTextWithShadow(mc.textRenderer, name,
            x + HEAD_SIZE + 4, y + 4, nameColor);

        // === Пинг (полоски как в ваниле) ===
        int pingX = x + COL_WIDTH - 20;
        int pingY = y + 6;
        drawPing(context, pingX, pingY, entry.getLatency());
    }

    private static void drawPing(DrawContext context, int x, int y, int latency) {
        int bars;
        int color;
        if (latency < 0) {
            bars = 5;
            color = 0xFF5A5A5A; // серый
        } else if (latency < 150) {
            bars = 5;
            color = 0xFF55FF55; // зелёный
        } else if (latency < 300) {
            bars = 4;
            color = 0xFF55FF55;
        } else if (latency < 600) {
            bars = 3;
            color = 0xFFFFAA00; // жёлтый
        } else if (latency < 1000) {
            bars = 2;
            color = 0xFFFF5555; // красный
        } else {
            bars = 1;
            color = 0xFFFF5555;
        }

        for (int i = 0; i < 5; i++) {
            int barH = (i + 1) * 2;
            int barY = y + 10 - barH;
            int barColor = (i < bars) ? color : 0x40FFFFFF;
            context.fill(x + i * 3, barY, x + i * 3 + 2, y + 10, barColor);
        }
    }

    private static Color parseColor(String hex) {
        try {
            return Color.decode(hex);
        } catch (Exception e) {
            return new Color(0, 212, 255);
        }
    }

    private static void drawRounded(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        r = Math.min(r, Math.min(w / 2, h / 2));
        ctx.fill(x + r, y, x + w - r, y + h, color);
        ctx.fill(x, y + r, x + r, y + h - r, color);
        ctx.fill(x + w - r, y + r, x + w, y + h - r, color);

        for (int i = 0; i < r; i++) {
            for (int j = 0; j < r; j++) {
                float dx = r - i - 0.5f;
                float dy = r - j - 0.5f;
                if (dx * dx + dy * dy <= r * r) {
                    ctx.fill(x + i, y + j, x + i + 1, y + j + 1, color);
                    ctx.fill(x + w - 1 - i, y + j, x + w - i, y + j + 1, color);
                    ctx.fill(x + i, y + h - 1 - j, x + i + 1, y + h - j, color);
                    ctx.fill(x + w - 1 - i, y + h - 1 - j, x + w - i, y + h - j, color);
                }
            }
        }
    }
}