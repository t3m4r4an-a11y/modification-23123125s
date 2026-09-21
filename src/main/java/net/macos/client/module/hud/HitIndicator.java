package net.macos.client.module.hud;

import net.macos.client.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class HitIndicator {

    private static final List<Hit> HITS = new ArrayList<>();

    private static class Hit {
        float angleScreen;
        long startTime;
        Hit(float angleScreen) {
            this.angleScreen = angleScreen;
            this.startTime = System.currentTimeMillis();
        }
    }

    public static void trigger(float relativeYaw) {
        float angleScreen = relativeYaw - 180f;
        HITS.add(new Hit(angleScreen));
    }

    public static void render(DrawContext ctx, float delta) {
        if (!ConfigManager.INSTANCE.enableHitIndicator) {
            HITS.clear();
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        int cx = ctx.getScaledWindowWidth() / 2;
        int cy = ctx.getScaledWindowHeight() / 2;
        int radius = ConfigManager.INSTANCE.hitIndicatorRadius;
        long duration = ConfigManager.INSTANCE.hitIndicatorDuration;

        long now = System.currentTimeMillis();

        Iterator<Hit> it = HITS.iterator();
        while (it.hasNext()) {
            Hit h = it.next();
            long elapsed = now - h.startTime;
            if (elapsed > duration) {
                it.remove();
                continue;
            }

            float t = elapsed / (float) duration;
            float alpha = 1.0f - t;

            // Угловая ширина сужается со временем — острая стрелка
            float arcHalf = 25f - t * 10f;
            // Радиус растёт со временем
            int currentRadius = (int) (radius + t * 20);

            drawArcThick(ctx, cx, cy, currentRadius,
                h.angleScreen - arcHalf, h.angleScreen + arcHalf,
                alpha, 2, t);
        }
    }

    /** Дуга с fade и градиентом — ярче в центре, тусклее к краям */
    private static void drawArcThick(DrawContext ctx, int cx, int cy, int radius,
                                      float fromAngle, float toAngle,
                                      float alpha, int thickness, float progress) {
        float step = 2f;
        float centerAngle = (fromAngle + toAngle) / 2f;
        float halfSpan = (toAngle - fromAngle) / 2f;

        for (float ang = fromAngle; ang <= toAngle; ang += step) {
            // Fade к краям дуги
            float distFromCenter = Math.abs(ang - centerAngle);
            float edgeFade = 1f - (distFromCenter / halfSpan);
            edgeFade = Math.max(0.2f, edgeFade);

            int a = (int) (alpha * edgeFade * 255);
            if (a <= 4) continue;

            // Красный с лёгким сдвигом в оранжевый к краям
            int r = 255;
            int g = (int) (60 + (edgeFade * 40));
            int b = 60;
            int color = (a << 24) | (r << 16) | (g << 8) | b;

            float rad = (float) Math.toRadians(ang);
            int px = cx + (int) (Math.cos(rad) * radius);
            int py = cy + (int) (Math.sin(rad) * radius);
            ctx.fill(px, py, px + thickness, py + thickness, color);
        }
    }

    public static void clear() {
        HITS.clear();
    }
}