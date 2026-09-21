package net.macos.client.hud.impl;

import net.macos.client.MacClient;
import net.macos.client.hud.glass.GlassWidget;
import net.macos.client.hud.glass.PanelStyle;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.StatusEffectSpriteManager;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.village.VillagerProfession;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

public class TargetHudWidget extends GlassWidget {

    // ============ КОНСТАНТЫ ============
    private static final float HP_LERP = 1.2f;
    private static final float CHIP_SPEED = 2.5f;
    private static final float COLOR_LERP = 2.0f;
    private static final Identifier ICONS = new Identifier("textures/gui/icons.png");

    // ============ СОСТОЯНИЕ ============
    private LivingEntity target;
    private long lastHitTime = 0;
    private float displayHealth = 0;
    private float displayAbsorb = 0;
    private float chipHealth = 0;
    private float displayedColorT = 0f;

    // ============ ЛИЦА МОБОВ ============
    private static class MobFace {
        final Identifier texture;
        final int texW, texH;
        MobFace(Identifier t, int w, int h) { this.texture = t; this.texW = w; this.texH = h; }
    }

    private static final Map<EntityType<?>, MobFace> MOB_FACES = new HashMap<>();
    static {
        put(EntityType.ZOMBIE,           "zombie/zombie",                        64, 64);
        put(EntityType.HUSK,             "zombie/husk",                          64, 64);
        put(EntityType.DROWNED,          "zombie/drowned",                       64, 64);
        put(EntityType.ZOMBIE_VILLAGER,  "zombie_villager/zombie_villager",      64, 64);
        put(EntityType.SKELETON,         "skeleton/skeleton",                    64, 32);
        put(EntityType.STRAY,            "skeleton/stray",                       64, 32);
        put(EntityType.WITHER_SKELETON,  "skeleton/wither_skeleton",             64, 32);
        put(EntityType.CREEPER,          "creeper/creeper",                      64, 32);
        put(EntityType.ENDERMAN,         "enderman/enderman",                    64, 32);
        put(EntityType.PIGLIN,           "piglin/piglin",                        64, 64);
        put(EntityType.PIGLIN_BRUTE,     "piglin/piglin_brute",                  64, 64);
        put(EntityType.ZOMBIFIED_PIGLIN, "piglin/zombified_piglin",              64, 64);
        put(EntityType.WITCH,            "witch",                                64, 64);
        put(EntityType.PILLAGER,         "illager/pillager",                     64, 64);
        put(EntityType.VINDICATOR,       "illager/vindicator",                   64, 64);
        put(EntityType.EVOKER,           "illager/evoker",                       64, 64);
        put(EntityType.ILLUSIONER,       "illager/illusioner",                   64, 64);
    }

    private static void put(EntityType<?> type, String path, int w, int h) {
        MOB_FACES.put(type, new MobFace(
            new Identifier("minecraft", "textures/entity/" + path + ".png"), w, h));
    }

    private static final Identifier VILLAGER_BASE =
        new Identifier("minecraft", "textures/entity/villager/villager.png");

    // ============ КОНСТРУКТОР ============
    public TargetHudWidget() {
        super("targetHud");
        this.appearSpeed = 2.5f;
    }

    public void onAttack(LivingEntity entity) {
        if (this.target != entity) {
            this.displayHealth = entity.getHealth();
            this.displayAbsorb = entity.getAbsorptionAmount();
            this.chipHealth = entity.getHealth();
            this.displayedColorT = 1f - Math.min(1f, entity.getHealth() / entity.getMaxHealth());
        }
        this.target = entity;
        this.lastHitTime = System.currentTimeMillis();
    }

    @Override
    protected boolean shouldBeVisible() {
        if (MacClient.hudEditorOpen) return true;
        return target != null && target.isAlive()
            && System.currentTimeMillis() - lastHitTime <= 5000;
    }

