package net.macos.client.utils;

import net.macos.client.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;

import java.util.Random;

public class SoundManager {

    private static final Random RNG = new Random();

    public static void playHitSound() {
        if (!ConfigManager.INSTANCE.enableHitSound) return;

        String preset = ConfigManager.INSTANCE.hitSoundPreset.toLowerCase();
        if (preset.equals("off")) return;

        SoundEvent event = SoundRegistry.HIT_EVENTS.get(preset);
        if (event == null) return;

        float volume = ConfigManager.INSTANCE.hitSoundVolume;
        MinecraftClient.getInstance().getSoundManager().play(
            PositionedSoundInstance.master(event, ConfigManager.INSTANCE.hitSoundPitch, volume)
        );
    }

       public static void playKillSound() {
        System.out.println("[KillSound] called. enabled=" + ConfigManager.INSTANCE.enableKillSound
            + " preset=" + ConfigManager.INSTANCE.killSoundPreset);

        if (!ConfigManager.INSTANCE.enableKillSound) return;

        String preset = ConfigManager.INSTANCE.killSoundPreset.toLowerCase();
        if (preset.equals("off")) return;

        SoundEvent event = SoundRegistry.KILL_EVENTS.get(preset);
        System.out.println("[KillSound] event=" + (event != null ? event.getId() : "NULL"));

        if (event == null) return;

        float volume = ConfigManager.INSTANCE.hitSoundVolume;
        MinecraftClient.getInstance().getSoundManager().play(
            PositionedSoundInstance.master(event, ConfigManager.INSTANCE.hitSoundPitch, volume)
        );
    }
}