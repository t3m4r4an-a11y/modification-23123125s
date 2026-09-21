package net.macos.client.utils;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.macos.client.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

public class Zoom {
    public static KeyBinding zoomKey;

    public static boolean isZooming = false;
    private static float prevFov = 70f;
    private static float currentFov = 70f;
    private static boolean initialized = false;

    public static void init() {
        zoomKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.macclient.zoom",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            "category.macclient"
        ));
    }

    public static void tick(MinecraftClient mc) {
        if (mc.player == null || mc.options == null) return;

        if (!ConfigManager.INSTANCE.enableZoom) {
            isZooming = false;
            return;
        }

        boolean pressed = mc.currentScreen == null
            && InputUtil.isKeyPressed(mc.getWindow().getHandle(), GLFW.GLFW_KEY_C);

        isZooming = pressed;

        float vanillaFov = (float)(int) mc.options.getFov().getValue();

        // Первый тик — синхронизируем prevFov и currentFov с реальным FOV
        if (!initialized) {
            currentFov = vanillaFov;
            prevFov = vanillaFov;
            initialized = true;
            return; // пропускаем первый тик, нет смысла считать
        }

        // Если не зумим — синхронизируемся с ванильным FOV (для смены настроек)
        if (!isZooming) {
            currentFov = vanillaFov;
            prevFov = vanillaFov;
            return;
        }

        prevFov = currentFov;

        float target = ConfigManager.INSTANCE.zoomFov;
        float speed = ConfigManager.INSTANCE.zoomSpeed / 100f;
        // Множитель 1.5f вместо 0.5f — быстрее и без "слишком плавно"
        currentFov = currentFov + (target - currentFov) * speed * 1.5f;
    }

    public static float getCurrentFov(float tickDelta) {
        if (!isZooming) return -1f;
        return MathHelper.lerp(tickDelta, prevFov, currentFov);
    }
}