package net.macos.client.module.hud;

import net.macos.client.config.ConfigManager;
import net.macos.client.render.SquircleRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 2026 Modern Tactical Damage Hit Indicator.
 * Renders smooth floating directional damage arcs pointing towards incoming attackers in world space.
 */
public class HitIndicator {

    private static final List<Hit> HITS = new ArrayList<>();

    private static class Hit {
        final float worldAngle;
        final long startTime;

        Hit(float worldAngle) {
            this.worldAngle = worldAngle;
            this.startTime = System.currentTimeMillis();
        }
    }

    public static void trigger(float relativeYaw) {
        MinecraftClient mc = MinecraftClient.getInstance();
        float currentYaw = mc.player != null ? mc.player.getYaw() : 0f;
        triggerWorld(currentYaw + relativeYaw);
    }

    public static void triggerWorld(float worldAngle) {
        HITS.add(new Hit(worldAngle));
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
        float playerYaw = mc.player.getYaw(delta);

        Iterator<Hit> it = HITS.iterator();
        while (it.hasNext()) {
            Hit h = it.next();
            long elapsed = now - h.startTime;
            if (elapsed > duration) {
                it.remove();
                continue;
            }

            float t = elapsed / (float) duration;
            float alpha = (1.0f - t);
            int a = (int) (alpha * 245);
            if (a <= 4) continue;

            // Compute dynamic screen angle from world orientation
            float rel = MathHelper.wrapDegrees(h.worldAngle - playerYaw);
            float angleScreen = rel - 180f;

            float currentRadius = radius + t * 16f;
            float wedgeW = Math.max(16f, 38f - t * 14f);
            float wedgeH = Math.max(3f, 5f - t * 2f);

            ctx.getMatrices().push();
            ctx.getMatrices().translate(cx, cy, 0);
            ctx.getMatrices().multiply(new Quaternionf().rotateZ((float) Math.toRadians(angleScreen + 90f)));

            // Outer soft glow pill
            int glowCol = ((int) (a * 0.38f) << 24) | 0xFF1726;
            SquircleRenderer.pill(ctx, -wedgeW / 2f - 3f, -currentRadius - 3f, wedgeW + 6f, wedgeH + 6f, glowCol);

            // Core neon crimson indicator
            int coreCol = (a << 24) | 0xFF3544;
            SquircleRenderer.pill(ctx, -wedgeW / 2f, -currentRadius, wedgeW, wedgeH, coreCol);

            // Hot white apex pointer
            int tipA = (int) (a * 0.75f);
            if (tipA > 10) {
                int tipCol = (tipA << 24) | 0xFFFFFF;
                SquircleRenderer.pill(ctx, -2.5f, -currentRadius - 2f, 5f, 4f, tipCol);
            }

            ctx.getMatrices().pop();
        }

        ctx.draw();
    }

    public static void clear() {
        HITS.clear();
    }
}