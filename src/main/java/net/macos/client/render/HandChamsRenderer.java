package net.macos.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.macos.client.config.ConfigManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;

/**
 * Hand & Held Item Chams with glass transparency, color tint, and inner glow.
 */
public final class HandChamsRenderer {

    private HandChamsRenderer() {}

    public static boolean isRenderingArm = false;
    public static boolean isRenderingItem = false;

    public static void onPreRenderArm() {
        if (!ConfigManager.INSTANCE.enableHandChams) return;
        isRenderingArm = true;
        applyChamsRenderState();
    }

    public static void onPostRenderArm() {
        if (!isRenderingArm) return;
        isRenderingArm = false;
        resetChamsRenderState();
    }

    public static void onPreRenderItem() {
        if (!ConfigManager.INSTANCE.enableItemChams) return;
        isRenderingItem = true;
        applyChamsRenderState();
    }

    public static void onPostRenderItem() {
        if (!isRenderingItem) return;
        isRenderingItem = false;
        resetChamsRenderState();
    }

    private static void applyChamsRenderState() {
        String mode = ConfigManager.INSTANCE.handChamsMode;
        if ("Wireframe".equalsIgnoreCase(mode)) {
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
            GL11.glLineWidth(2.0f);
        }

        int color = 0x00D4FF;
        try {
            color = Integer.parseInt(ConfigManager.INSTANCE.handChamsColor.replace("#", ""), 16);
        } catch (Exception ignored) {}

        float cr = ((color >> 16) & 0xFF) / 255.0f;
        float cg = ((color >> 8) & 0xFF) / 255.0f;
        float cb = (color & 0xFF) / 255.0f;

        float r = 0.80f + 0.20f * cr;
        float g = 0.80f + 0.20f * cg;
        float b = 0.80f + 0.20f * cb;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(r, g, b, 1.0f);
    }

    private static void resetChamsRenderState() {
        String mode = ConfigManager.INSTANCE.handChamsMode;
        if ("Wireframe".equalsIgnoreCase(mode)) {
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
        }
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    public static VertexConsumerProvider wrapConsumer(VertexConsumerProvider original, boolean isArm) {
        boolean active = (isArm && ConfigManager.INSTANCE.enableHandChams)
                || (!isArm && ConfigManager.INSTANCE.enableItemChams);
        if (!active) return original;

        return renderLayer -> {
            VertexConsumer consumer = original.getBuffer(renderLayer);
            String mode = ConfigManager.INSTANCE.handChamsMode;
            if (mode == null) mode = "Glass";

            float tr = 0f, tg = 0.83f, tb = 1f;
            float ta = Math.max(0.1f, Math.min(1.0f, ConfigManager.INSTANCE.handChamsAlpha));

            if ("Rainbow".equalsIgnoreCase(mode)) {
                float hue = (System.currentTimeMillis() % 3000L) / 3000.0f;
                int rgb = java.awt.Color.HSBtoRGB(hue, 0.9f, 1.0f);
                tr = ((rgb >> 16) & 0xFF) / 255.0f;
                tg = ((rgb >> 8) & 0xFF) / 255.0f;
                tb = (rgb & 0xFF) / 255.0f;
            } else if ("Gold".equalsIgnoreCase(mode)) {
                tr = 1.0f; tg = 0.84f; tb = 0.0f;
            } else if ("Cyberpunk".equalsIgnoreCase(mode)) {
                boolean cycle = (System.currentTimeMillis() % 1600L) > 800L;
                if (cycle) { tr = 1.0f; tg = 0.0f; tb = 0.5f; } // Neon Pink
                else { tr = 0.0f; tg = 0.94f; tb = 1.0f; }       // Neon Cyan
            } else {
                try {
                    int col = Integer.parseInt(ConfigManager.INSTANCE.handChamsColor.replace("#", ""), 16);
                    tr = ((col >> 16) & 0xFF) / 255.0f;
                    tg = ((col >> 8) & 0xFF) / 255.0f;
                    tb = (col & 0xFF) / 255.0f;
                } catch (Exception ignored) {}
            }

            if ("Hologram".equalsIgnoreCase(mode)) {
                float pulse = 0.35f + 0.35f * (float) Math.sin(System.currentTimeMillis() * 0.006f);
                ta = pulse;
            }

            return new ChamsVertexConsumer(consumer, tr, tg, tb, ta, mode);
        };
    }

    private static class ChamsVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final float tr, tg, tb, ta;
        private final String mode;

        public ChamsVertexConsumer(VertexConsumer delegate, float tr, float tg, float tb, float ta, String mode) {
            this.delegate = delegate;
            this.tr = tr;
            this.tg = tg;
            this.tb = tb;
            this.ta = ta;
            this.mode = mode;
        }

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            return delegate.vertex(x, y, z);
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            int newR, newG, newB, newA;
            if ("Flat".equalsIgnoreCase(mode)) {
                // Flat unshaded CS2-style solid tint
                newR = (int) (tr * 255);
                newG = (int) (tg * 255);
                newB = (int) (tb * 255);
                newA = (int) (ta * 255);
            } else if ("Gold".equalsIgnoreCase(mode)) {
                newR = Math.min(255, (int) (red * 0.2f + 255f * 0.8f));
                newG = Math.min(255, (int) (green * 0.2f + 215f * 0.8f));
                newB = (int) (blue * 0.1f);
                newA = (int) (alpha * ta);
            } else {
                newR = Math.min(255, (int) (red * (0.35f + 0.65f * tr)));
                newG = Math.min(255, (int) (green * (0.35f + 0.65f * tg)));
                newB = Math.min(255, (int) (blue * (0.35f + 0.65f * tb)));
                newA = Math.min(255, (int) (alpha * ta));
            }
            return delegate.color(newR, newG, newB, newA);
        }

        @Override
        public VertexConsumer texture(float u, float v) {
            return delegate.texture(u, v);
        }

        @Override
        public VertexConsumer overlay(int u, int v) {
            return delegate.overlay(u, v);
        }

        @Override
        public VertexConsumer light(int u, int v) {
            if (ConfigManager.INSTANCE.handChamsGlow) {
                return delegate.light(0x00F0, 0x00F0); // full bright glow in darkness
            }
            return delegate.light(u, v);
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            return delegate.normal(x, y, z);
        }

        @Override
        public void next() {
            delegate.next();
        }

        @Override
        public void fixedColor(int red, int green, int blue, int alpha) {
            delegate.fixedColor((int)(255 * tr), (int)(255 * tg), (int)(255 * tb), (int)(255 * ta));
        }

        @Override
        public void unfixColor() {
            delegate.unfixColor();
        }
    }
}
