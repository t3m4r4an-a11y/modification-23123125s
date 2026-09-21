package net.macos.client.waypoint;

import net.macos.client.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class WaypointRenderer {

    // Сюда складываем проекции каждый кадр из WorldRenderEvents.LAST
    private static final List<Projected> projections = new ArrayList<>();

    private static class Projected {
        Waypoint wp;
        int screenX;
        int screenY;
        double dist;
        boolean visible;
    }

    // === ВЫЗЫВАЕТСЯ ИЗ WorldRenderEvents.LAST ===
    public static void updateProjections(MatrixStack matrices, Camera camera, float tickDelta) {
        projections.clear();

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        if (!ConfigManager.INSTANCE.enableWaypoints) return;

        String dim = mc.world.getRegistryKey().getValue().toString();
        Vec3d camPos = camera.getPos();
        double renderDist = mc.options.getViewDistance().getValue() * 16.0;

        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();

        // Настоящая проекционная матрица текущего кадра
        Matrix4f projMat = RenderSystem.getProjectionMatrix();

        for (Waypoint wp : WaypointManager.getAll()) {
            if (!wp.dimension.equals(dim)) continue;

            double dist = wp.distanceTo(mc.player.getX(), mc.player.getY(), mc.player.getZ());
            if (dist > renderDist) continue;

            // Позиция вейпоинта в системе мира
            matrices.push();
            matrices.translate(wp.x - camPos.x, wp.y - camPos.y, wp.z - camPos.z);
            Matrix4f modelView = matrices.peek().getPositionMatrix();

            // Мировая точка -> клип-спейс
            Vector4f clip = new Vector4f(0, 0, 0, 1);
            modelView.transform(clip);
            projMat.transform(clip);
            matrices.pop();

            // w <= 0 — точка позади камеры
            if (clip.w <= 0.001f) continue;

            float ndcX = clip.x / clip.w;
            float ndcY = clip.y / clip.w;

            int sx = (int)((ndcX * 0.5f + 0.5f) * sw);
            int sy = (int)((1.0f - (ndcY * 0.5f + 0.5f)) * sh);

            Projected p = new Projected();
            p.wp = wp;
            p.screenX = sx;
            p.screenY = sy;
            p.dist = dist;
            p.visible = isVisible(mc, wp);
            projections.add(p);
        }
    }

    // === ВЫЗЫВАЕТСЯ ИЗ HudRenderCallback ===
    public static void render2D(DrawContext context) {
        if (!ConfigManager.INSTANCE.enableWaypoints) return;
        if (projections.isEmpty()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();

        for (Projected p : projections) {
            if (p.screenX < -300 || p.screenX > sw + 300) continue;
            if (p.screenY < -300 || p.screenY > sh + 300) continue;

            int alpha = p.visible ? 255 : 70;
            Color color = new Color(p.wp.color, true);

            // Ромб-маркер
            drawDiamond(context, p.screenX, p.screenY, 8, (alpha << 24) | 0x000000);
            drawDiamond(context, p.screenX, p.screenY, 6, (alpha << 24) | (color.getRGB() & 0xFFFFFF));

            // Подпись с расстоянием
            String text = p.wp.name + " [" + (int) p.dist + "m]";
            int tw = mc.textRenderer.getWidth(text);
            int boxW = tw + 12;
            int boxH = 14;
            int bx = p.screenX - boxW / 2;
            int by = p.screenY + 12;

            int bgAlpha = p.visible ? 0xE0 : 0x50;
            context.fill(bx, by, bx + boxW, by + boxH, (bgAlpha << 24) | 0x101820);
            context.fill(bx, by, bx + boxW, by + 1, (alpha << 24) | (color.getRGB() & 0xFFFFFF));
            context.drawTextWithShadow(mc.textRenderer, text, bx + 6, by + 3,
                (alpha << 24) | 0xFFFFFF);
        }
    }

    private static boolean isVisible(MinecraftClient mc, Waypoint wp) {
        Camera cam = mc.gameRenderer.getCamera();
        Vec3d start = cam.getPos();
        Vec3d end = new Vec3d(wp.x, wp.y, wp.z);

        if (start.distanceTo(end) < 1.5) return true;

        BlockHitResult hit = mc.world.raycast(new RaycastContext(
            start, end,
            RaycastContext.ShapeType.COLLIDER,
            RaycastContext.FluidHandling.NONE,
            mc.player
        ));

        return hit.getType() == HitResult.Type.MISS;
    }

    private static void drawDiamond(DrawContext context, int cx, int cy, int size, int color) {
        for (int i = 0; i <= size; i++) {
            int w = i;
            context.fill(cx - w, cy - (size - i), cx + w + 1, cy - (size - i) + 1, color);
            context.fill(cx - w, cy + (size - i), cx + w + 1, cy + (size - i) + 1, color);
        }
    }
}