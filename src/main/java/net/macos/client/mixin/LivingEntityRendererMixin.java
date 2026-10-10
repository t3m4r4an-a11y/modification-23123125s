package net.macos.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.macos.client.config.ConfigManager;
import net.macos.client.render.WetnessRenderer;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.awt.Color;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> {

    @Inject(
        method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
        at = @At("HEAD")
    )
    private void onPreRenderLiving(T entity, float f, float g, MatrixStack matrixStack,
                                   VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        WetnessRenderer.onPreRenderEntity(entity);
    }

    @Inject(
        method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
        at = @At("RETURN")
    )
    private void onPostRenderLiving(T entity, float f, float g, MatrixStack matrixStack,
                                    VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        WetnessRenderer.onPostRenderEntity(entity);
    }

    @org.spongepowered.asm.mixin.injection.ModifyVariable(
        method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private VertexConsumerProvider wrapConsumerForHitColor(VertexConsumerProvider original, T entity) {
        if (!ConfigManager.INSTANCE.enableHitColor || entity.hurtTime <= 0) {
            return original;
        }

        String mode = ConfigManager.INSTANCE.hitColorMode != null ? ConfigManager.INSTANCE.hitColorMode : "White";
        if ("White".equalsIgnoreCase(mode)) {
            return original;
        }

        float[] rgb = getHitColorRgb(mode);
        return renderLayer -> {
            net.minecraft.client.render.VertexConsumer consumer = original.getBuffer(renderLayer);
            return new HitColorVertexConsumer(consumer, rgb[0], rgb[1], rgb[2]);
        };
    }

    private static float[] getHitColorRgb(String mode) {
        if ("Rainbow".equalsIgnoreCase(mode)) {
            float hue = (System.currentTimeMillis() % 2000L) / 2000.0f;
            int rgb = Color.HSBtoRGB(hue, 0.95f, 1.0f);
            return new float[]{ ((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f };
        } else if ("Accent".equalsIgnoreCase(mode)) {
            try {
                int rgb = Integer.parseInt(ConfigManager.INSTANCE.accentColor.replace("#", ""), 16);
                return new float[]{ ((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f };
            } catch (Exception e) {
                return new float[]{ 0f, 0.83f, 1f };
            }
        } else if ("Red".equalsIgnoreCase(mode)) {
            return new float[]{ 1f, 0.15f, 0.25f };
        } else if ("Golden".equalsIgnoreCase(mode)) {
            return new float[]{ 1f, 0.84f, 0f };
        } else {
            try {
                int rgb = Integer.parseInt(ConfigManager.INSTANCE.hitColorHex.replace("#", ""), 16);
                return new float[]{ ((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f };
            } catch (Exception e) {
                return new float[]{ 1f, 1f, 1f };
            }
        }
    }

    @Inject(method = "getOverlay", at = @At("HEAD"), cancellable = true)
    private static void onGetOverlay(LivingEntity entity, float whiteOverlayProgress, CallbackInfoReturnable<Integer> cir) {
        if (ConfigManager.INSTANCE.enableHitColor && entity.hurtTime > 0) {
            String mode = ConfigManager.INSTANCE.hitColorMode;
            if ("White".equalsIgnoreCase(mode)) {
                cir.setReturnValue(OverlayTexture.packUv(OverlayTexture.getU(1.0f), OverlayTexture.getV(false)));
            } else {
                cir.setReturnValue(OverlayTexture.packUv(OverlayTexture.getU(0.0f), OverlayTexture.getV(false)));
            }
        }
    }

    private static class HitColorVertexConsumer implements net.minecraft.client.render.VertexConsumer {
        private final net.minecraft.client.render.VertexConsumer delegate;
        private final float tr, tg, tb;

        public HitColorVertexConsumer(net.minecraft.client.render.VertexConsumer delegate, float tr, float tg, float tb) {
            this.delegate = delegate;
            this.tr = tr;
            this.tg = tg;
            this.tb = tb;
        }

        @Override
        public net.minecraft.client.render.VertexConsumer vertex(double x, double y, double z) {
            return delegate.vertex(x, y, z);
        }

        @Override
        public net.minecraft.client.render.VertexConsumer color(int red, int green, int blue, int alpha) {
            float blend = 0.78f;
            int nr = Math.min(255, (int) (red * (1.0f - blend) + tr * 255.0f * blend));
            int ng = Math.min(255, (int) (green * (1.0f - blend) + tg * 255.0f * blend));
            int nb = Math.min(255, (int) (blue * (1.0f - blend) + tb * 255.0f * blend));
            return delegate.color(nr, ng, nb, alpha);
        }

        @Override
        public net.minecraft.client.render.VertexConsumer texture(float u, float v) {
            return delegate.texture(u, v);
        }

        @Override
        public net.minecraft.client.render.VertexConsumer overlay(int u, int v) {
            return delegate.overlay(u, v);
        }

        @Override
        public net.minecraft.client.render.VertexConsumer light(int u, int v) {
            return delegate.light(0x00F0, 0x00F0);
        }

        @Override
        public net.minecraft.client.render.VertexConsumer normal(float x, float y, float z) {
            return delegate.normal(x, y, z);
        }

        @Override
        public void next() {
            delegate.next();
        }

        @Override
        public void fixedColor(int red, int green, int blue, int alpha) {
            float blend = 0.78f;
            int nr = Math.min(255, (int) (red * (1.0f - blend) + tr * 255.0f * blend));
            int ng = Math.min(255, (int) (green * (1.0f - blend) + tg * 255.0f * blend));
            int nb = Math.min(255, (int) (blue * (1.0f - blend) + tb * 255.0f * blend));
            delegate.fixedColor(nr, ng, nb, alpha);
        }

        @Override
        public void unfixColor() {
            delegate.unfixColor();
        }
    }
}
