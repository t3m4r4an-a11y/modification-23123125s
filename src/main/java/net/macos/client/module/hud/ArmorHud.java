package net.macos.client.module.hud;

import net.macos.client.config.ConfigManager;
import net.macos.client.gui.GlassPanel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import java.awt.Color;

public class ArmorHud extends GlassPanel {
    private float[] lerpProgress = new float[4];

    public ArmorHud() {
        super(ConfigManager.INSTANCE.armorHudX, ConfigManager.INSTANCE.armorHudY, 80, 100, "Armor");
    }

    @Override
    protected void renderContent(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        boolean horizontal = ConfigManager.INSTANCE.armorHudHorizontal;

        int itemX = x + 10;
        int itemY = y + 15;

        for (int i = 0; i < 4; i++) {
            ItemStack stack = mc.player.getEquippedStack(slots[i]);
            if (stack.isEmpty()) continue;

            int maxDur = stack.getMaxDamage();
            int curDur = maxDur - stack.getDamage();
            float percent = maxDur > 0 ? (float) curDur / maxDur : 1.0f;

            lerpProgress[i] = MathHelper.lerp(delta * 0.2f, lerpProgress[i], percent);

            // Цвет по состоянию (как в ваниле)
            Color arcColor;
            if (percent > 0.5f) {
                arcColor = new Color(80, 220, 80);       // Зелёный
            } else if (percent > 0.2f) {
                arcColor = new Color(255, 200, 50);      // Жёлтый
            } else {
                arcColor = new Color(255, 60, 60);       // Красный
            }

            // Пульсация только при критическом износе
            float pulse = 1.0f;
            if (percent < 0.2f) {
                pulse = 1.0f + (float) Math.sin(System.currentTimeMillis() / 200.0) * 0.2f;
            }

            // Рисуем предмет
            context.drawItem(stack, itemX, itemY);

            // Арка ВОКРУГ предмета: центр = itemX+8, itemY+8
            drawArcProgress(context, itemX + 8, itemY + 8, 10, lerpProgress[i], arcColor, pulse);

            if (horizontal) {
                itemX += 30;
            } else {
                itemY += 20;
            }
        }

        if (horizontal) {
            this.width = Math.max(40, itemX - x + 5);
            this.height = 40;
        } else {
            this.width = 80;
            this.height = Math.max(20, itemY - y + 5);
        }
    }

    private void drawArcProgress(DrawContext context, int cx, int cy, int radius, float percent, Color color, float pulseScale) {
        if (percent <= 0) return;

        int r = color.getRed(), g = color.getGreen(), b = color.getBlue();
        int rgb = (255 << 24) | (r << 16) | (g << 8) | b;

        float startAngle = -90f;                            // Начинаем сверху
        float endAngle = startAngle + (360f * percent);     // По часовой
        float step = 6f;
        int scaledRadius = (int)(radius * pulseScale);

        for (float angle = startAngle; angle <= endAngle; angle += step) {
            float rad = (float) Math.toRadians(angle);
            int px = cx + (int)(Math.cos(rad) * scaledRadius);
            int py = cy + (int)(Math.sin(rad) * scaledRadius);
            context.fill(px - 1, py - 1, px + 1, py + 1, rgb);
        }
    }

    @Override
    protected void savePosition() {
        ConfigManager.INSTANCE.armorHudX = this.x;
        ConfigManager.INSTANCE.armorHudY = this.y;
        ConfigManager.save();
    }
}