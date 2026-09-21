package net.macos.client.mixin;
import net.macos.client.particle.FXType;
import net.macos.client.particle.HitFX;
import net.macos.client.utils.KillTracker;
import net.macos.client.utils.SoundManager;
import net.minecraft.client.MinecraftClient;
import net.macos.client.MacClient;
import net.macos.client.config.ConfigManager;
import net.macos.client.hud.WidgetManager;
import net.macos.client.hud.impl.TargetHudWidget;
import net.macos.client.module.hud.TargetIndicator;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {

    @Inject(method = "attackEntity", at = @At("HEAD"))
    private void onAttackEntity(PlayerEntity player, Entity target, CallbackInfo ci) {
        if (player != MinecraftClient.getInstance().player) return;
        if (!(target instanceof LivingEntity living)) return;

        // Target HUD + Combo
        TargetHudWidget th = WidgetManager.INSTANCE.get("targetHud");
        if (th != null) th.onAttack(living);       
        net.macos.client.hud.impl.ComboCounterWidget cc =
            net.macos.client.hud.WidgetManager.INSTANCE.get("comboCounter");
            if (cc != null) cc.onHit();

        // Hit Sound
        SoundManager.playHitSound();
        //target indicator
        TargetIndicator.trigger(living);

        // Kill Sound — запоминаем цель, проверим её смерть в следующих тиках
        KillTracker.onAttack(living);
                // Определяем крит
        boolean crit = player.fallDistance > 0.0f
            && !player.isOnGround()
            && !player.isClimbing()
            && !player.isTouchingWater()
            && !player.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.BLINDNESS)
            && !player.hasVehicle()
            && target instanceof LivingEntity;

        double cx = target.getX();
        double cy = target.getY() + target.getHeight() / 2.0;
        double cz = target.getZ();

        // HitFX
        if (ConfigManager.INSTANCE.enableHitFX
                && !ConfigManager.INSTANCE.hitEffect.equals("none")) {
            FXType hitType = FXType.fromName(ConfigManager.INSTANCE.hitEffect);
            HitFX.trigger(cx, cy, cz, hitType, ConfigManager.INSTANCE.hitEffectColor, false);
        }

        // CritFX
        if (crit && ConfigManager.INSTANCE.enableCritFX
                && !ConfigManager.INSTANCE.critEffect.equals("none")) {
            FXType critType = FXType.fromName(ConfigManager.INSTANCE.critEffect);
            HitFX.trigger(cx, cy, cz, critType, ConfigManager.INSTANCE.critEffectColor, true);
        }
    }
    
}