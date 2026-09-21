package net.macos.client.utils;

import net.macos.client.config.ConfigManager;
import net.minecraft.client.MinecraftClient;

public class AutoSprint {
    public static void tick(MinecraftClient mc) {
        if (!ConfigManager.INSTANCE.enableAutoSprint) return;
        if (mc.player == null) return;

        // Спринт только если игрок идёт вперёд и не крадётся и не ест
        boolean movingForward = mc.options.forwardKey.isPressed();
        boolean sneaking = mc.player.isSneaking();
        boolean usingItem = mc.player.isUsingItem();
        boolean inWater = mc.player.isTouchingWater();
        boolean hunger = mc.player.getHungerManager().getFoodLevel() > 6;

        if (movingForward && !sneaking && !usingItem && !inWater && hunger) {
            mc.player.setSprinting(true);
        }
    }
}