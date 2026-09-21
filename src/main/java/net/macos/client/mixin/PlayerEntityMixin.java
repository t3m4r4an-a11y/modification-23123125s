package net.macos.client.mixin;

import net.macos.client.config.ConfigManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {

    @Redirect(
        method = "attack",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/World;playSound(Lnet/minecraft/entity/player/PlayerEntity;DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FF)V"
        )
    )
    private void macclient$blockVanillaHitSound(World world, PlayerEntity except,
                                                 double x, double y, double z,
                                                 SoundEvent sound, SoundCategory category,
                                                 float volume, float pitch) {
        if (ConfigManager.INSTANCE.enableHitSound
                && ConfigManager.INSTANCE.disableVanillaHitSound) {
            return; // звук не играет
        }
        world.playSound(except, x, y, z, sound, category, volume, pitch);
    }
}