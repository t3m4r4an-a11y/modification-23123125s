package net.macos.client.hud.impl;

import net.macos.client.MacClient;
import net.macos.client.config.ConfigManager;
import net.macos.client.hud.glass.GlassRenderer;
import net.macos.client.hud.glass.GlassWidget;
import net.macos.client.hud.glass.PanelStyle;
import net.macos.client.render.SquircleRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

import java.awt.Color;

/**
 * 2026 Luxury Liquid Glass Armor HUD.
 * Renders individual squircle item capsules with smooth gradient durability tracks.
 */
public class ArmorHudWidget extends GlassWidget {

    private static final EquipmentSlot[] SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private final float[] lerpProgress = new float[4];

    public ArmorHudWidget() {
        super("armorHud");
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
        if (mc.player == null) {
            this.w = 0; this.h = 0;
            return;
        }

        int count = countVisible();
        if (count == 0) {
            this.w = 0; this.h = 0;
            return;
        }

        boolean horizontal = ConfigManager.INSTANCE.armorHudHorizontal;
        int slotSize = 28;
        int gap = 5;

        if (horizontal) {
            this.w = count * slotSize + (count - 1) * gap;
            this.h = 32;
        } else {
            this.w = slotSize;
            this.h = count * 32 + (count - 1) * gap;
        }
    }

    @Override
    protected boolean shouldBeVisible() {
        if (MacClient.hudEditorOpen) return true;
        if (!ConfigManager.INSTANCE.getWidget("armorHud").enabled) return false;
        return countVisible() > 0;
    }

    private int countVisible() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return MacClient.hudEditorOpen ? 4 : 0;
        int count = 0;
        for (EquipmentSlot slot : SLOTS) {
            if (!mc.player.getEquippedStack(slot).isEmpty()) count++;
        }
        if (count == 0 && MacClient.hudEditorOpen) return 4;
        return count;
    }

    private static final net.minecraft.item.Item[] PREVIEW_ARMOR = {
        net.minecraft.item.Items.DIAMOND_HELMET,
        net.minecraft.item.Items.DIAMOND_CHESTPLATE,
        net.minecraft.item.Items.DIAMOND_LEGGINGS,
        net.minecraft.item.Items.DIAMOND_BOOTS
    };

    @Override
    protected void renderInner(DrawContext ctx, int mouseX, int mouseY, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null && !MacClient.hudEditorOpen) return;

        boolean horizontal = ConfigManager.INSTANCE.armorHudHorizontal;

        int curX = x;
        int curY = y;
        int slotW = 28;
        int slotH = 32;
        int a = (int) (appearProgress * 255);

        for (int i = 0; i < SLOTS.length; i++) {
            ItemStack stack = mc.player != null ? mc.player.getEquippedStack(SLOTS[i]) : ItemStack.EMPTY;
            if (stack.isEmpty()) {
                if (MacClient.hudEditorOpen) {
                    stack = new ItemStack(PREVIEW_ARMOR[i]);
                } else {
                    continue;
                }
            }

            int maxDur = stack.getMaxDamage();
            int curDur = maxDur - stack.getDamage();
            float percent = maxDur > 0 ? (float) curDur / maxDur : 1.0f;

            lerpProgress[i] = MathHelper.lerp(delta * 0.25f, lerpProgress[i], percent);

            // Color coding
            Color arcColor;
            if (percent > 0.5f) {
                arcColor = new Color(74, 222, 128); // Emerald green
            } else if (percent > 0.2f) {
                arcColor = new Color(250, 204, 21); // Amber yellow
            } else {
                arcColor = new Color(248, 113, 113); // Coral red
            }

            boolean isLow = percent < 0.2f;
            int borderCol = isLow ? 0x60F87171 : 0x22FFFFFF;

            // 1. Ambient drop shadow
            GlassRenderer.dropShadow(ctx, curX, curY, slotW, slotH, 10, 6, (int) (0x45 * appearProgress));

            // 2. Squircle glass capsule
            SquircleRenderer.fill(ctx, curX, curY, slotW, slotH, 7,
                    ((int) (0x75 * appearProgress) << 24) | 0x111624);
            GlassRenderer.specular(ctx, curX, curY, slotW, slotH, 7,
                    ((int) (0x20 * appearProgress) << 24) | 0xFFFFFF);
            SquircleRenderer.border(ctx, curX, curY, slotW, slotH, 7, 1.0f,
                    ((int) (appearProgress * 255) << 24) | (borderCol & 0xFFFFFF));

            // 3. Render Item
            ctx.drawItem(stack, curX + 6, curY + 5);

            // 4. Durability track & fill
            if (maxDur > 0) {
                int barW = 20;
                int barX = curX + 4;
                int barY = curY + 25;
                int fillW = Math.max(2, (int) (barW * MathHelper.clamp(lerpProgress[i], 0f, 1f)));

                SquircleRenderer.pill(ctx, barX, barY, barW, 2, ((int) (0x30 * appearProgress) << 24) | 0x000000);
                SquircleRenderer.pill(ctx, barX, barY, fillW, 2, (a << 24) | (arcColor.getRGB() & 0xFFFFFF));
            }

            if (horizontal) {
                curX += slotW + 5;
            } else {
                curY += slotH + 5;
            }
        }

        ctx.draw();
    }
}