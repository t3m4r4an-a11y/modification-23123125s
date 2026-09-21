package net.macos.client.hud.impl;

import net.macos.client.MacClient;
import net.macos.client.config.ConfigManager;
import net.macos.client.hud.glass.GlassWidget;
import net.macos.client.hud.glass.PanelStyle;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

import java.awt.Color;

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
        s.bgColor = 0x00000000;      // без фона панели — только ячейки
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
            this.w = 0;
            this.h = 0;
            return;
        }

        int count = countVisible();
        if (count == 0) {
            this.w = 0;
            this.h = 0;
            return;
        }

        boolean horizontal = ConfigManager.INSTANCE.armorHudHorizontal;

        if (horizontal) {
            this.w = 6 + count * 24;
            this.h = 28;
        } else {
            this.w = 28;
            this.h = 6 + count * 24;
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
        if (mc.player == null) return 0;
        int count = 0;
        for (EquipmentSlot slot : SLOTS) {
            if (!mc.player.getEquippedStack(slot).isEmpty()) count++;
        }
        return count;
    }

    @Override
    protected void renderInner(DrawContext ctx, int mouseX, int mouseY, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        boolean horizontal = ConfigManager.INSTANCE.armorHudHorizontal;

        int curX = x + 3;
        int curY = y + 3;

        for (int i = 0; i < SLOTS.length; i++) {
            ItemStack stack = mc.player.getEquippedStack(SLOTS[i]);
            if (stack.isEmpty()) continue;

            int maxDur = stack.getMaxDamage();
            int curDur = maxDur - stack.getDamage();
            float percent = maxDur > 0 ? (float) curDur / maxDur : 1.0f;

            lerpProgress[i] = MathHelper.lerp(delta * 0.2f, lerpProgress[i], percent);

            // Цвет по состоянию
            Color arcColor;
            if (percent > 0.5f) {
                arcColor = new Color(80, 220, 80);
            } else if (percent > 0.2f) {
                arcColor = new Color(255, 200, 50);
            } else {
                arcColor = new Color(255, 60, 60);
            }

            // Пульсация при критическом износе
            float pulse = 1.0f;
            if (percent < 0.2f) {
                pulse = 1.0f + (float) Math.sin(System.currentTimeMillis() / 200.0) * 0.06f;
            }

            int cellCX = curX + 10;
            int cellCY = curY + 10;

            // Круглый фон ячейки
            drawCellBg(ctx, cellCX, cellCY, 10);

            // Предмет
            ctx.drawItem(stack, curX + 2, curY + 2);

            // Дуга прочности
            drawUProgress(ctx, cellCX, cellCY, 11, lerpProgress[i], arcColor, pulse);

            if (horizontal) {
                curX += 24;
            } else {
                curY += 24;
            }
        }
    }

    private void drawCellBg(DrawContext ctx, int cx, int cy, int radius) {
        int a = (int) (appearProgress * 140);
        int color = (a << 24) | 0x14141F;
        for (int yOff = -radius; yOff <= radius; yOff++) {
            for (int xOff = -radius; xOff <= radius; xOff++) {
                if (xOff * xOff + yOff * yOff <= radius * radius) {
                    ctx.fill(cx + xOff, cy + yOff, cx + xOff + 1, cy + yOff + 1, color);
                }
            }
        }
    }

    private void drawUProgress(DrawContext ctx, int cx, int cy, int radius,
                               float percent, Color color, float pulseScale) {
        if (percent <= 0) return;

        int a = (int) (appearProgress * 255);
        int r = color.getRed(), g = color.getGreen(), b = color.getBlue();
        int rgb = (a << 24) | (r << 16) | (g << 8) | b;

        // U-shape: 180° (левый бок) → 90° (низ) → 0° (правый бок)
        // Идём от 180° вниз до 0°, при percent = 1 доходим до 0°
        float startAngle = 180f;
        float endAngle = 180f - (180f * percent);
        float step = 2f;

        int scaledRadius = (int) (radius * pulseScale);

        for (float angle = startAngle; angle >= endAngle; angle -= step) {
            float rad = (float) Math.toRadians(angle);
            float fx = cx + (float) (Math.cos(rad) * scaledRadius);
            float fy = cy + (float) (Math.sin(rad) * scaledRadius);

            int px = (int) fx;
            int py = (int) fy;

            // 2x2 пикселя — плотнее и глаже
            ctx.fill(px - 1, py - 1, px + 1, py + 1, rgb);
        }
    }
}