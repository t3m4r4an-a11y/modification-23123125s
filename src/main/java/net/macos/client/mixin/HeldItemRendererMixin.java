package net.macos.client.mixin;

import net.macos.client.config.ConfigManager;
import net.macos.client.utils.Animation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
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
}