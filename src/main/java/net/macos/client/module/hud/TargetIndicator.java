package net.macos.client.module.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.macos.client.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TargetIndicator {

    private static final Identifier TEXTURE = new Identifier("macclient", "textures/gui/target/target.png");

    private static class Entry {
        LivingEntity entity;
        long firstAttack;   // когда впервые ударили — для rotation
        long lastAttack;    // когда последний раз ударили — для fade
        Entry(LivingEntity e) {
            this.entity = e;
            this.firstAttack = System.currentTimeMillis();
            this.lastAttack = this.firstAttack;
        }
        // Если уже есть — просто обновляем время последнего удара, rotation НЕ сбрасывается
        void refresh() {
            this.lastAttack = System.currentTimeMillis();
        }
    }

    private static class Projected {
        int screenX, screenY;
        float sizeOnScreen;
        float alpha;
        float rotation;
    }

    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static final List<Projected> PROJECTIONS = new ArrayList<>();
    private static final long FADE_DURATION = 1000L;   // сколько держим после последнего удара
    private static final long FADE_TIME = 500L;         // сколько длится плавное исчезновение
    private static final long ROTATION_TIME = 1500L;    // за сколько делаем полный оборот

    public static void trigger(LivingEntity entity) {
        for (Entry e : ENTRIES) {
            if (e.entity == entity) {
                // Уже есть — обновляем время, rotation не сбрасываем
                e.refresh();
                return;
            }
        }
        ENTRIES.add(new Entry(entity));
    }

    // ============================================================
    // ПРОЕКЦИЯ
    // ============================================================
    public static void updateProjections(MatrixStack matrices, Camera camera, float tickDelta) {
        PROJECTIONS.clear();

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        if (!ConfigManager.INSTANCE.enableTargetIndicator) return;

        long now = System.currentTimeMillis();
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();

        Matrix4f projMat = RenderSystem.getProjectionMatrix();
        Vec3d camPos = camera.getPos();

        Iterator<Entry> it = ENTRIES.iterator();
        while (it.hasNext()) {
            Entry entry = it.next();

            long sinceLastHit = now - entry.lastAttack;
            if (sinceLastHit > FADE_DURATION + FADE_TIME || !entry.entity.isAlive()) {
                it.remove();
                continue;
            }

            // Цель за стеной — не рисуем
            if (!mc.player.canSee(entry.entity)) continue;

            // Alpha: 1.0 пока держим, потом плавно до 0
            float alpha;
            if (sinceLastHit <= FADE_DURATION) {
                alpha = 1f;
            } else {
                float t = (sinceLastHit - FADE_DURATION) / (float) FADE_TIME;
                alpha = 1f - t;
            }

            // Интерполированная позиция
            double ex = entry.entity.prevX + (entry.entity.getX() - entry.entity.prevX) * tickDelta;
            double ey = entry.entity.prevY + (entry.entity.getY() - entry.entity.prevY) * tickDelta;
            double ez = entry.entity.prevZ + (entry.entity.getZ() - entry.entity.prevZ) * tickDelta;

            double tx = ex;
            double ty = ey + entry.entity.getHeight() / 2.0;
            double tz = ez;

            // Проекция
            matrices.push();
            matrices.translate(tx - camPos.x, ty - camPos.y, tz - camPos.z);
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

            // Размер с учётом расстояния
            double dist = mc.player.distanceTo(entry.entity);
            float size = ConfigManager.INSTANCE.targetIndicatorSize;
            size = size * (float)(4.0 / Math.max(1.0, dist));

            // Вращение — от ПЕРВОГО удара, не сбрасывается
            long sinceFirst = now - entry.firstAttack;
            float rotT = sinceFirst / (float) ROTATION_TIME;
            // Один оборот каждые ROTATION_TIME, ease-in-out на первом обороте
            float rotation;
            if (rotT < 1f) {
                float eased = rotT * rotT * (3f - 2f * rotT);
                rotation = eased * 360f;
            } else {
                // Дальше — постоянное вращение
                rotation = 360f + ((rotT - 1f) * 360f) % 360f;
            }
            rotation *= ConfigManager.INSTANCE.targetIndicatorRotations;

            Projected p = new Projected();
            p.screenX = sx;
            p.screenY = sy;
            p.sizeOnScreen = size;
            p.alpha = alpha;
            p.rotation = rotation;
            PROJECTIONS.add(p);
        }
    }

    // ============================================================
    // РЕНДЕР
    // ============================================================
    public static void render(DrawContext context, float tickDelta) {
        if (PROJECTIONS.isEmpty()) return;

        // Включаем blend — нужно для плавного fade
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        int texW = 256;
        int texH = 256;

        for (Projected p : PROJECTIONS) {
            int a = (int)(p.alpha * 255);
            if (a <= 2) continue;

            int size = (int) p.sizeOnScreen;
            if (size < 8) continue;

            context.setShaderColor(1f, 1f, 1f, a / 255f);

            context.getMatrices().push();
            context.getMatrices().translate(p.screenX, p.screenY, 0);
            context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(p.rotation));
            float scaleX = size / (float) texW;
            float scaleY = size / (float) texH;
            context.getMatrices().scale(scaleX, scaleY, 1f);
            context.getMatrices().translate(-texW / 2f, -texH / 2f, 0);

            context.drawTexture(TEXTURE, 0, 0, 0, 0, texW, texH, texW, texH);
            context.getMatrices().pop();

            context.setShaderColor(1f, 1f, 1f, 1f);
        }

        RenderSystem.disableBlend();
    }
}