    @Override
    protected void configureStyle(PanelStyle s) {
        s.radius = 8;
        s.bgColor = 0xC014141F;
        s.borderColor = 0x20FFFFFF;
        s.topAccent = false;
        s.gradientTop = 0;
        s.gradientBot = 0;
    }

    @Override
    protected void measure(float delta) {
        int effects = (target != null) ? target.getStatusEffects().size() : 0;
        int rows = (int) Math.ceil(effects / 8.0);
        this.w = 150;
        this.h = 45 + rows * 12;
    }

    @Override
    protected void renderInner(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (target == null || !target.isAlive()) return;
        // Force refresh — берём актуальный инстанс entity из мира по id
        MinecraftClient mcRef = MinecraftClient.getInstance();
        if (mcRef.world != null && target != null) {
            net.minecraft.entity.Entity fresh = mcRef.world.getEntityById(target.getId());
            if (fresh instanceof LivingEntity le) {
                target = le;
            }
        }
        MinecraftClient mc = MinecraftClient.getInstance();

        // === Анимации ===
        float hpLerp = Math.min(1f, delta * HP_LERP);
        displayHealth += (target.getHealth() - displayHealth) * hpLerp;
        displayAbsorb += (target.getAbsorptionAmount() - displayAbsorb) * hpLerp;

        // Chip
        if (chipHealth < displayHealth - 0.05f) {
            chipHealth += (displayHealth - chipHealth) * Math.min(1f, delta * CHIP_SPEED);
            if (Math.abs(chipHealth - displayHealth) < 0.05f) chipHealth = displayHealth;
        } else if (chipHealth > displayHealth) {
            chipHealth += (displayHealth - chipHealth) * Math.min(1f, delta * CHIP_SPEED);
        }

        float hpTarget = Math.max(0, displayHealth / target.getMaxHealth());
        float chipTarget = Math.max(0, chipHealth / target.getMaxHealth());
        float absorbPercent = Math.min(1f, displayAbsorb / target.getMaxHealth());

        // Плавный цвет
        float colorTarget = 1f - hpTarget;
        displayedColorT += (colorTarget - displayedColorT) * Math.min(1f, delta * COLOR_LERP);

        int a = (int) (appearProgress * 255);
        int hpColor = lerpColor(0xFF44FF66, 0xFFFF3344, displayedColorT, a);

        int textColor = (a << 24) | 0xFFFFFF;

        // Лицо
        drawFlatFace(ctx, target, x + 8, y + 8, 28, a);

        // Имя
        ctx.drawTextWithShadow(mc.textRenderer, target.getName().getString(),
            x + 42, y + 10, textColor);

        // === HP BAR ===
        int barX = x + 42;
        int barY = y + 26;
        int barW = w - 52;
        int barH = 5;
        int radius = barH / 2;

        glassRounded(ctx, barX, barY, barW, barH, radius, (a / 3 << 24) | 0x000000);

        if (chipTarget > hpTarget + 0.001f) {
            int chipW = (int)(barW * chipTarget);
            glassRounded(ctx, barX, barY, chipW, barH, radius, (a << 24) | 0xFFFFFF);
        }

        if (hpTarget > 0.001f) {
            int hpW = (int)(barW * hpTarget);
            glassRounded(ctx, barX, barY, hpW, barH, radius, hpColor);
        }

        // Absorption — справа налево
        if (absorbPercent > 0.001f) {
            int absW = (int)(barW * absorbPercent);
            int absX = barX + barW - absW;
            int absColor = (255 << 24) | 0xFFDD33;
            if (absW > 0) {
                glassRounded(ctx, absX, barY, absW, barH, radius, absColor);
            }
        }

        // === HP число с сердечком ===
        renderHpNumber(ctx, mc, x + 42, y + 34, textColor, a);

        // Эффекты
        renderEffects(ctx, target, x + 8, y + 42, a);
    }

