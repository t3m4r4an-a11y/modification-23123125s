package net.macos.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.macos.client.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Procedural rain droplets and model wetness dripping system:
 * 1. Simulates realistic wetness dripping down the player's screen/arms.
 * 2. Tinted glistening highlights on models when standing under the rain.
 */
public final class WetnessRenderer {

    private WetnessRenderer() {}

    private static final Random RNG = new Random();
    private static final List<Droplet> DROPLETS = new ArrayList<>();
    private static long lastSpawnTime = 0;

    private static class Droplet {
        float x, y;
        float speed;
        float size;
        float alpha;

        Droplet(float x, float y, float speed, float size) {
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.size = size;
            this.alpha = 0.8f;
        }
    }

    public static boolean isWet(LivingEntity entity) {
        if (!ConfigManager.INSTANCE.enableModelWetness || entity == null || entity.getWorld() == null) {
            return false;
        }
        if (!entity.getWorld().isRaining()) {
            return false;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && entity != mc.player && entity.squaredDistanceTo(mc.player) > 64.0) {
            return false;
        }
        return entity.getWorld().isSkyVisible(entity.getBlockPos());
    }

    public static void onPreRenderEntity(LivingEntity entity) {
        if (!isWet(entity)) return;

        // Darker saturated gloss for realistic soaked/wet surfaces
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(0.70f, 0.78f, 0.92f, 1.0f);
    }

    public static void onPostRenderEntity(LivingEntity entity) {
        if (!isWet(entity)) return;

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.defaultBlendFunc();
    }

    /**
     * Renders glass rain droplets trickling on screen when the player is outdoors in the rain
     */
    public static void renderScreenDroplets(DrawContext ctx, float delta) {
        if (!ConfigManager.INSTANCE.enableRainDroplets) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null || mc.world == null) return;
        if (!mc.world.isRaining() || !mc.world.isSkyVisible(mc.player.getBlockPos())) {
            DROPLETS.clear();
            return;
        }

        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        long now = System.currentTimeMillis();

        // Spawn new droplets
        if (now - lastSpawnTime > 150 && DROPLETS.size() < 40) {
            lastSpawnTime = now;
            float rx = RNG.nextFloat() * sw;
            float ry = RNG.nextFloat() * (sh / 3f);
            float speed = 0.8f + RNG.nextFloat() * 1.5f;
            float size = 2f + RNG.nextFloat() * 3f;
            DROPLETS.add(new Droplet(rx, ry, speed, size));
        }

        // Render droplets
        Iterator<Droplet> it = DROPLETS.iterator();
        while (it.hasNext()) {
            Droplet d = it.next();
            d.y += d.speed;
            d.alpha -= 0.003f;

            if (d.y > sh || d.alpha <= 0.05f) {
                it.remove();
                continue;
            }

            int a = (int) (d.alpha * 200);
            int baseColor = (a << 24) | 0x88CCFF;
            int specularColor = ((int)(a * 0.9f) << 24) | 0xFFFFFF;

            // Droplet teardrop pill shape
            int ix = (int) d.x;
            int iy = (int) d.y;
            int is = (int) d.size;

            ctx.fill(ix, iy, ix + is, iy + is * 2, baseColor);
            ctx.fill(ix + 1, iy, ix + is - 1, iy + 1, specularColor);
        }
    }
}
