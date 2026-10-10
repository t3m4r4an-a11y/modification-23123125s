package net.macos.client.hud.impl;

import net.macos.client.MacClient;
import net.macos.client.hud.glass.GlassRenderer;
import net.macos.client.hud.glass.GlassWidget;
import net.macos.client.hud.glass.PanelStyle;
import net.macos.client.gui.font.AetherionFont;
import net.macos.client.render.SquircleRenderer;
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
    private long lastFrameTime = System.currentTimeMillis();

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

    public static boolean hasActiveCombatTarget = false;

    @Override
    protected boolean shouldBeVisible() {
        if (MacClient.hudEditorOpen) return true;
        boolean active = target != null && target.isAlive()
            && System.currentTimeMillis() - lastHitTime <= 5000;
        hasActiveCombatTarget = active;
        return active;
    }

    @Override
    protected void configureStyle(PanelStyle s) {
        s.radius = 12;
        s.bgColor = 0xD80A0E18;
        s.borderColor = 0x35FFFFFF;
        s.specular = true;
        s.specularColor = 0x35FFFFFF;
        s.glowLayers = 1;
        s.glowColor = 0x3500D4FF;
        s.topAccent = false;
        s.gradientTop = 0x18FFFFFF;
        s.gradientBot = 0x20000000;
    }

    @Override
    protected void measure(float delta) {
        int effects = (target != null) ? target.getStatusEffects().size() : (MacClient.hudEditorOpen ? 1 : 0);
        int rows = (int) Math.ceil(effects / 8.0);
        this.w = 164;
        this.h = 48 + rows * 12;
    }

    @Override
    protected void renderInner(DrawContext ctx, int mouseX, int mouseY, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        LivingEntity active = target;
        boolean isPreview = false;

        if (active == null || !active.isAlive()) {
            if (MacClient.hudEditorOpen && mc.player != null) {
                active = mc.player;
                isPreview = true;
            } else {
                return;
            }
        }

        if (mc.world != null && !isPreview && target != null) {
            net.minecraft.entity.Entity fresh = mc.world.getEntityById(target.getId());
            if (fresh instanceof LivingEntity le) {
                active = le;
                target = le;
            }
        }

        float curHp = isPreview ? 15.0f : active.getHealth();
        float maxHp = isPreview ? 20.0f : active.getMaxHealth();
        float curAbs = isPreview ? 4.0f : active.getAbsorptionAmount();

        // Immediate sync on initial acquire or respawn
        if (displayHealth <= 0.05f && curHp > 0.05f) {
            displayHealth = curHp;
            chipHealth = curHp;
            displayAbsorb = curAbs;
            displayedColorT = 1f - Math.min(1f, curHp / maxHp);
        }

        // === Анимации (Framerate-independent exponential lerp) ===
        long now = System.currentTimeMillis();
        float dt = Math.max(0.001f, Math.min(0.08f, (now - lastFrameTime) / 1000f));
        lastFrameTime = now;

        float hpFactor = 1.0f - (float) Math.exp(-6.0 * dt);
        displayHealth += (curHp - displayHealth) * hpFactor;
        displayAbsorb += (curAbs - displayAbsorb) * hpFactor;

        // Trailing chip health animation
        float chipFactor = 1.0f - (float) Math.exp(-2.5 * dt);
        if (chipHealth < displayHealth - 0.02f) {
            chipHealth += (displayHealth - chipHealth) * hpFactor;
            if (Math.abs(chipHealth - displayHealth) < 0.02f) chipHealth = displayHealth;
        } else if (chipHealth > displayHealth) {
            chipHealth += (displayHealth - chipHealth) * chipFactor;
        }

        float hpTarget = maxHp > 0 ? Math.max(0, displayHealth / maxHp) : 0;
        float chipTarget = maxHp > 0 ? Math.max(0, chipHealth / maxHp) : 0;
        float absorbPercent = maxHp > 0 ? Math.min(1f, displayAbsorb / maxHp) : 0;

        // Плавный цвет
        float colorTarget = 1f - Math.min(1f, hpTarget);
        displayedColorT += (colorTarget - displayedColorT) * Math.min(1f, dt * COLOR_LERP * 4.0f);

        int a = (int) (appearProgress * 255);
        int hpColor = lerpColor(0xFF44FF66, 0xFFFF3344, displayedColorT, a);
        int textColor = (a << 24) | 0xFFFFFF;

        // Лицо в стеклянном бейдже
        int avSize = 30;
        int avX = x + 9, avY = y + 9;
        GlassRenderer.roundedRect(ctx, avX - 1, avY - 1, avSize + 2, avSize + 2, 7, ((int) (0x25 * (a / 255f)) << 24) | 0xFFFFFF);
        drawFlatFace(ctx, active, avX, avY, avSize, a);
        GlassRenderer.roundedBorder(ctx, avX - 1, avY - 1, avSize + 2, avSize + 2, 7, ((int) (0x35 * (a / 255f)) << 24) | 0xFFFFFF);

        // Имя
        String displayName = isPreview ? "Target Player" : active.getName().getString();
        AetherionFont.draw(ctx, displayName, x + 46, y + 10, textColor);

        // === HP BAR (8px Liquid Glass Bar) ===
        int barX = x + 46;
        int barY = y + 22;
        int barW = w - 54;
        int barH = 8;
        int radius = 3;

        // Dark track background
        GlassRenderer.roundedRect(ctx, barX, barY, barW, barH, radius, ((int) (0x95 * (a / 255f)) << 24) | 0x070B14);
        GlassRenderer.roundedBorder(ctx, barX, barY, barW, barH, radius, ((int) (0x35 * (a / 255f)) << 24) | 0xFFFFFF);

        // Chip damage bar (trailing damage)
        if (chipTarget > hpTarget + 0.001f) {
            int chipW = Math.max(radius * 2, (int)(barW * Math.min(1f, chipTarget)));
            GlassRenderer.roundedRect(ctx, barX, barY, chipW, barH, radius, ((int) (0xDD * (a / 255f)) << 24) | 0xFFAAAA);
        }

        // Active liquid health bar
        if (hpTarget > 0.001f) {
            int hpW = Math.max(radius * 2, (int)(barW * Math.min(1f, hpTarget)));
            GlassRenderer.roundedRect(ctx, barX, barY, hpW, barH, radius, hpColor);
            // Specular gloss strip along top half of the bar
            int glossA = (int) (0x55 * (a / 255f));
            if (glossA > 0 && hpW > radius * 2) {
                SquircleRenderer.fill(ctx, barX + 1, barY + 1, hpW - 2, 2, 1, (glossA << 24) | 0xFFFFFF);
            }
        }

        // Absorption — golden segment on the right
        if (absorbPercent > 0.001f) {
            int absW = Math.max(radius * 2, (int)(barW * Math.min(1f, absorbPercent)));
            int absX = barX + barW - absW;
            int absColor = (a << 24) | 0xFFD700;
            GlassRenderer.roundedRect(ctx, absX, barY, absW, barH, radius, absColor);
        }

        // === HP число с сердечком ===
        renderHpNumber(ctx, mc, x + 46, y + 33, textColor, a);

        // Эффекты
        if (!isPreview) {
            renderEffects(ctx, active, x + 8, y + 43, a);
        }
    }

    // ============ HP ЧИСЛО + СЕРДЕЧКО ============
    private void renderHpNumber(DrawContext ctx, MinecraftClient mc, int px, int py, int textColor, int alpha) {
        float rounded = Math.round(displayHealth * 10f) / 10f;
        String hpText = String.format(java.util.Locale.US, "%.1f", rounded);

        // Сердечко (ванильная иконка)
        try {
            ctx.setShaderColor(1f, 1f, 1f, alpha / 255f);
            ctx.drawTexture(ICONS, px, py, 52, 0, 9, 9, 256, 256);
        } finally {
            ctx.setShaderColor(1f, 1f, 1f, 1f);
        }

        // Число
        AetherionFont.draw(ctx, hpText, px + 12, py + 1, textColor);

        // Absorption +N
        if (displayAbsorb > 0.01f) {
            float absRound = Math.round(displayAbsorb * 2f) / 2f;
            String absText = "§e+" + (absRound == Math.floor(absRound)
                ? String.format("%.0f", absRound)
                : String.format("%.1f", absRound));
            int textW = AetherionFont.width(hpText);
            AetherionFont.draw(ctx, absText, px + 12 + textW + 6, py + 1, textColor);
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
                AetherionFont.draw(ctx, lvl, px + 8, py + 4,
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
        try {
            ctx.getMatrices().translate(px, py, 0);
            float scale = size / 8f;
            ctx.getMatrices().scale(scale, scale, 1f);
            ctx.setShaderColor(1f, 1f, 1f, alpha / 255f);
            ctx.drawTexture(tex, 0, 0, 8, 8, 8, 8, texW, texH);
        } finally {
            ctx.setShaderColor(1f, 1f, 1f, 1f);
            ctx.getMatrices().pop();
        }
    }

    // ============ ХЕЛПЕРЫ ============
    private static void glassRounded(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        GlassRenderer.roundedRect(ctx, x, y, w, h, r, color);
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