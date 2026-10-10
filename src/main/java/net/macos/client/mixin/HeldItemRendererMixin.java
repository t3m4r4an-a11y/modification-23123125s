package net.macos.client.mixin;

import net.macos.client.config.ConfigManager;
import net.macos.client.render.HandGlowRenderer;
import net.macos.client.utils.Animation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {

    @Shadow
    protected abstract void applySwingOffset(MatrixStack matrices, Arm arm, float swingProgress);

    // ============================================================
    // SWING PROGRESS = 0 ВСЕГДА
    // (реальный свинг берём через getHandSwingProgress отдельно)
    // ============================================================
    @ModifyVariable(
        method = "renderFirstPersonItem",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 2
    )
    private float macclient$zeroSwing(float swingProgress) {
        if (!ConfigManager.INSTANCE.enableSmoothSwing) return swingProgress;
        return 0f;
    }

    // ============================================================
    // EQUIP PROGRESS = 0 — убираем нырок
    // ============================================================
    @ModifyArgs(
        method = "renderFirstPersonItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/item/HeldItemRenderer;applyEquipOffset(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/Arm;F)V"
        )
    )
    private void macclient$noEquip(Args args) {
        args.set(2, 0.0f);
    }

    // ============================================================
    // VIEW MODEL
    // ============================================================
    @ModifyVariable(
        method = "renderFirstPersonItem",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private VertexConsumerProvider wrapFirstPersonConsumers(VertexConsumerProvider provider) {
        return net.macos.client.render.HandChamsRenderer.wrapConsumer(provider, false);
    }

    @Inject(
        method = "renderFirstPersonItem",
        at = @At("HEAD")
    )
    private void onPreRenderFirstPersonItem(AbstractClientPlayerEntity player, float tickDelta, float pitch,
                                           Hand hand, float swingProgress, ItemStack item, float equipProgress,
                                           MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                           int light, CallbackInfo ci) {
        if (item.isEmpty()) {
            net.macos.client.render.HandChamsRenderer.onPreRenderArm();
        } else {
            net.macos.client.render.HandChamsRenderer.onPreRenderItem();
        }
    }

    @Inject(
        method = "renderFirstPersonItem",
        at = @At("RETURN")
    )
    private void onPostRenderFirstPersonItem(AbstractClientPlayerEntity player, float tickDelta, float pitch,
                                            Hand hand, float swingProgress, ItemStack item, float equipProgress,
                                            MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                            int light, CallbackInfo ci) {
        net.macos.client.render.HandChamsRenderer.onPostRenderArm();
        net.macos.client.render.HandChamsRenderer.onPostRenderItem();
    }

    @Inject(
        method = "renderFirstPersonItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/item/HeldItemRenderer;applyEquipOffset(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/Arm;F)V",
            shift = At.Shift.AFTER
        )
    )
    private void onRenderViewModel(AbstractClientPlayerEntity player,
                                   float tickDelta, float pitch, Hand hand,
                                   float swingProgress, ItemStack item,
                                   float equipProgress, MatrixStack matrices,
                                   VertexConsumerProvider vertexConsumers,
                                   int light, CallbackInfo ci) {
        if (item.isEmpty()) return;

        if (hand == Hand.MAIN_HAND) {
            if (!ConfigManager.INSTANCE.enableViewModel) return;
            matrices.translate(
                ConfigManager.INSTANCE.vmOffsetX,
                ConfigManager.INSTANCE.vmOffsetY,
                ConfigManager.INSTANCE.vmOffsetZ
            );
            float s = ConfigManager.INSTANCE.vmScale;
            matrices.scale(s, s, s);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(ConfigManager.INSTANCE.vmRotateX));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(ConfigManager.INSTANCE.vmRotateY));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(ConfigManager.INSTANCE.vmRotateZ));

        } else if (hand == Hand.OFF_HAND) {
            if (!ConfigManager.INSTANCE.enableOffHandViewModel) return;
            matrices.translate(
                ConfigManager.INSTANCE.offOffsetX,
                ConfigManager.INSTANCE.offOffsetY,
                ConfigManager.INSTANCE.offOffsetZ
            );
            float s = ConfigManager.INSTANCE.offScale;
            matrices.scale(s, s, s);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(ConfigManager.INSTANCE.offRotateX));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(ConfigManager.INSTANCE.offRotateY));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(ConfigManager.INSTANCE.offRotateZ));
        }
    }

    // ============================================================
    // SWING — наша анимация
    // ============================================================
    @Redirect(
        method = "renderFirstPersonItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/item/HeldItemRenderer;applySwingOffset(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/Arm;F)V"
        )
    )
    private void onApplySwingOffset(HeldItemRenderer instance, MatrixStack matrices,
                                     Arm arm, float swingProgress) {
        if (!ConfigManager.INSTANCE.enableSmoothSwing) {
            applySwingOffset(matrices, arm, swingProgress);
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            applySwingOffset(matrices, arm, swingProgress);
            return;
        }

        Animation.applySwing(matrices, arm, swingProgress, ConfigManager.INSTANCE.swingMode);
    }

    // ============================================================
    // HAND & ITEM CHAMS (FIRST PERSON)
    // ============================================================
    @Inject(
        method = "renderArm",
        at = @At("HEAD")
    )
    private void onPreRenderArm(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                               int light, Arm arm, CallbackInfo ci) {
        net.macos.client.render.HandChamsRenderer.onPreRenderArm();
    }

    @Inject(
        method = "renderArm",
        at = @At("RETURN")
    )
    private void onPostRenderArm(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                int light, Arm arm, CallbackInfo ci) {
        net.macos.client.render.HandChamsRenderer.onPostRenderArm();
    }

    @ModifyVariable(
        method = "renderArm",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private VertexConsumerProvider wrapArmConsumers(VertexConsumerProvider provider) {
        return net.macos.client.render.HandChamsRenderer.wrapConsumer(provider, true);
    }

    @Inject(
        method = "renderArmHoldingItem",
        at = @At("HEAD")
    )
    private void onPreRenderArmHolding(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                      int light, float equipProgress, float swingProgress,
                                      Arm arm, CallbackInfo ci) {
        net.macos.client.render.HandChamsRenderer.onPreRenderArm();
    }

    @Inject(
        method = "renderArmHoldingItem",
        at = @At("RETURN")
    )
    private void onPostRenderArmHolding(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                       int light, float equipProgress, float swingProgress,
                                       Arm arm, CallbackInfo ci) {
        net.macos.client.render.HandChamsRenderer.onPostRenderArm();
    }

    @ModifyVariable(
        method = "renderArmHoldingItem",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private VertexConsumerProvider wrapArmHoldingConsumers(VertexConsumerProvider provider) {
        return net.macos.client.render.HandChamsRenderer.wrapConsumer(provider, true);
    }

    // === HELD ITEM CHAMS ===
    @Inject(
        method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
        at = @At("HEAD")
    )
    private void onPreRenderItem(net.minecraft.entity.LivingEntity entity, ItemStack item,
                                net.minecraft.client.render.model.json.ModelTransformationMode modelTransformationMode,
                                boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                int light, CallbackInfo ci) {
        net.macos.client.render.HandChamsRenderer.onPreRenderItem();
    }

    @Inject(
        method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
        at = @At("RETURN")
    )
    private void onPostRenderItem(net.minecraft.entity.LivingEntity entity, ItemStack item,
                                 net.minecraft.client.render.model.json.ModelTransformationMode modelTransformationMode,
                                 boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                 int light, CallbackInfo ci) {
        net.macos.client.render.HandChamsRenderer.onPostRenderItem();
    }

    @ModifyVariable(
        method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private VertexConsumerProvider wrapItemConsumers(VertexConsumerProvider provider) {
        return net.macos.client.render.HandChamsRenderer.wrapConsumer(provider, false);
    }

    // ============================================================
    // HAND GLOW — capture framebuffer BEFORE all 1st-person rendering
    // ============================================================
    @Inject(
        method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V",
        at = @At("HEAD")
    )
    private void macclient$glowCaptureBefore(
            float tickDelta, MatrixStack matrices, Immediate vertexConsumers,
            ClientPlayerEntity player, int light, CallbackInfo ci) {
        HandGlowRenderer.getInstance().captureBeforeHands();
    }

    // ============================================================
    // HAND GLOW — capture framebuffer AFTER all 1st-person rendering
    // ============================================================
    @Inject(
        method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V",
        at = @At("TAIL")
    )
    private void macclient$glowCaptureAfter(
            float tickDelta, MatrixStack matrices, Immediate vertexConsumers,
            ClientPlayerEntity player, int light, CallbackInfo ci) {
        HandGlowRenderer.getInstance().captureAfterHands();
    }
}