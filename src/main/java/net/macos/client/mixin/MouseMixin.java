package net.macos.client.mixin;

import net.macos.client.utils.FreeLook;
import net.minecraft.client.Mouse;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Mouse.class)
public class MouseMixin {

    @Redirect(
        method = "updateMouse",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"
        )
    )
    private void onLookDirection(ClientPlayerEntity player, double dx, double dy) {
        if (FreeLook.active) {
            FreeLook.applyDelta(dx, dy);
        } else {
            player.changeLookDirection(dx, dy);
        }
    }
}