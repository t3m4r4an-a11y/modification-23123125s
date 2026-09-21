package net.macos.client.utils;

import net.macos.client.config.ConfigManager;
import net.minecraft.client.MinecraftClient;

public class Fullbright {

    private static double originalGamma = 0.5;
    private static boolean wasEnabled = false;

    @SuppressWarnings("unchecked")
    public static void tick(MinecraftClient mc) {
        if (mc == null || mc.options == null) return;

        boolean enabled = ConfigManager.INSTANCE.enableFullbright;

        // Запоминаем оригинальную гамму при первом включении
        if (enabled && !wasEnabled) {
            originalGamma = mc.options.getGamma().getValue();
        }

        if (enabled) {
            // forceSetValue — обходит валидатор 0..1
            ((ISimpleOption<Double>)(Object) mc.options.getGamma())
                .forceSetValue((double) ConfigManager.INSTANCE.fullbrightGamma);
        } else if (wasEnabled) {
            // Возвращаем оригинал при выключении
            ((ISimpleOption<Double>)(Object) mc.options.getGamma())
                .forceSetValue(originalGamma);
        }

        wasEnabled = enabled;
    }
}