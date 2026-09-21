package net.macos.client.utils;

import net.macos.client.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

public class AntiAFK {
    private static long lastAction = 0;
    private static final long INTERVAL = 20_000L; // раз в 20 секунд

    public static void tick(MinecraftClient mc) {
        if (!ConfigManager.INSTANCE.enableAntiAFK) return;
        if (mc.player == null || mc.world == null) return;

        long now = System.currentTimeMillis();
        if (now - lastAction < INTERVAL) return;
        lastAction = now;

        // Лёгкий поворот камеры (незаметно, но сервер видит активность)
        float yaw = mc.player.getYaw() + (float)(Math.random() * 4 - 2);
        float pitch = MathHelper.clamp(mc.player.getPitch() + (float)(Math.random() * 4 - 2), -90f, 90f);
        mc.player.setYaw(yaw);
        mc.player.setPitch(pitch);
    }
}