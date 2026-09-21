package net.macos.client.module.hud;

import net.macos.client.config.ConfigManager;
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

public class CustomScoreboard {

    public static void render(DrawContext context, ScoreboardObjective objective) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        Scoreboard scoreboard = mc.world.getScoreboard();
        if (scoreboard == null) return;

        // Собираем все score entries
        List<ScoreboardPlayerScore> scores = new ArrayList<>(scoreboard.getAllPlayerScores(objective));
        scores.removeIf(s -> s.getPlayerName() == null || s.getPlayerName().startsWith("#"));

        // Сортируем по убыванию очков
        scores.sort(Comparator.comparingInt(ScoreboardPlayerScore::getScore).reversed());

        // Ограничиваем до 15 строк
        if (scores.size() > 15) {
            scores = scores.subList(0, 15);
        }

        if (scores.isEmpty()) return;

        int lineHeight = 10;
        int padding = 6;
        int headerHeight = 14;
        int width = 120;

        // Заголовок
        String title = objective.getDisplayName().getString();

        // Считаем ширину — максимальная строка + число
        int textW = mc.textRenderer.getWidth(title);
        for (ScoreboardPlayerScore s : scores) {
            String name = getEntryName(scoreboard, s);
            int w = mc.textRenderer.getWidth(name) + 20 + mc.textRenderer.getWidth(String.valueOf(s.getScore()));
            if (w > textW) textW = w;
        }
        width = Math.max(80, textW + padding * 2);

        int height = headerHeight + scores.size() * lineHeight + padding;

        int screenW = context.getScaledWindowWidth();
        int screenH = context.getScaledWindowHeight();
        int rightX = screenW - width - 4;
        int y = (screenH - height) / 2;  // центр по вертикали

        Color accent = parseColor(ConfigManager.INSTANCE.accentColor);
        int bgColor = 0xC014141F;
        int borderColor = (0xFF << 24) | (accent.getRGB() & 0xFFFFFF);

        // Фон панели
        drawRounded(context, rightX, y, width, height, 6, bgColor);

        // Заголовок — accent полоска сверху
        context.fill(rightX, y, rightX + width, y + 1, borderColor);
        context.drawTextWithShadow(mc.textRenderer, title,
            rightX + (width - mc.textRenderer.getWidth(title)) / 2,
            y + 4, 0xFFFFFFFF);

        // Разделитель под заголовком
        context.fill(rightX + 4, y + headerHeight - 1, rightX + width - 4, y + headerHeight, 0x40FFFFFF);

        // Строки
        int lineY = y + headerHeight + 2;
        for (ScoreboardPlayerScore s : scores) {
            String name = getEntryName(scoreboard, s);
            String scoreStr = String.valueOf(s.getScore());

            context.drawTextWithShadow(mc.textRenderer, name, rightX + padding, lineY, 0xFFE0E0E0);
            context.drawTextWithShadow(mc.textRenderer, scoreStr,
                rightX + width - padding - mc.textRenderer.getWidth(scoreStr),
                lineY, 0xFFFFAA00);
            lineY += lineHeight;
        }
    }

    private static String getEntryName(Scoreboard scoreboard, ScoreboardPlayerScore s) {
        Team team = scoreboard.getPlayerTeam(s.getPlayerName());
        if (team != null) {
            return Team.decorateName(team, net.minecraft.text.Text.literal(s.getPlayerName())).getString();
        }
        return s.getPlayerName();
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