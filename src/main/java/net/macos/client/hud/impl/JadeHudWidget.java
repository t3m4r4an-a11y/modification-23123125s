package net.macos.client.hud.impl;

import net.macos.client.MacClient;
import net.macos.client.config.ConfigManager;
import net.macos.client.gui.font.AetherionFont;
import net.macos.client.hud.glass.GlassRenderer;
import net.macos.client.hud.glass.GlassWidget;
import net.macos.client.hud.glass.PanelStyle;
import net.macos.client.render.SquircleRenderer;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

/**
 * 2026 Luxury Liquid Glass Jade / Waila HUD overlay:
 * Inspect target block or mob under crosshair with icon, name, mod, health/tool info.
 * Automatically suppresses itself when Target HUD is active to prevent overlapping clutter.
 */
public class JadeHudWidget extends GlassWidget {

    private String targetName = "";
    private String targetSub = "";
    private ItemStack targetItem = ItemStack.EMPTY;
    private float targetHpRatio = -1f;

    public JadeHudWidget() {
        super("jadeHud");
        this.appearSpeed = 5f;
    }

    @Override
    protected void configureStyle(PanelStyle s) {
        s.radius = 9;
        s.bgColor = 0xD8080C14;
        s.borderColor = 0x28FFFFFF;
        s.topAccent = false;
        s.specular = true;
        s.specularColor = 0x20FFFFFF;
        s.gradientTop = 0x16FFFFFF;
        s.gradientBot = 0x10000000;
        s.glowLayers = 1;
        s.glowColor = 0x2000D4FF;
    }

    @Override
    protected boolean shouldBeVisible() {
        if (MacClient.hudEditorOpen) return true;
        if (!ConfigManager.INSTANCE.enableJadeHud) return false;

        // If in combat and Target HUD is active, suppress Jade to keep screen clean
        if (TargetHudWidget.hasActiveCombatTarget) return false;

        return updateTarget();
    }

    private boolean updateTarget() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null || mc.player == null) return false;

        HitResult hit = mc.crosshairTarget;
        if (hit == null || hit.getType() == HitResult.Type.MISS) return false;

        if (hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult bHit) {
            BlockState state = mc.world.getBlockState(bHit.getBlockPos());
            if (state.isAir()) return false;

            targetItem = new ItemStack(state.getBlock().asItem());
            targetName = state.getBlock().getName().getString();
            String id = Registries.BLOCK.getId(state.getBlock()).getNamespace();
            targetSub = id.substring(0, 1).toUpperCase() + id.substring(1);
            targetHpRatio = -1f;
            return true;
        } else if (hit.getType() == HitResult.Type.ENTITY && hit instanceof EntityHitResult eHit) {
            // If Target HUD is enabled, let Target HUD take priority for living entities
            if (ConfigManager.INSTANCE.enableTargetHUD) return false;

            Entity ent = eHit.getEntity();
            targetItem = ItemStack.EMPTY;
            targetName = ent.getName().getString();
            if (ent instanceof LivingEntity living) {
                targetSub = String.format("%.1f / %.1f HP", living.getHealth(), living.getMaxHealth());
                targetHpRatio = Math.max(0f, Math.min(1f, living.getHealth() / living.getMaxHealth()));
            } else {
                targetSub = "Entity";
                targetHpRatio = -1f;
            }
            return true;
        }
        return false;
    }

    @Override
    protected void measure(float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (MacClient.hudEditorOpen && targetName.isEmpty()) {
            targetName = "Diamond Ore";
            targetSub = "Minecraft";
        }

        int nameW = mc != null ? mc.textRenderer.getWidth(targetName) : 80;
        int subW = mc != null ? mc.textRenderer.getWidth(targetSub) : 60;
        int maxText = Math.max(nameW, subW);

        this.w = 36 + maxText + 18;
        this.h = 32;
    }

    @Override
    protected void renderInner(DrawContext ctx, int mouseX, int mouseY, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;

        int alpha = (int) (appearProgress * 255);
        int textColor = (alpha << 24) | 0xF5F5F7;
        int subColor = (alpha << 24) | 0x9EA2B0;

        int iconX = x + 7;
        int iconY = y + (h - 18) / 2;

        // Squircle capsule backing for item icon
        SquircleRenderer.fill(ctx, iconX - 2, iconY - 2, 22, 22, 6, (alpha / 6 << 24) | 0xFFFFFF);
        SquircleRenderer.border(ctx, iconX - 2, iconY - 2, 22, 22, 6, 1.0f, (alpha / 8 << 24) | 0xFFFFFF);

        if (!targetItem.isEmpty()) {
            ctx.drawItem(targetItem, iconX + 1, iconY + 1);
        } else {
            // Glowing pill dot for entities
            SquircleRenderer.pill(ctx, iconX + 3, iconY + 3, 12, 12, (alpha << 24) | style.accentColor);
        }

        int textX = iconX + 26;
        AetherionFont.draw(ctx, targetName, textX, y + 6, textColor);

        if (targetHpRatio >= 0f) {
            // Health bar under name
            int barW = w - (textX - x) - 12;
            int barH = 4;
            int barY = y + 20;
            SquircleRenderer.pill(ctx, textX, barY, barW, barH, (alpha / 5 << 24) | 0xFFFFFF);
            int fillW = (int) (barW * targetHpRatio);
            if (fillW > 0) {
                SquircleRenderer.pill(ctx, textX, barY, fillW, barH, (alpha << 24) | 0x00E676);
            }
        } else {
            // Mod / Category Pill Badge
            int badgeW = AetherionFont.width(targetSub) + 10;
            int badgeH = 11;
            int badgeY = y + 17;
            SquircleRenderer.pill(ctx, textX, badgeY, badgeW, badgeH, (alpha / 10 << 24) | 0xFFFFFF);
            AetherionFont.draw(ctx, targetSub, textX + 5, badgeY + 2, subColor);
        }

        ctx.draw();
    }
}
