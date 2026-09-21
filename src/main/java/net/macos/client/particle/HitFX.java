package net.macos.client.particle;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class HitFX {

    private static final Random RNG = new Random();
    private static final List<FX> FXS = new ArrayList<>();

    // ============================================================
    // НАСТРОЙКИ РАНДОМА — меняй тут
    // ============================================================
    private static final int    COUNT_MIN  = 6;
    private static final int    COUNT_MAX  = 14;
    private static final double SPEED_MIN  = 2.5;
    private static final double SPEED_MAX  = 6.5;
    private static final double LIFE_MIN   = 0.7;   // сек
    private static final double LIFE_MAX   = 1.2;

    private static class FX {
        double x, y, z;
        double vx, vy, vz;
        long startTime;
        long lifetime;
        float size;
        FXType type;
        int color;
        float rotation;
        float rotationSpeed;
    }

    private static class Projected {
        int screenX, screenY;
        float size;
        float alpha;
        float rotation;
        Identifier texture;
        int color;
    }

    private static final List<Projected> PROJECTIONS = new ArrayList<>();

    // ============================================================
    // ТРИГГЕР
    // ============================================================
    public static void trigger(double cx, double cy, double cz, FXType type,
                                String colorHex, boolean crit) {
        Color color = parseColor(colorHex);

        int count = COUNT_MIN + RNG.nextInt(COUNT_MAX - COUNT_MIN + 1);

        for (int i = 0; i < count; i++) {
            FX fx = new FX();
            fx.x = cx;
            fx.y = cy;
            fx.z = cz;

            double speed = SPEED_MIN + RNG.nextDouble() * (SPEED_MAX - SPEED_MIN);
            double theta = RNG.nextDouble() * Math.PI * 2;
            double phi = (RNG.nextDouble() - 0.5) * Math.PI;
            fx.vx = Math.cos(theta) * Math.cos(phi) * speed;
            fx.vy = Math.sin(phi) * speed * 0.6 + 1.5;
            fx.vz = Math.sin(theta) * Math.cos(phi) * speed;

            fx.startTime = System.currentTimeMillis();
            fx.lifetime = (long)((LIFE_MIN + RNG.nextDouble() * (LIFE_MAX - LIFE_MIN)) * 1000);

            fx.size = (crit ? 0.20f : 0.12f) + RNG.nextFloat() * 0.10f;

            fx.type = type;
            fx.color = (255 << 24) | (color.getRGB() & 0xFFFFFF);
            fx.rotation = RNG.nextFloat() * 360f;
            fx.rotationSpeed = (RNG.nextFloat() - 0.5f) * 300f;
            FXS.add(fx);
        }
    }

    // ============================================================
    // ПРОЕКЦИИ
    // ============================================================
    public static void updateProjections(MatrixStack matrices, Camera camera, float tickDelta) {
        PROJECTIONS.clear();

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        long now = System.currentTimeMillis();
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();

        Matrix4f projMat = RenderSystem.getProjectionMatrix();
        Vec3d camPos = camera.getPos();

        Iterator<FX> it = FXS.iterator();
        while (it.hasNext()) {
            FX fx = it.next();
            long elapsed = now - fx.startTime;
            if (elapsed > fx.lifetime) {
                it.remove();
                continue;
            }

            double dt = elapsed / 1000.0;
            double px = fx.x + fx.vx * dt;
            double py = fx.y + fx.vy * dt - 1.5 * dt * dt;
            double pz = fx.z + fx.vz * dt;

            matrices.push();
            matrices.translate(px - camPos.x, py - camPos.y, pz - camPos.z);
            Matrix4f mv = matrices.peek().getPositionMatrix();

            Vector4f clip = new Vector4f(0, 0, 0, 1);
            mv.transform(clip);
            projMat.transform(clip);
            matrices.pop();

            if (clip.w <= 0.001f) continue;

            float ndcX = clip.x / clip.w;
            float ndcY = clip.y / clip.w;

            int sx = (int)((ndcX * 0.5f + 0.5f) * sw);
            int sy = (int)((1.0f - (ndcY * 0.5f + 0.5f)) * sh);

            double dist = Math.sqrt((px - camPos.x) * (px - camPos.x)
                    + (py - camPos.y) * (py - camPos.y)
                    + (pz - camPos.z) * (pz - camPos.z));
            float size = fx.size * 60f * (float)(4.0 / Math.max(1.0, dist));

            float progress = elapsed / (float) fx.lifetime;
            float alpha = 1f - progress;

            Projected p = new Projected();
            p.screenX = sx;
            p.screenY = sy;
            p.size = size;
            p.alpha = alpha;
            p.rotation = fx.rotation + fx.rotationSpeed * (elapsed / 1000f);
            p.texture = new Identifier("macclient", "textures/particle/" + fx.type.texture + ".png");
            p.color = fx.color;
            PROJECTIONS.add(p);
        }
    }

    // ============================================================
    // РЕНДЕР
    // ============================================================
    public static void render(DrawContext context, float tickDelta) {
        if (PROJECTIONS.isEmpty()) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        for (Projected p : PROJECTIONS) {
            int a = (int)(p.alpha * 255);
            if (a <= 3) continue;

            int size = (int) p.size;
            if (size < 4) continue;

            int r = (p.color >> 16) & 0xFF;
            int g = (p.color >> 8) & 0xFF;
            int b = p.color & 0xFF;

            context.setShaderColor(r / 255f, g / 255f, b / 255f, a / 255f);

            context.getMatrices().push();
            context.getMatrices().translate(p.screenX, p.screenY, 0);
            context.getMatrices().multiply(
                net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(p.rotation));
            context.getMatrices().scale(size / 32f, size / 32f, 1f);
            context.getMatrices().translate(-16, -16, 0);

            context.drawTexture(p.texture, 0, 0, 0, 0, 32, 32, 32, 32);

            context.getMatrices().pop();
            context.setShaderColor(1f, 1f, 1f, 1f);
        }

        RenderSystem.disableBlend();
    }

    private static Color parseColor(String hex) {
        try {
            return Color.decode(hex);
        } catch (Exception e) {
            return Color.WHITE;
        }
    }
}