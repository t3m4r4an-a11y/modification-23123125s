package net.macos.client.utils;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.macos.client.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

public class FreeLook {
    public static KeyBinding freeLookKey;
    public static boolean active = false;

    public static float yawOffset = 0f;
    public static float pitchOffset = 0f;

    private static float prevYawOffset = 0f;
    private static float prevPitchOffset = 0f;

    private static Perspective savedPerspective = null;

    public static void init() {
        freeLookKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.macclient.freelook",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_ALT,
            "category.macclient"
        ));
    }

    public static void tick(MinecraftClient mc) {
        if (mc.player == null || mc.options == null) return;

        prevYawOffset = yawOffset;
        prevPitchOffset = pitchOffset;

        boolean wantActive = ConfigManager.INSTANCE.enableFreeLook
                && freeLookKey.isPressed()
                && mc.currentScreen == null;

        // Включение: сохраняем перспективу и уходим в 3-е лицо
        if (wantActive && !active) {
            savedPerspective = mc.options.getPerspective();
            if (savedPerspective == Perspective.FIRST_PERSON) {
                mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
            }
            yawOffset = 0f;
            pitchOffset = 0f;
            prevYawOffset = 0f;
            prevPitchOffset = 0f;
        }
        // Выключение: возвращаем перспективу
        else if (!wantActive && active) {
            if (savedPerspective != null) {
                mc.options.setPerspective(savedPerspective);
                savedPerspective = null;
            }
        }

        active = wantActive;

        // Быстрый и отзывчивый возврат оффсетов в 0
        if (!active) {
            yawOffset   *= 0.28f;
            pitchOffset *= 0.28f;
            if (Math.abs(yawOffset)   < 0.15f) yawOffset = 0f;
            if (Math.abs(pitchOffset) < 0.15f) pitchOffset = 0f;
        }
    }

    public static float getYawOffset(float tickDelta) {
        if (!active && Math.abs(yawOffset) < 0.01f) return 0f;
        return MathHelper.lerpAngleDegrees(tickDelta, prevYawOffset, yawOffset);
    }

    public static float getPitchOffset(float tickDelta) {
        if (!active && Math.abs(pitchOffset) < 0.01f) return 0f;
        return MathHelper.lerp(tickDelta, prevPitchOffset, pitchOffset);
    }

    public static void applyDelta(double dx, double dy) {
        yawOffset   += (float) dx * 0.15f;
        pitchOffset += (float) dy * 0.15f;
        pitchOffset = MathHelper.clamp(pitchOffset, -89.9f, 89.9f);
    }
}