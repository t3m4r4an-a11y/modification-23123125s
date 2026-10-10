package net.macos.client.mixin;

import net.macos.client.render.HandChamsRenderer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {

    // === RIGHT ARM ===
    @Inject(
        method = "renderRightArm",
        at = @At("HEAD")
    )
    private void onPreRenderRightArm(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                    int light, AbstractClientPlayerEntity player, CallbackInfo ci) {
        HandChamsRenderer.onPreRenderArm();
    }

    @Inject(
        method = "renderRightArm",
        at = @At("RETURN")
    )
    private void onPostRenderRightArm(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                     int light, AbstractClientPlayerEntity player, CallbackInfo ci) {
        HandChamsRenderer.onPostRenderArm();
    }

    @ModifyVariable(
        method = "renderRightArm",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private VertexConsumerProvider wrapRightArmConsumers(VertexConsumerProvider provider) {
        return HandChamsRenderer.wrapConsumer(provider, true);
    }

    // === LEFT ARM ===
    @Inject(
        method = "renderLeftArm",
        at = @At("HEAD")
    )
    private void onPreRenderLeftArm(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                   int light, AbstractClientPlayerEntity player, CallbackInfo ci) {
        HandChamsRenderer.onPreRenderArm();
    }

    @Inject(
        method = "renderLeftArm",
        at = @At("RETURN")
    )
    private void onPostRenderLeftArm(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                    int light, AbstractClientPlayerEntity player, CallbackInfo ci) {
        HandChamsRenderer.onPostRenderArm();
    }

    @ModifyVariable(
        method = "renderLeftArm",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private VertexConsumerProvider wrapLeftArmConsumers(VertexConsumerProvider provider) {
        return HandChamsRenderer.wrapConsumer(provider, true);
    }
}
