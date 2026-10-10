package net.macos.client.hud.impl;

import net.macos.client.MacClient;
import net.macos.client.config.ConfigManager;
import net.macos.client.gui.font.AetherionFont;
import net.macos.client.hud.glass.GlassRenderer;
import net.macos.client.hud.glass.GlassWidget;
import net.macos.client.hud.glass.PanelStyle;
import net.macos.client.render.SquircleRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.StatusEffectSpriteManager;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 2026 Luxury Liquid Glass Potion HUD.
 * Renders sleek horizontal status effect cards with smooth squircle styling and progress tracks.
 */
public class PotionHudWidget extends GlassWidget {

    private static final int CARD_H = 26;
    private static final int ROW_GAP = 5;
    private static final int MAX_DURATION_TICKS = 1200; // 60s reference

    public PotionHudWidget() {
        super("potionHud");
        this.appearSpeed = 4f;
    }

    @Override
    protected void configureStyle(PanelStyle s) {
        s.radius = 8;
        s.bgColor = 0x00000000;
        s.borderColor = 0x00000000;
        s.topAccent = false;
        s.gradientTop = 0;
        s.gradientBot = 0;
        s.glowLayers = 0;
    }

    @Override
    protected void measure(float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        var effects = mc.player != null ? mc.player.getStatusEffects() : java.util.Collections.<StatusEffectInstance>emptyList();
        if (effects.isEmpty()) {
            if (MacClient.hudEditorOpen) {
                this.w = 120;
                this.h = 2 * (CARD_H + ROW_GAP);
            } else {
                this.w = 0;
                this.h = 0;
            }
            return;
        }

        int maxTextW = 60;
        for (StatusEffectInstance eff : effects) {
            int nameW = AetherionFont.width(eff.getEffectType().getName().getString());
            int timeW = AetherionFont.width(formatTime(eff.getDuration()));
            maxTextW = Math.max(maxTextW, nameW + timeW);
        }

        this.w = Math.max(115, maxTextW + 36);
        this.h = effects.size() * (CARD_H + ROW_GAP);
    }

    @Override
    protected boolean shouldBeVisible() {
        if (MacClient.hudEditorOpen) return true;
        if (!ConfigManager.INSTANCE.getWidget("potionHud").enabled) return false;
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc.player != null && !mc.player.getStatusEffects().isEmpty();
    }

    @Override
    protected void renderInner(DrawContext ctx, int mouseX, int mouseY, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null && !MacClient.hudEditorOpen) return;

        var effects = mc.player != null ? mc.player.getStatusEffects() : java.util.Collections.<StatusEffectInstance>emptyList();
        List<StatusEffectInstance> sorted;
        if (effects.isEmpty()) {
            if (MacClient.hudEditorOpen) {
                sorted = List.of(
                    new StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.SPEED, 1200, 1),
                    new StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.STRENGTH, 2400, 0)
                );
            } else {
                return;
            }
        } else {
            sorted = new ArrayList<>(effects);
            sorted.sort(Comparator.comparingInt(StatusEffectInstance::getDuration).reversed());
        }

        StatusEffectSpriteManager spriteManager = mc.getStatusEffectSpriteManager();
        int a = (int) (appearProgress * 255);
        int curY = y;
        int cardW = Math.max(115, this.w);

        for (StatusEffectInstance effect : sorted) {
            StatusEffect type = effect.getEffectType();
            int color = type.getColor() | 0xFF000000;

            // 1. Ambient shadow
            GlassRenderer.dropShadow(ctx, x, curY, cardW, CARD_H, 10, 6, (int) (0x45 * appearProgress));

            // 2. Liquid glass squircle card
            SquircleRenderer.fill(ctx, x, curY, cardW, CARD_H, 7,
                    ((int) (0x75 * appearProgress) << 24) | 0x111624);
            GlassRenderer.specular(ctx, x, curY, cardW, CARD_H, 7,
                    ((int) (0x20 * appearProgress) << 24) | 0xFFFFFF);
            SquircleRenderer.border(ctx, x, curY, cardW, CARD_H, 7, 1.0f,
                    ((int) (0x35 * appearProgress) << 24) | (color & 0xFFFFFF));

            // 3. Potion sprite
            try {
                Sprite sprite = spriteManager.getSprite(type);
                ctx.drawSprite(x + 4, curY + 4, 0, 18, 18, sprite);
            } catch (Exception ignored) {}

            // 4. Name & amplifier
            String name = type.getName().getString();
            int amp = effect.getAmplifier();
            if (amp > 0) {
                name = name + " " + roman(amp + 1);
            }
            AetherionFont.draw(ctx, name, x + 26, curY + 4, (a << 24) | 0xFFFFFF);

            // 5. Duration
            String time = effect.isInfinite() ? "∞" : formatTime(effect.getDuration());
            int timeW = AetherionFont.width(time);
            AetherionFont.draw(ctx, time, x + cardW - timeW - 6, curY + 4, (a << 24) | 0xA0FFFFFF);

            // 6. Smooth bottom progress bar
            float progress = effect.isInfinite() ? 1f : Math.min(1.0f, effect.getDuration() / (float) MAX_DURATION_TICKS);
            int barW = cardW - 32;
            int barFillW = Math.max(2, (int) (barW * progress));
            SquircleRenderer.pill(ctx, x + 26, curY + 18, barW, 2, ((int) (0x25 * appearProgress) << 24) | color);
            SquircleRenderer.pill(ctx, x + 26, curY + 18, barFillW, 2, (a << 24) | color);

            curY += CARD_H + ROW_GAP;
        }

        ctx.draw();
    }

    private static String roman(int n) {
        return switch (n) {
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(n);
        };
    }

    private static String formatTime(int ticks) {
        int totalSeconds = ticks / 20;
        return String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60);
    }
}