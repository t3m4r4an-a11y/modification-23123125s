package net.macos.client.mixin;

import net.macos.client.config.ConfigManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin {

    @Inject(
        method = "render(Lnet/minecraft/entity/ItemEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V", shift = At.Shift.AFTER)
    )
    private void onRenderItemPhysics(ItemEntity entity, float yaw, float tickDelta,
                                     MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                     int light, CallbackInfo ci) {
        if (!ConfigManager.INSTANCE.enableItemPhysic) return;

        if (entity.isOnGround()) {
            // Cancel vertical bobbing sine wave so item rests flat against block surface
            float h = MathHelper.sin(((float) entity.getItemAge() + tickDelta) / 10.0f + entity.uniqueOffset) * 0.1f + 0.1f;
            matrices.translate(0.0f, -h + 0.04f, 0.0f);

            // Lay flat horizontally with random natural rotation
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0f));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(entity.uniqueOffset * 180.0f));
        } else if (ConfigManager.INSTANCE.itemPhysicRotate) {
            // Tumble smoothly in 3D mid-air when dropped or falling
            float rot = (entity.getItemAge() + tickDelta) * 14.0f;
            float dir = (entity.uniqueOffset % 2.0f < 1.0f) ? 1.0f : -1.0f;
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rot * 1.5f * dir));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rot * dir));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rot * 0.8f * dir));
        }
    }
}