    // ============ HP ЧИСЛО + СЕРДЕЧКО ============
    private void renderHpNumber(DrawContext ctx, MinecraftClient mc, int px, int py, int textColor, int alpha) {
        // Округление HP к 0.5
        float rounded = Math.round(displayHealth * 2f) / 2f;
        String hpText;
        if (rounded == Math.floor(rounded)) {
            hpText = String.format("%.0f", rounded);
        } else {
            hpText = String.format("%.1f", rounded);
        }

        // Сердечко (ванильная иконка)
        ctx.setShaderColor(1f, 1f, 1f, alpha / 255f);
        ctx.drawTexture(ICONS, px, py, 52, 0, 9, 9, 256, 256);
        ctx.setShaderColor(1f, 1f, 1f, 1f);

        // Число
        ctx.drawTextWithShadow(mc.textRenderer, hpText, px + 12, py + 1, textColor);

        // Absorption +N
        if (displayAbsorb > 0.01f) {
            float absRound = Math.round(displayAbsorb * 2f) / 2f;
            String absText = "§e+" + (absRound == Math.floor(absRound)
                ? String.format("%.0f", absRound)
                : String.format("%.1f", absRound));
            int textW = mc.textRenderer.getWidth(hpText);
            ctx.drawTextWithShadow(mc.textRenderer, absText, px + 12 + textW + 6, py + 1, textColor);
        }
    }

