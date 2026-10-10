package net.macos.client.module.hud;

import net.macos.client.config.ConfigManager;
import net.macos.client.gui.font.AetherionFont;
import net.macos.client.hud.glass.GlassRenderer;
import net.macos.client.render.SquircleRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardPlayerScore;
import net.minecraft.scoreboard.Team;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 2026 Luxury Liquid Glass Scoreboard HUD.
 * Features ultra-smooth squircle backdrop, gradient accent rim, and clean typography.
 */
public class CustomScoreboard {

    public static void render(DrawContext context, ScoreboardObjective objective) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        Scoreboard scoreboard = mc.world.getScoreboard();
        if (scoreboard == null) return;

        List<ScoreboardPlayerScore> scores = new ArrayList<>(scoreboard.getAllPlayerScores(objective));
        scores.removeIf(s -> s.getPlayerName() == null || s.getPlayerName().startsWith("#"));
        scores.sort(Comparator.comparingInt(ScoreboardPlayerScore::getScore).reversed());

        if (scores.size() > 15) {
            scores = scores.subList(0, 15);
        }
        if (scores.isEmpty()) return;

        int lineHeight = 12;
        int padding = 8;
        int headerHeight = 18;

        String title = objective.getDisplayName().getString();
        int textW = AetherionFont.width(title);
        for (ScoreboardPlayerScore s : scores) {
            String name = getEntryName(scoreboard, s);
            int w = AetherionFont.width(name) + 24 + AetherionFont.width(String.valueOf(s.getScore()));
            if (w > textW) textW = w;
        }
        int width = Math.max(90, textW + padding * 2);
        int height = headerHeight + scores.size() * lineHeight + padding;

        int screenW = context.getScaledWindowWidth();
        int screenH = context.getScaledWindowHeight();
        int rightX = screenW - width - 6;
        int y = (screenH - height) / 2;

        int accent = parseAccent();

        // 1. Ambient drop shadow
        GlassRenderer.dropShadow(context, rightX, y, width, height, 14, 8, 0x55);

        // 2. Liquid squircle glass background
        SquircleRenderer.fill(context, rightX, y, width, height, 9, 0x85111624);

        // 3. Specular top highlight & accent border
        GlassRenderer.specular(context, rightX, y, width, height, 9, 0x25FFFFFF);
        SquircleRenderer.border(context, rightX, y, width, height, 9, 1.0f, (0x30 << 24) | (accent & 0xFFFFFF));

        // 4. Accent line above title
        SquircleRenderer.pill(context, rightX + (width - 24) / 2f, y + 2, 24, 2, (0xD0 << 24) | accent);

        // 5. Title
        AetherionFont.draw(context, title,
                rightX + (width - AetherionFont.width(title)) / 2,
                y + 6, 0xFFFFFFFF);

        // Divider
        context.fill(rightX + 6, y + headerHeight - 2, rightX + width - 6, y + headerHeight - 1, 0x20FFFFFF);

        // 6. Player rows
        int lineY = y + headerHeight + 2;
        for (ScoreboardPlayerScore s : scores) {
            String name = getEntryName(scoreboard, s);
            String scoreStr = String.valueOf(s.getScore());

            AetherionFont.draw(context, name, rightX + padding, lineY + 1, 0xFFE0E0E6);

            // Score pill badge
            int scoreW = AetherionFont.width(scoreStr) + 8;
            int scoreX = rightX + width - padding - scoreW;
            SquircleRenderer.pill(context, scoreX, lineY, scoreW, 10, (0x35 << 24) | accent);
            AetherionFont.draw(context, scoreStr, scoreX + 4, lineY + 1, (0xFF << 24) | accent);

            lineY += lineHeight;
        }

        context.draw();
    }

    private static String getEntryName(Scoreboard scoreboard, ScoreboardPlayerScore s) {
        Team team = scoreboard.getPlayerTeam(s.getPlayerName());
        if (team != null) {
            return Team.decorateName(team, net.minecraft.text.Text.literal(s.getPlayerName())).getString();
        }
        return s.getPlayerName();
    }

    private static int parseAccent() {
        try {
            return Integer.parseInt(ConfigManager.INSTANCE.accentColor.replace("#", ""), 16);
        } catch (Exception e) {
            return 0x00D4FF;
        }
    }
}