package net.macos.client.gui;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class MacClientMenuKey {
    public static KeyBinding openKey;

    public static void init() {
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.macclient.menu",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            "category.macclient"
        ));
    }
}