    private void renderEffects(DrawContext ctx, LivingEntity entity, int startX, int startY, int alpha) {
        var effects = entity.getStatusEffects();
        if (effects.isEmpty()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        StatusEffectSpriteManager spriteManager = mc.getStatusEffectSpriteManager();

        int px = startX;
        int py = startY;
        int count = 0;
        int maxPerRow = 8;

        for (StatusEffectInstance eff : effects) {
            Sprite sprite = spriteManager.getSprite(eff.getEffectType());
            ctx.drawSprite(px, py, 0, 10, 10, sprite);

            int amp = eff.getAmplifier();
            if (amp > 0) {
                String lvl = String.valueOf(amp + 1);
                ctx.drawTextWithShadow(mc.textRenderer, lvl, px + 8, py + 4,
                    (alpha << 24) | 0xFFFFFF);
            }

            px += 12;
            count++;
            if (count % maxPerRow == 0) {
                px = startX;
                py += 12;
            }
        }
    }

    // ============ ЛИЦА ============
    private void drawFlatFace(DrawContext ctx, LivingEntity entity, int px, int py, int size, int alpha) {
        if (entity instanceof AbstractClientPlayerEntity player) {
            drawUv(ctx, player.getSkinTexture(), 64, 64, px, py, size, alpha);
            return;
        }

        if (entity instanceof VillagerEntity villager) {
            drawUv(ctx, VILLAGER_BASE, 64, 64, px, py, size, alpha);

            String typeId = Registries.VILLAGER_TYPE.getId(
                villager.getVillagerData().getType()).getPath();
            Identifier typeTex = new Identifier("minecraft",
                "textures/entity/villager/type/" + typeId + ".png");
            drawUv(ctx, typeTex, 64, 64, px, py, size, alpha);

            VillagerProfession prof = villager.getVillagerData().getProfession();
            if (prof != VillagerProfession.NONE) {
                String profId = Registries.VILLAGER_PROFESSION.getId(prof).getPath();
                Identifier profTex = new Identifier("minecraft",
                    "textures/entity/villager/profession/" + profId + ".png");
                drawUv(ctx, profTex, 64, 64, px, py, size, alpha);
            }
            return;
        }

        if (entity instanceof WanderingTraderEntity) {
            drawUv(ctx, new Identifier("minecraft",
                "textures/entity/wandering_trader.png"), 64, 64, px, py, size, alpha);
            return;
        }

        MobFace face = MOB_FACES.get(entity.getType());
        if (face != null) {
            drawUv(ctx, face.texture, face.texW, face.texH, px, py, size, alpha);
            return;
        }

        Item egg = findSpawnEgg(entity.getType());
        if (egg != null) {
            int off = (size - 16) / 2;
            ctx.setShaderColor(1f, 1f, 1f, alpha / 255f);
            ctx.drawItem(new ItemStack(egg), px + off, py + off);
            ctx.setShaderColor(1f, 1f, 1f, 1f);
            return;
        }

        int accent = 0xFF00D4FF;
        ctx.fill(px, py, px + size, py + size, (alpha << 24) | 0x1A1A2E);
        ctx.fill(px, py, px + size, py + 1, (alpha << 24) | (accent & 0xFFFFFF));
        ctx.fill(px, py + size - 1, px + size, py + size, (alpha << 24) | (accent & 0xFFFFFF));
        ctx.fill(px, py, px + 1, py + size, (alpha << 24) | (accent & 0xFFFFFF));
        ctx.fill(px + size - 1, py, px + size, py + size, (alpha << 24) | (accent & 0xFFFFFF));
    }

    private void drawUv(DrawContext ctx, Identifier tex, int texW, int texH,
                        int px, int py, int size, int alpha) {
        ctx.getMatrices().push();
        ctx.getMatrices().translate(px, py, 0);
        float scale = size / 8f;
        ctx.getMatrices().scale(scale, scale, 1f);
        ctx.setShaderColor(1f, 1f, 1f, alpha / 255f);
        ctx.drawTexture(tex, 0, 0, 8, 8, 8, 8, texW, texH);
        ctx.setShaderColor(1f, 1f, 1f, 1f);
        ctx.getMatrices().pop();
    }

    // ============ ХЕЛПЕРЫ ============
    private static void glassRounded(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0) return;
        r = Math.min(r, Math.min(w / 2, h / 2));
        if (r <= 0) {
            ctx.fill(x, y, x + w, y + h, color);
            return;
        }
        ctx.fill(x + r, y, x + w - r, y + h, color);
        ctx.fill(x, y + r, x + r, y + h - r, color);
        ctx.fill(x + w - r, y + r, x + w, y + h - r, color);

        int baseA = (color >>> 24) & 0xFF;
        int rgb = color & 0xFFFFFF;
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < r; j++) {
                float dx = r - i - 0.5f;
                float dy = r - j - 0.5f;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                float cov = r - dist + 0.5f;
                if (cov <= 0f) continue;
                if (cov > 1f) cov = 1f;
                int ca = (int)(baseA * cov);
                if (ca <= 0) continue;
                int c = (ca << 24) | rgb;
                ctx.fill(x + i, y + j, x + i + 1, y + j + 1, c);
                ctx.fill(x + w - 1 - i, y + j, x + w - i, y + j + 1, c);
                ctx.fill(x + i, y + h - 1 - j, x + i + 1, y + h - j, c);
                ctx.fill(x + w - 1 - i, y + h - 1 - j, x + w - i, y + h - j, c);
            }
        }
    }

    private static int lerpColor(int colorA, int colorB, float t, int alpha) {
        t = Math.max(0f, Math.min(1f, t));
        int rA = (colorA >> 16) & 0xFF, gA = (colorA >> 8) & 0xFF, bA = colorA & 0xFF;
        int rB = (colorB >> 16) & 0xFF, gB = (colorB >> 8) & 0xFF, bB = colorB & 0xFF;
        int r = (int)(rA + (rB - rA) * t);
        int g = (int)(gA + (gB - gA) * t);
        int b = (int)(bA + (bB - bA) * t);
        return (alpha << 24) | (r << 16) | (g << 8) | b;
    }

    private static Item findSpawnEgg(EntityType<?> type) {
        Identifier id = Registries.ENTITY_TYPE.getId(type);
        Identifier eggId = new Identifier(id.getNamespace(), id.getPath() + "_spawn_egg");
        Item item = Registries.ITEM.get(eggId);
        return item != Items.AIR ? item : null;
    }
}