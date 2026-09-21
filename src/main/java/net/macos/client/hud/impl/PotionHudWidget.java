package net.macos.client.hud.impl;

import net.macos.client.MacClient;
import net.macos.client.config.ConfigManager;
import net.macos.client.hud.glass.GlassWidget;
import net.macos.client.hud.glass.PanelStyle;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.StatusEffectSpriteManager;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PotionHudWidget extends GlassWidget {

    private static final int ICON_SIZE = 24;    // размер иконки на экране
    private static final int ROW_GAP = 6;       // отступ между строками
    private static final int RING_RADIUS = 14;  // радиус кольца прогресса
    private static final int RING_THICK = 2;    // толщина кольца
    private static final int MAX_DURATION_TICKS = 1200; // 60 сек = полное кольцо

    public PotionHudWidget() {
        super("potionHud");
        this.appearSpeed = 3f;
    }

    @Override
    protected void configureStyle(PanelStyle s) {
        s.radius = 6;
        s.bgColor = 0x00000000;
        s.borderColor = 0x00000000;
        s.topAccent = false;
        s.gradientTop = 0;
        s.gradientBot = 0;
        s.glowLayers = 0;
    }

    @Override
    protected void measure(float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            this.w = 0;
            this.h = 0;
            return;
        }

        var effects = mc.player.getStatusEffects();
        if (effects.isEmpty()) {
            this.w = 0;
            this.h = 0;
            return;
        }

        // Ширина: круг (2*RING_RADIUS) + gap + самая длинная строка
        int maxTextW = 0;
        for (StatusEffectInstance eff : effects) {
            int nameW = mc.textRenderer.getWidth(eff.getEffectType().getName().getString());
            int timeW = mc.textRenderer.getWidth(formatTime(eff.getDuration()));
            maxTextW = Math.max(maxTextW, Math.max(nameW, timeW));
        }

        this.w = RING_RADIUS * 2 + 8 + maxTextW + 4;
        this.h = effects.size() * (RING_RADIUS * 2 + ROW_GAP) + 2;
    }

    @Override
    protected boolean shouldBeVisible() {
        if (MacClient.hudEditorOpen) return true;
        if (!ConfigManager.INSTANCE.getWidget("potionHud").enabled) return false;
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc.player != null && !mc.player.getStatusEffects().isEmpty();
    }

    @Override
    protected void renderInner(DrawContext ctx, int mouseX, int mouseY, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        var effects = mc.player.getStatusEffects();
        if (effects.isEmpty()) return;

        List<StatusEffectInstance> sorted = new ArrayList<>(effects);
        sorted.sort(Comparator.comparingInt(StatusEffectInstance::getDuration).reversed());

        StatusEffectSpriteManager spriteManager = mc.getStatusEffectSpriteManager();

        int a = (int) (appearProgress * 255);
        int curY = y + 2;

        for (StatusEffectInstance effect : sorted) {
            StatusEffect type = effect.getEffectType();
            int color = type.getColor() | 0xFF000000;

            int cx = x + RING_RADIUS;
            int cy = curY + RING_RADIUS;

            // 1. Фон ячейки (круг)
            drawCellBg(ctx, cx, cy, RING_RADIUS, a);

            // 2. Иконка эффекта — по центру
            try {
                Sprite sprite = spriteManager.getSprite(type);
                ctx.drawSprite(cx - ICON_SIZE / 2, cy - ICON_SIZE / 2, 0,
                    ICON_SIZE, ICON_SIZE, sprite);
            } catch (Exception ignored) {}

            // 3. Кольцо прогресса — ВОКРУГ иконки, деплеит по часовой
            drawProgressRing(ctx, cx, cy, RING_RADIUS, effect, (a << 24) | (color & 0xFFFFFF));

            // 4. Уровень — маленький бейдж в правом нижнем углу
            int amp = effect.getAmplifier();
            if (amp > 0) {
                String level = String.valueOf(amp + 1);
                int lw = mc.textRenderer.getWidth(level);
                int bx = cx + RING_RADIUS - 8;
                int by = cy + RING_RADIUS - 9;
                // Тёмный фон бейджа
                ctx.fill(bx - 2, by - 1, bx + lw + 2, by + 9, (a << 24) | 0x000000);
                ctx.drawTextWithShadow(mc.textRenderer, level, bx, by, (a << 24) | 0xFFFFFF);
            }

            // 5. Имя эффекта (справа сверху)
            String name = type.getName().getString();
            int textX = cx + RING_RADIUS + 6;
            ctx.drawTextWithShadow(mc.textRenderer, name, textX, curY + 4, (a << 24) | 0xFFFFFF);

            // 6. Время (справа снизу)
            String time = effect.isInfinite() ? "∞" : formatTime(effect.getDuration());
            ctx.drawTextWithShadow(mc.textRenderer, time, textX, curY + 16,
                (a << 24) | 0xB0FFFFFF);

            curY += RING_RADIUS * 2 + ROW_GAP;
        }
    }

    // ============================================================
    // ХЕЛПЕРЫ
    // ============================================================

    /** Круглый фон ячейки */
    private void drawCellBg(DrawContext ctx, int cx, int cy, int radius, int alpha) {
        int a = (int) (alpha * 0.6);
        int color = (a << 24) | 0x14141F;
        // Рисуем горизонтальными линиями: одна fill на строку вместо 28
        for (int yOff = -radius; yOff <= radius; yOff++) {
            int xSpan = (int) Math.sqrt(radius * radius - yOff * yOff);
            ctx.fill(cx - xSpan, cy + yOff, cx + xSpan + 1, cy + yOff + 1, color);
        }
    }

    /** Кольцо прогресса: старт сверху (-90°), деплеит по часовой */
    private void drawProgressRing(DrawContext ctx, int cx, int cy, int radius,
                                   StatusEffectInstance effect, int color) {
        float progress;
        if (effect.isInfinite()) {
            progress = 1.0f;
        } else {
            progress = Math.min(1.0f, effect.getDuration() / (float) MAX_DURATION_TICKS);
        }

        // Активное кольцо — от 12 часов по часовой
        drawRingArc(ctx, cx, cy, radius, 0f, progress, RING_THICK, color);
    }

    /** Рисует дугу кольца от startT до endT (0..1), 0 = верх */
    private void drawRingArc(DrawContext ctx, int cx, int cy, int radius,
                              float startT, float endT, int thickness, int color) {
        if (endT <= startT) return;

        // 0 → 12 часов (-90°), идём по часовой
        float startAngle = -90f + (360f * startT);
        float endAngle = -90f + (360f * endT);
        float step = 6f;

        for (float angle = startAngle; angle < endAngle; angle += step) {
            float rad = (float) Math.toRadians(angle);
            float fx = cx + (float) (Math.cos(rad) * radius);
            float fy = cy + (float) (Math.sin(rad) * radius);
            int px = (int) fx;
            int py = (int) fy;

            // 2×2 пикселя на точку вместо 4×4
            ctx.fill(px - 1, py - 1, px + 1, py + 1, color);
        }
    }

    private static String formatTime(int ticks) {
        int totalSeconds = ticks / 20;
        return String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60);
    }
}