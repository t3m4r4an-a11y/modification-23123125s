package net.macos.client.utils;

import net.macos.client.config.ConfigManager;
import net.macos.client.particle.KillEffect;
import net.macos.client.particle.KillEffectType;
import net.minecraft.entity.LivingEntity;

public class KillTracker {

    private static LivingEntity target = null;
    private static long attackTime = 0;
    private static final long WINDOW_MS = 3000L;

    public static void onAttack(LivingEntity entity) {
        target = entity;
        attackTime = System.currentTimeMillis();
    }

    public static void tick() {
        if (target == null) return;

        if (System.currentTimeMillis() - attackTime > WINDOW_MS) {
            target = null;
            return;
        }

        if (!target.isAlive()) {
            SoundManager.playKillSound();

            if (ConfigManager.INSTANCE.enableKillEffect) {
                KillEffectType type = KillEffectType.fromName(ConfigManager.INSTANCE.killEffectType);
                double headY = target.getY() + target.getHeight();
                KillEffect.trigger(
                    target.getX(),
                    headY,
                    target.getZ(),
                    type,
                    ConfigManager.INSTANCE.killEffectColor
                );
            }

            target = null;
        }
    }
}