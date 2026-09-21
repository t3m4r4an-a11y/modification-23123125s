package net.macos.client.mixin;

import net.macos.client.module.hud.HitIndicator;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.DamageTiltS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    @Inject(method = "onDamageTilt", at = @At("HEAD"))
    private void onDamageTilt(DamageTiltS2CPacket packet, CallbackInfo ci) {
        // packet.yaw() возвращает угол относительно игрока
        // 0 = спереди, 90 = справа, -90 = слева, 180 = сзади
        HitIndicator.trigger(packet.yaw());
    }
}