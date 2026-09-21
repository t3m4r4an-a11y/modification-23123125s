package net.macos.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.macos.client.MacClient;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File("config/macclient.json");

    public static ConfigManager INSTANCE = new ConfigManager();
    public static ConfigManager config = INSTANCE;

    // Визуал / Blur
    public int cornerRadius = 12;
    public String accentColor = "#00D4FF";

    // HUD Modules
    public boolean enableTargetHUD = true;
    public boolean enablePotionEffects = true;
    public boolean enableKeystrokes = true;
    public boolean enableComboCounter = true;

    // Watermark
    public boolean enableWatermark = true;
    public int watermarkX = 16;
    public int watermarkY = 16;

    // Keystrokes
    public boolean enableKeystrokesWidget = true;
    public int keystrokesWidgetX = 16;
    public int keystrokesWidgetY = 120;

    // Attack Cooldown
    public boolean enableAttackCooldown = true;
    public int attackCooldownOffsetX = 0;
    public int attackCooldownOffsetY = 0;
    public int attackCooldownRadius = 14;

    // Zoom
    public boolean enableZoom = true;
    public int zoomFov = 20;
    public int zoomSpeed = 20;

    // Fullbright
    public boolean enableFullbright = false;
    public int fullbrightGamma = 10;

    // NoFog
    public boolean enableNoFog = false;

    // Auto Sprint & Anti-AFK
    public boolean enableAutoSprint = false;
    public boolean enableAntiAFK = false;

    // Waypoints
    public boolean enableWaypoints = true;

    // HUD Editor
    public boolean hudEditorSnap = true;
    public int hudEditorGrid = 10;
    public boolean hudEditorShowGrid = true;

    // Hit Indicator
    public boolean enableHitIndicator = true;
    public int hitIndicatorRadius = 40;
    public int hitIndicatorDuration = 600;

    // Crosshair
    public boolean enableCustomCrosshair = true;
    public boolean hideVanillaCrosshair = true;
    public int crosshairStyle = 0;
    public int crosshairSize = 6;
    public int crosshairGap = 2;
    public int crosshairThickness = 1;
    public String crosshairColor = "#FFFFFF";
    public boolean crosshairDynamic = true;
    public int crosshairOffsetX = 0;
    public int crosshairOffsetY = 0;
    // Custom Chat
    public boolean enableCustomChat = true;
    // Potion HUD
    public boolean enablePotionHud = true;
    public boolean hideVanillaEffects = true;
    public int potionHudX = 5;
    public int potionHudY = 50;
    public int potionIconSize = 22;
    public int potionIconGap = 4;

    // Visuals & PvP
    public boolean enableGlassChams = false;
    public boolean enableCustomCritParticles = true;
    public boolean enableCustomKillEffects = true;

    // Armor HUD
    public boolean enableArmorBar = true;
    public boolean armorHudHorizontal = false;
    public boolean hideVanillaArmor = true;

    // View Model
    public boolean enableViewModel = false;
    public float vmOffsetX = 0f;
    public float vmOffsetY = 0f;
    public float vmOffsetZ = 0f;
    public float vmScale = 1f;
    public float vmRotateX = 0f;
    public float vmRotateY = 0f;
    public float vmRotateZ = 0f;

    // View Model — Off Hand
    public boolean enableOffHandViewModel = false;
    public float offOffsetX = 0f;
    public float offOffsetY = 0f;
    public float offOffsetZ = 0f;
    public float offScale = 1f;
    public float offRotateX = 0f;
    public float offRotateY = 0f;
    public float offRotateZ = 0f;

    // Free Look
    public boolean enableFreeLook = true;

    // Smooth Swing
    public boolean enableSmoothSwing = false;

    // Sword Block
    public boolean enableSwordBlock = false;

    // Toast
    public boolean enableCustomToasts = true;

    // Sound
    public boolean enableHitSound = true;
    public boolean enableKillSound = true;
    public String hitSoundPreset = "pop";
    public String killSoundPreset = "off";
    public float hitSoundVolume = 1.0f;
    public float hitSoundPitch = 1.0f;
    public boolean disableVanillaHitSound = true;

    // Swing Mode
    public String swingMode = "HORIZONTAL";

    // Block Animation (idle)
    public boolean enableBlockAnimation = false;
    public String blockAnimMode = "LEGACY";
    public float blockAnimX = 0f;
    public float blockAnimY = 0f;
    public float blockAnimZ = 0f;
    public float blockAnimScale = 1f;

    // Aspect Ratio
    public boolean enableAspectRatio = false;
    public float aspectRatio = 0.75f;

    // Target Indicator
    public boolean enableTargetIndicator = true;
    public int targetIndicatorSize = 64;
    public float targetIndicatorRotations = 1f;

    // Custom Scoreboard
    public boolean enableCustomScoreboard = true;

    // Позиции
    public int armorHudX = 10, armorHudY = 10;
    public int targetHudX = 10, targetHudY = 50;
    public int comboCounterX = 10, comboCounterY = 90;
    public int keystrokesX = 10, keystrokesY = 130;

    // Custom Tab
    public boolean enableCustomTabList = true;

    // Visuals
    public boolean removePunch = true;

    // HitFX
    public boolean enableHitFX = true;
    public String hitEffect = "spark";
    public String hitEffectColor = "#FFFFFF";

    public boolean enableCritFX = true;
    public String critEffect = "lightning";
    public String critEffectColor = "#FFFF00";

    // Kill Effect
    public boolean enableKillEffect = true;
    public String killEffectType = "ring";
    public String killEffectColor = "#FFD700";

    // Bloom
    public boolean enableBloom = true;
    public float bloomThreshold = 0.8f;
    public float bloomIntensity = 1.5f;

    // Blur
    public boolean enableGlassBlur = true;
    public int blurRadius = 12;
    public boolean enableInventoryBlur = true;
    public boolean enableChatBlur = true;

    // === ЕДИНАЯ СИСТЕМА ПОЗИЦИЙ ВИДЖЕТОВ ===
    public Map<String, WidgetPos> widgets = new HashMap<>();

    public WidgetPos getWidget(String key) {
        WidgetPos pos = widgets.get(key);
        if (pos == null) {
            pos = new WidgetPos(16, 16, true);
            widgets.put(key, pos);
        }
        return pos;
    }

    public static void save() {
        try {
            CONFIG_FILE.getParentFile().mkdirs();
            FileWriter writer = new FileWriter(CONFIG_FILE);
            GSON.toJson(INSTANCE, writer);
            writer.close();
        } catch (Exception e) {
            MacClient.LOGGER.error("Failed to save config", e);
        }
    }

    public void load() {
        try {
            if (CONFIG_FILE.exists()) {
                FileReader reader = new FileReader(CONFIG_FILE);
                ConfigManager loaded = GSON.fromJson(reader, ConfigManager.class);
                reader.close();
                if (loaded != null) {
                    INSTANCE = loaded;
                    config = loaded;
                }
            }

            if (INSTANCE.widgets == null) {
                INSTANCE.widgets = new HashMap<>();
            }

            // === ВАЛИДАЦИЯ ПОСЛЕ ЗАГРУЗКИ ===

            if (INSTANCE.swingMode == null
                || !(INSTANCE.swingMode.equals("DIAGONAL")
                  || INSTANCE.swingMode.equals("HORIZONTAL")
                  || INSTANCE.swingMode.equals("BACKHAND")
                  || INSTANCE.swingMode.equals("THRUST")
                  || INSTANCE.swingMode.equals("CHOP")
                  || INSTANCE.swingMode.equals("JAB"))) {
                INSTANCE.swingMode = "DIAGONAL";
            }
            if (INSTANCE.offScale <= 0f) INSTANCE.offScale = 1f;
            if (INSTANCE.vmScale <= 0f) INSTANCE.vmScale = 1f;

            if (INSTANCE.accentColor == null || INSTANCE.accentColor.isEmpty()) {
                INSTANCE.accentColor = "#00D4FF";
            }
            if (INSTANCE.crosshairColor == null || INSTANCE.crosshairColor.isEmpty()) {
                INSTANCE.crosshairColor = "#FFFFFF";
            }

            String[] hitPresets = {
                "aimbooster", "applepay", "bonk", "boykisser", "brick", "bring",
                "bump", "click", "coin", "glass", "hitsound", "magicsquash",
                "meow", "moan", "nya", "osu", "pop", "schoolboy", "skeet",
                "slap", "soft", "squash", "tf2crit", "tung", "uwu", "off"
            };
            boolean hitValid = false;
            for (String p : hitPresets) {
                if (p.equals(INSTANCE.hitSoundPreset)) { hitValid = true; break; }
            }
            if (!hitValid) INSTANCE.hitSoundPreset = "pop";

            boolean killValid = false;
            for (String p : hitPresets) {
                if (p.equals(INSTANCE.killSoundPreset)) { killValid = true; break; }
            }
            if (INSTANCE.killSoundPreset != null
                && INSTANCE.killSoundPreset.equals("rust")) {
                killValid = true;
            }
            if (!killValid) INSTANCE.killSoundPreset = "off";

            if (INSTANCE.hitSoundVolume < 0f) INSTANCE.hitSoundVolume = 0f;
            if (INSTANCE.hitSoundVolume > 2f) INSTANCE.hitSoundVolume = 2f;
            if (INSTANCE.hitSoundPitch < 0.5f) INSTANCE.hitSoundPitch = 0.5f;
            if (INSTANCE.hitSoundPitch > 2f) INSTANCE.hitSoundPitch = 2f;

            if (INSTANCE.aspectRatio < 0.5f) INSTANCE.aspectRatio = 0.5f;
            if (INSTANCE.aspectRatio > 2.0f) INSTANCE.aspectRatio = 2.0f;

            if (INSTANCE.killEffectType == null
                || !(INSTANCE.killEffectType.equals("ring")
                  || INSTANCE.killEffectType.equals("lightning")
                  || INSTANCE.killEffectType.equals("spiral")
                  || INSTANCE.killEffectType.equals("ghost")
                  || INSTANCE.killEffectType.equals("none"))) {
                INSTANCE.killEffectType = "ring";
            }

            // === МИГРАЦИЯ СТАРЫХ ПОЛЕЙ В widgets ===
            migrate("watermark",    INSTANCE.watermarkX,        INSTANCE.watermarkY,        INSTANCE.enableWatermark);
            migrate("keystrokes",   INSTANCE.keystrokesWidgetX, INSTANCE.keystrokesWidgetY, INSTANCE.enableKeystrokesWidget);
            migrate("armorHud",     INSTANCE.armorHudX,         INSTANCE.armorHudY,         INSTANCE.enableArmorBar);
            migrate("targetHud",    INSTANCE.targetHudX,        INSTANCE.targetHudY,        INSTANCE.enableTargetHUD);
            migrate("comboCounter", INSTANCE.comboCounterX,     INSTANCE.comboCounterY,     INSTANCE.enableComboCounter);
            migrate("potionHud",    INSTANCE.potionHudX,        INSTANCE.potionHudY,        INSTANCE.enablePotionHud);

        } catch (Exception e) {
            MacClient.LOGGER.error("Failed to load config", e);
        }
    }

    private void migrate(String key, int x, int y, boolean enabled) {
        if (!INSTANCE.widgets.containsKey(key)) {
            INSTANCE.widgets.put(key, new WidgetPos(x, y, enabled));
        }
    }
}