package net.macos.client.module.hud;

import net.macos.client.config.ConfigManager;
import net.macos.client.gui.font.AetherionFont;
import net.macos.client.hud.glass.GlassRenderer;
import net.macos.client.render.SquircleRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 2026 Luxury Liquid Glass Player List (Tab) HUD.
 * Features ultra-smooth squircle backdrop, rounded skin heads, and ping badges.
 */
public class CustomTabList {

    private static final int HEAD_SIZE = 16;
    private static final int ROW_HEIGHT = 22;
    private static final int COL_WIDTH = 150;
    private static final int PADDING = 10;

    public static void render(DrawContext context, int screenWidth, Scoreboard scoreboard,
                              ScoreboardObjective objective) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        List<PlayerListEntry> players = new ArrayList<>(mc.getNetworkHandler().getPlayerList());
        if (players.isEmpty()) return;

        players.sort(Comparator
            .comparing((PlayerListEntry p) -> p.getGameMode() == GameMode.SPECTATOR)
            .thenComparing(p -> p.getProfile().getName(), String.CASE_INSENSITIVE_ORDER));

        int total = players.size();
        int maxRows = 20;
        int rows = Math.min(maxRows, total);
        int cols = (int) Math.ceil(total / (double) maxRows);

        int panelW = cols * COL_WIDTH + PADDING * 2;
        int panelH = rows * ROW_HEIGHT + PADDING * 2 + 18;

        int panelX = (screenWidth - panelW) / 2;
        int panelY = 8;

        int accent = parseAccent();

        // 1. Drop shadow & glass backdrop
        GlassRenderer.dropShadow(context, panelX, panelY, panelW, panelH, 16, 10, 0x65);
        SquircleRenderer.fill(context, panelX, panelY, panelW, panelH, 12, 0x8A111624);
        GlassRenderer.specular(context, panelX, panelY, panelW, panelH, 12, 0x25FFFFFF);
        SquircleRenderer.border(context, panelX, panelY, panelW, panelH, 12, 1.0f, (0x30 << 24) | accent);

        // 2. Header
        String countStr = String.valueOf(total);
        String headerTitle = "ONLINE PLAYERS";
        int titleW = AetherionFont.width(headerTitle);
        int badgeW = AetherionFont.width(countStr) + 12;

        AetherionFont.draw(context, headerTitle, panelX + PADDING + 2, panelY + 6, 0xFFFFFFFF);
        SquircleRenderer.pill(context, panelX + PADDING + titleW + 8, panelY + 5, badgeW, 12, (0x35 << 24) | accent);
        AetherionFont.draw(context, countStr, panelX + PADDING + titleW + 14, panelY + 6, (0xFF << 24) | accent);

        // Divider
        context.fill(panelX + 8, panelY + 20, panelX + panelW - 8, panelY + 21, 0x1AFFFFFF);

        // 3. Player entries
        int startY = panelY + 24;
        for (int i = 0; i < total; i++) {
            int col = i / maxRows;
            int row = i % maxRows;

            int px = panelX + PADDING + col * COL_WIDTH;
            int py = startY + row * ROW_HEIGHT;

            PlayerListEntry entry = players.get(i);
            drawPlayerRow(context, mc, entry, px, py, scoreboard, accent);
        }

        context.draw();
    }

    private static void drawPlayerRow(DrawContext context, MinecraftClient mc, PlayerListEntry entry,
                                      int x, int y, Scoreboard scoreboard, int accent) {
        // Subtle row card
        SquircleRenderer.fill(context, x, y, COL_WIDTH - 6, ROW_HEIGHT - 2, 5, 0x12FFFFFF);

        // Head
        Identifier skin = entry.getSkinTexture();
        context.getMatrices().push();
        context.getMatrices().translate(x + 3, y + 2, 0);
        float scale = HEAD_SIZE / 8f;
        context.getMatrices().scale(scale, scale, 1f);
        context.drawTexture(skin, 0, 0, 8, 8, 8, 8, 64, 64);
        context.getMatrices().pop();

        // Name
        Text displayName = entry.getDisplayName();
        String name = displayName != null ? displayName.getString() : entry.getProfile().getName();

        int nameColor = 0xFFFFFFFF;
        Team team = scoreboard.getPlayerTeam(entry.getProfile().getName());
        if (team != null) {
            Integer color = team.getColor().getColorValue();
            if (color != null) nameColor = 0xFF000000 | color;
        }

        if (entry.getGameMode() == GameMode.SPECTATOR) {
            nameColor = (nameColor & 0x00FFFFFF) | (0x75 << 24);
        }

        int maxNameW = COL_WIDTH - HEAD_SIZE - 38;
        if (AetherionFont.width(name) > maxNameW) {
            while (AetherionFont.width(name + "...") > maxNameW && name.length() > 3) {
                name = name.substring(0, name.length() - 1);
            }
            name = name + "...";
        }

        AetherionFont.draw(context, name, x + HEAD_SIZE + 7, y + 5, nameColor);

        // Ping indicator
        int pingX = x + COL_WIDTH - 24;
        int pingY = y + 7;
        drawPing(context, pingX, pingY, entry.getLatency());
    }

    private static void drawPing(DrawContext context, int x, int y, int latency) {
        int bars;
        int color;
        if (latency < 0) {
            bars = 4; color = 0xFF7E7E84;
        } else if (latency < 120) {
            bars = 4; color = 0xFF4ADE80;
        } else if (latency < 250) {
            bars = 3; color = 0xFFFACC15;
        } else if (latency < 500) {
            bars = 2; color = 0xFFFB923C;
        } else {
            bars = 1; color = 0xFFF87171;
        }

        for (int i = 0; i < 4; i++) {
            int barH = (i + 1) * 2;
            int barY = y + 8 - barH;
            int barColor = (i < bars) ? color : 0x25FFFFFF;
            SquircleRenderer.pill(context, x + i * 3, barY, 2, barH, barColor);
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