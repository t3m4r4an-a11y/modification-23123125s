package net.macos.client.mixin;

import net.macos.client.module.hud.HitIndicator;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Inject(method = "damage", at = @At("RETURN"))
    private void onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof ClientPlayerEntity player)) return;
        if (!cir.getReturnValue()) return;

        Entity attacker = source.getAttacker();
        if (attacker == null) return;
        if (attacker == player) return;

        double dx = attacker.getX() - player.getX();
        double dz = attacker.getZ() - player.getZ();
        double worldAngle = Math.toDegrees(Math.atan2(-dx, dz));
        double relative = worldAngle - player.getYaw();
        relative = ((relative + 180) % 360 + 180) % 360 - 180;

        HitIndicator.trigger((float) relative);
    }
}