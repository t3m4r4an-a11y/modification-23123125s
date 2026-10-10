package net.macos.client.gui;

import net.macos.client.MacClient;
import net.macos.client.config.ConfigManager;
import net.macos.client.gui.anim.AnimationState;
import net.macos.client.gui.icon.IconRenderer;
import net.macos.client.gui.icon.MacIcons;
import net.macos.client.hud.glass.GlassRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.macos.client.gui.font.AetherionFont;
import net.macos.client.render.AetherionLogoRenderer;
import net.macos.client.render.GlassBackdrop;
import net.macos.client.render.SquircleRenderer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class MacClientMenu extends Screen implements BlurableScreen {

    // ============================================================
    // LAYOUT
    // ============================================================

    private static final int PANEL_W    = 560;
    private static final int PANEL_H    = 416;
    private static final int HEADER_H   = 50;
    private static final int OPT_ROW_H  = 42;
    private static final int OPT_LIST_TOP = HEADER_H + 42;
    private static final int LIST_SIDE_PADDING = 18;

    private static final int TAB_SIZE          = 28;
    private static final int TAB_GAP           = 6;
    private static final int DOCK_PAD_X        = 8;
    private static final int DOCK_PAD_Y        = 4;
    private static final int TABS_BAR_H        = TAB_SIZE + DOCK_PAD_Y * 2; // 36
    private static final int TABS_BOTTOM_MARGIN = 16;
    private static final int DETACHED_GAP      = 8;
    private static final int DETACHED_SIZE     = TABS_BAR_H;

    private static final int DROPDOWN_MAX_VISIBLE = 7;

    // ============================================================
    // PALETTE
    // ============================================================

    private static final int C_TEXT_PRIMARY   = 0xF5F5F7;
    private static final int C_TEXT_SECONDARY = 0xA1A1A6;
    private static final int C_TEXT_TERTIARY  = 0x7E7E84;
    private static final int C_ACCENT_FALLBACK = 0x00D4FF;

    private static final int C_PANEL_BG     = 0x65101624;
    private static final int C_PANEL_BORDER = 0x1EFFFFFF;
    private static final int C_SPECULAR     = 0x25FFFFFF;
    private static final int C_DIVIDER      = 0x12FFFFFF;

    private static final int C_CARD_BG      = 0x10FFFFFF;
    private static final int C_CARD_BORDER  = 0x15FFFFFF;
    private static final int C_ROW_HOVER    = 0x1CFFFFFF;
    private static final int C_TRACK        = 0x28FFFFFF;
    private static final int C_KNOB         = 0xF5F5F7;
    private static final int C_KNOB_SHADOW  = 0x60000000;

    // ============================================================
    // STATE
    // ============================================================

    private int panelX, panelY;
    private int selectedCategory = 0;
    private double scrollOffset = 0;
    private double maxScroll = 0;

    private boolean draggingPanel = false;
    private int dragOffX = 0, dragOffY = 0;

    private Option draggingSlider = null;
    private int sliderX = 0, sliderW = 0;

    private Option draggingVecOption = null;
    private int draggingVecAxis = 0;
    private double vecDragStartX = 0;
    private float vecDragStartVal = 0;

    private Option openDropdown = null;
    private int dropdownScroll = 0;

    private final List<Category> categories = new ArrayList<>();
    private final List<WidgetEntry> widgetEntries = new ArrayList<>();

    // Per-option animation cache
    private final Map<Option, OptionAnim> anims = new HashMap<>();

    // Panel open animation
    private final AnimationState panelOpen = new AnimationState(0f, 9f);

    // Tab selection pill animation
    private final AnimationState tabSelectionX = new AnimationState(0f, 16f);

    // Category page transition animation
    private final AnimationState categoryTransition = new AnimationState(1f, 16f);

    public MacClientMenu() {
        super(Text.literal("Aetherion"));
    }

    // ============================================================
    // INIT
    // ============================================================

    @Override
    protected void init() {
        super.init();
        MacClient.hudEditorOpen = true;
        panelX = (width - PANEL_W) / 2;
        panelY = (height - PANEL_H) / 2 - 10;
        scrollOffset = 0;

        categories.clear();
        widgetEntries.clear();
        anims.clear();

        panelOpen.snapTo(0f);
        panelOpen.setTarget(1f);
        tabSelectionX.snapTo(DOCK_PAD_X + selectedCategory * (TAB_SIZE + TAB_GAP));
        categoryTransition.snapTo(1f);

        // ===== GENERAL =====
        List<Option> general = new ArrayList<>();
        general.add(Option.bool("Glass Blur", "Размытие фона меню.",
                () -> ConfigManager.INSTANCE.enableGlassBlur,
                v -> ConfigManager.INSTANCE.enableGlassBlur = v));
        general.add(Option.slider("Corner Radius", "Скругление углов.",
                () -> ConfigManager.INSTANCE.cornerRadius,
                v -> ConfigManager.INSTANCE.cornerRadius = v, 0, 24, 1));
        general.add(Option.slider("Blur Radius", "Радиус размытия.",
                () -> ConfigManager.INSTANCE.blurRadius,
                v -> ConfigManager.INSTANCE.blurRadius = v, 10, 40, 1));
        general.add(Option.text("Accent Color", "Акцентный цвет (#RRGGBB).",
                () -> ConfigManager.INSTANCE.accentColor,
                v -> ConfigManager.INSTANCE.accentColor = v));
        general.add(Option.bool("Inventory Blur", "Размытие фона в инвентаре/сундуках.",
                () -> ConfigManager.INSTANCE.enableInventoryBlur,
                v -> ConfigManager.INSTANCE.enableInventoryBlur = v));
        general.add(Option.bool("Chat Blur", "Размытие фона чата.",
                () -> ConfigManager.INSTANCE.enableChatBlur,
                v -> ConfigManager.INSTANCE.enableChatBlur = v));
        general.add(Option.bool("Hotbar Highlight", "Акцентная подсветка выбранного слота хотбара.",
                () -> ConfigManager.INSTANCE.enableHotbarHighlight,
                v -> ConfigManager.INSTANCE.enableHotbarHighlight = v));
        categories.add(new Category("General", general));

        // ===== EDITOR =====
        List<Option> editor = new ArrayList<>();
        editor.add(Option.bool("Snap to Grid", "Прилипание виджетов к сетке.",
                () -> ConfigManager.INSTANCE.hudEditorSnap,
                v -> ConfigManager.INSTANCE.hudEditorSnap = v));
        editor.add(Option.slider("Grid Size", "Размер сетки в пикселях.",
                () -> ConfigManager.INSTANCE.hudEditorGrid,
                v -> ConfigManager.INSTANCE.hudEditorGrid = v, 2, 50, 1));
        editor.add(Option.bool("Show Grid", "Показывать сетку на фоне.",
                () -> ConfigManager.INSTANCE.hudEditorShowGrid,
                v -> ConfigManager.INSTANCE.hudEditorShowGrid = v));
        categories.add(new Category("Editor", editor));

        // ===== HUD =====
        List<Option> hud = new ArrayList<>();
        hud.add(Option.bool("Watermark", "Ник, FPS, пинг, время.",
                () -> ConfigManager.INSTANCE.enableWatermark && ConfigManager.INSTANCE.getWidget("watermark").enabled,
                v -> {
                    ConfigManager.INSTANCE.enableWatermark = v;
                    ConfigManager.INSTANCE.getWidget("watermark").enabled = v;
                }));
        hud.add(Option.bool("Keystrokes", "WASD + LMB/RMB.",
                () -> ConfigManager.INSTANCE.enableKeystrokesWidget && ConfigManager.INSTANCE.getWidget("keystrokes").enabled,
                v -> {
                    ConfigManager.INSTANCE.enableKeystrokesWidget = v;
                    ConfigManager.INSTANCE.getWidget("keystrokes").enabled = v;
                }));
        hud.add(Option.bool("Combo Counter", "Счётчик комбо.",
                () -> ConfigManager.INSTANCE.enableComboCounter && ConfigManager.INSTANCE.getWidget("comboCounter").enabled,
                v -> {
                    ConfigManager.INSTANCE.enableComboCounter = v;
                    ConfigManager.INSTANCE.getWidget("comboCounter").enabled = v;
                }));
        hud.add(Option.bool("Target HUD", "Инфо о цели.",
                () -> ConfigManager.INSTANCE.enableTargetHUD && ConfigManager.INSTANCE.getWidget("targetHud").enabled,
                v -> {
                    ConfigManager.INSTANCE.enableTargetHUD = v;
                    ConfigManager.INSTANCE.getWidget("targetHud").enabled = v;
                }));
        hud.add(Option.bool("Armor HUD", "Отображение экипированной брони и прочности.",
                () -> ConfigManager.INSTANCE.enableArmorBar && ConfigManager.INSTANCE.getWidget("armorHud").enabled,
                v -> {
                    ConfigManager.INSTANCE.enableArmorBar = v;
                    ConfigManager.INSTANCE.getWidget("armorHud").enabled = v;
                }));
        hud.add(Option.bool("Hide Vanilla Armor", "Убирает полосу брони снизу.",
                () -> ConfigManager.INSTANCE.hideVanillaArmor,
                v -> ConfigManager.INSTANCE.hideVanillaArmor = v));
        hud.add(Option.bool("Horizontal Armor", "Горизонтальный Armor HUD.",
                () -> ConfigManager.INSTANCE.armorHudHorizontal,
                v -> ConfigManager.INSTANCE.armorHudHorizontal = v));
        hud.add(Option.bool("Attack Cooldown", "Кружок атаки.",
                () -> ConfigManager.INSTANCE.enableAttackCooldown,
                v -> ConfigManager.INSTANCE.enableAttackCooldown = v));
        hud.add(Option.bool("Hit Indicator", "Дуга урона от атакующего.",
                () -> ConfigManager.INSTANCE.enableHitIndicator,
                v -> ConfigManager.INSTANCE.enableHitIndicator = v));
        hud.add(Option.slider("Hit Radius", "Радиус дуги урона.",
                () -> ConfigManager.INSTANCE.hitIndicatorRadius,
                v -> ConfigManager.INSTANCE.hitIndicatorRadius = v, 20, 140, 2));
        hud.add(Option.bool("Custom Crosshair", "Свой прицел.",
                () -> ConfigManager.INSTANCE.enableCustomCrosshair,
                v -> ConfigManager.INSTANCE.enableCustomCrosshair = v));
        hud.add(Option.dropdown("Crosshair Style", "Стиль прицела.",
                () -> ConfigManager.INSTANCE.crosshairStyle,
                v -> ConfigManager.INSTANCE.crosshairStyle = v,
                new String[]{"Cross", "Dot", "Circle", "Cross+Dot", "Chevron"}));
        hud.add(Option.slider("Crosshair Size", "Размер прицела.",
                () -> ConfigManager.INSTANCE.crosshairSize,
                v -> ConfigManager.INSTANCE.crosshairSize = v, 2, 20, 1));
        hud.add(Option.slider("Crosshair Gap", "Отступ от центра.",
                () -> ConfigManager.INSTANCE.crosshairGap,
                v -> ConfigManager.INSTANCE.crosshairGap = v, 0, 12, 1));
        hud.add(Option.slider("Crosshair Thickness", "Толщина линий.",
                () -> ConfigManager.INSTANCE.crosshairThickness,
                v -> ConfigManager.INSTANCE.crosshairThickness = v, 1, 4, 1));
        hud.add(Option.bool("Dynamic Crosshair", "Расширяется при замахе.",
                () -> ConfigManager.INSTANCE.crosshairDynamic,
                v -> ConfigManager.INSTANCE.crosshairDynamic = v));
        hud.add(Option.bool("Potion HUD", "Свой вид эффектов зелий.",
                () -> ConfigManager.INSTANCE.enablePotionHud && ConfigManager.INSTANCE.getWidget("potionHud").enabled,
                v -> {
                    ConfigManager.INSTANCE.enablePotionHud = v;
                    ConfigManager.INSTANCE.getWidget("potionHud").enabled = v;
                }));
        hud.add(Option.bool("Hide Vanilla Effects", "Убирает ванильные эффекты.",
                () -> ConfigManager.INSTANCE.hideVanillaEffects,
                v -> ConfigManager.INSTANCE.hideVanillaEffects = v));
        hud.add(Option.slider("Potion Size", "Размер иконки зелья.",
                () -> ConfigManager.INSTANCE.potionIconSize,
                v -> ConfigManager.INSTANCE.potionIconSize = v, 16, 32, 1));
        hud.add(Option.bool("Target Indicator", "Иконка над целью при ударе.",
                () -> ConfigManager.INSTANCE.enableTargetIndicator,
                v -> ConfigManager.INSTANCE.enableTargetIndicator = v));
        hud.add(Option.slider("Indicator Size", "Размер иконки (px).",
                () -> ConfigManager.INSTANCE.targetIndicatorSize,
                v -> ConfigManager.INSTANCE.targetIndicatorSize = v, 16, 200, 4));
        hud.add(Option.sliderFloat("Indicator Rotations", "Оборотов за анимацию.",
                () -> ConfigManager.INSTANCE.targetIndicatorRotations,
                v -> ConfigManager.INSTANCE.targetIndicatorRotations = v, 0f, 3f, 0.25f));
        hud.add(Option.bool("Jade HUD", "Инфо о блоке под прицелом.",
                () -> ConfigManager.INSTANCE.enableJadeHud && ConfigManager.INSTANCE.getWidget("jadeHud").enabled,
                v -> {
                    ConfigManager.INSTANCE.enableJadeHud = v;
                    ConfigManager.INSTANCE.getWidget("jadeHud").enabled = v;
                }));
        hud.add(Option.bool("Shulker Preview", "Предпросмотр содержимого шалкеров.",
                () -> ConfigManager.INSTANCE.enableShulkerPreview,
                v -> ConfigManager.INSTANCE.enableShulkerPreview = v));
        categories.add(new Category("HUD", hud));

        // ===== VISUALS =====
        List<Option> visuals = new ArrayList<>();
        visuals.add(Option.bool("Rain Droplets", "Стекающие капли дождя на экране.",
                () -> ConfigManager.INSTANCE.enableRainDroplets,
                v -> ConfigManager.INSTANCE.enableRainDroplets = v));
        visuals.add(Option.bool("Fullbright", "Освещение в темноте.",
                () -> ConfigManager.INSTANCE.enableFullbright,
                v -> ConfigManager.INSTANCE.enableFullbright = v));
        visuals.add(Option.bool("No Fog", "Убирает туман.",
                () -> ConfigManager.INSTANCE.enableNoFog,
                v -> ConfigManager.INSTANCE.enableNoFog = v));
        visuals.add(Option.bool("Glass Chams", "Прозрачные предметы в мире.",
                () -> ConfigManager.INSTANCE.enableGlassChams,
                v -> ConfigManager.INSTANCE.enableGlassChams = v));
        visuals.add(Option.bool("No Hurt Camera", "Убирает тряску при уроне.",
                () -> ConfigManager.INSTANCE.removePunch,
                v -> ConfigManager.INSTANCE.removePunch = v));
        visuals.add(Option.bool("Hit FX", "Партиклы при ударе.",
                () -> ConfigManager.INSTANCE.enableHitFX,
                v -> ConfigManager.INSTANCE.enableHitFX = v));
        visuals.add(Option.dropdown("Hit Effect", "Тип партикла.",
                () -> indexOfFX(ConfigManager.INSTANCE.hitEffect),
                v -> ConfigManager.INSTANCE.hitEffect = FX_NAMES[v],
                FX_LABELS));
        visuals.add(Option.text("Hit FX Color", "Цвет партикла (#RRGGBB).",
                () -> ConfigManager.INSTANCE.hitEffectColor,
                v -> ConfigManager.INSTANCE.hitEffectColor = v));
        visuals.add(Option.bool("Crit FX", "Партиклы при крите.",
                () -> ConfigManager.INSTANCE.enableCritFX,
                v -> ConfigManager.INSTANCE.enableCritFX = v));
        visuals.add(Option.dropdown("Crit Effect", "Тип партикла.",
                () -> indexOfFX(ConfigManager.INSTANCE.critEffect),
                v -> ConfigManager.INSTANCE.critEffect = FX_NAMES[v],
                FX_LABELS));
        visuals.add(Option.text("Crit FX Color", "Цвет партикла (#RRGGBB).",
                () -> ConfigManager.INSTANCE.critEffectColor,
                v -> ConfigManager.INSTANCE.critEffectColor = v));
        visuals.add(Option.dropdown("Kill Effect Type", "Тип эффекта при убийстве.",
                () -> {
                    String[] types = {"ring", "lightning", "spiral", "ghost", "beams", "burst", "none"};
                    for (int i = 0; i < types.length; i++) {
                        if (types[i].equalsIgnoreCase(ConfigManager.INSTANCE.killEffectType)) return i;
                    }
                    return 0;
                },
                v -> {
                    String[] types = {"ring", "lightning", "spiral", "ghost", "beams", "burst", "none"};
                    if (v >= 0 && v < types.length) ConfigManager.INSTANCE.killEffectType = types[v];
                },
                new String[]{"Ring", "Lightning", "Spiral", "Ghost", "Beams", "Burst", "OFF"}));
        visuals.add(Option.text("Kill Effect Color", "Цвет эффекта при убийстве (#RRGGBB).",
                () -> ConfigManager.INSTANCE.killEffectColor,
                v -> ConfigManager.INSTANCE.killEffectColor = v));
        visuals.add(Option.bool("Slash Trails", "Плавный шлейф за мечом при взмахе.",
                () -> ConfigManager.INSTANCE.enableSlashTrails,
                v -> ConfigManager.INSTANCE.enableSlashTrails = v));
        visuals.add(Option.text("Slash Trail Color", "Цвет шлейфа (#RRGGBB).",
                () -> ConfigManager.INSTANCE.slashTrailColor,
                v -> ConfigManager.INSTANCE.slashTrailColor = v));
        visuals.add(Option.bool("Hit Bubble", "3D светящийся бабл при ударе (HitFX).",
                () -> ConfigManager.INSTANCE.enableHitBubble,
                v -> ConfigManager.INSTANCE.enableHitBubble = v));
        visuals.add(Option.text("Hit Bubble Color", "Цвет бабла (#RRGGBB).",
                () -> ConfigManager.INSTANCE.hitBubbleColor,
                v -> ConfigManager.INSTANCE.hitBubbleColor = v));
        visuals.add(Option.bool("Jump Circle", "Неоновое кольцо под ногами при прыжке.",
                () -> ConfigManager.INSTANCE.enableJumpCircle,
                v -> ConfigManager.INSTANCE.enableJumpCircle = v));
        visuals.add(Option.text("Jump Circle Color", "Цвет кольца (#RRGGBB).",
                () -> ConfigManager.INSTANCE.jumpCircleColor,
                v -> ConfigManager.INSTANCE.jumpCircleColor = v));

        // Block Overlay
        visuals.add(Option.bool("Block Overlay", "Плавная неоновая подсветка целевого блока.",
                () -> ConfigManager.INSTANCE.enableBlockOverlay,
                v -> ConfigManager.INSTANCE.enableBlockOverlay = v));
        visuals.add(Option.dropdown("Overlay Style", "Стиль перемещения рамки блока.",
                () -> "Smooth".equalsIgnoreCase(ConfigManager.INSTANCE.blockOverlayMode) ? 0 : 1,
                v -> ConfigManager.INSTANCE.blockOverlayMode = (v == 0 ? "Smooth" : "Normal"),
                new String[]{"Smooth", "Normal"}));
        visuals.add(Option.dropdown("Overlay Pattern", "Шейдерный узор подсветки граней.",
                () -> switch (ConfigManager.INSTANCE.blockOverlayShader.toLowerCase()) {
                    case "cyber" -> 1;
                    case "fire" -> 2;
                    case "plasma" -> 3;
                    case "rainbow" -> 4;
                    case "pulse" -> 5;
                    case "glitch" -> 6;
                    case "normal" -> 7;
                    default -> 0;
                },
                v -> ConfigManager.INSTANCE.blockOverlayShader = switch (v) {
                    case 1 -> "Cyber";
                    case 2 -> "Fire";
                    case 3 -> "Plasma";
                    case 4 -> "Rainbow";
                    case 5 -> "Pulse";
                    case 6 -> "Glitch";
                    case 7 -> "Normal";
                    default -> "Aurora";
                },
                new String[]{"Aurora", "Cyber", "Fire", "Plasma", "Rainbow", "Pulse", "Glitch", "Normal"}));
        visuals.add(Option.bool("Overlay Outline", "Отображать контурные линии блока.",
                () -> ConfigManager.INSTANCE.blockOverlayOutline,
                v -> ConfigManager.INSTANCE.blockOverlayOutline = v));
        visuals.add(Option.sliderFloat("Overlay Line Width", "Толщина линий рамки блока.",
                () -> ConfigManager.INSTANCE.blockOverlayLineWidth,
                v -> ConfigManager.INSTANCE.blockOverlayLineWidth = v, 0.5f, 5.0f, 0.5f));
        visuals.add(Option.slider("Overlay Line Alpha", "Непрозрачность контурных линий.",
                () -> ConfigManager.INSTANCE.blockOverlayLineAlpha,
                v -> ConfigManager.INSTANCE.blockOverlayLineAlpha = (int) v, 20, 255, 5));
        visuals.add(Option.bool("Overlay Fill", "Заливать грани блока полупрозрачным цветом.",
                () -> ConfigManager.INSTANCE.blockOverlayFill,
                v -> ConfigManager.INSTANCE.blockOverlayFill = v));
        visuals.add(Option.slider("Overlay Fill Alpha", "Непрозрачность заполнения граней.",
                () -> ConfigManager.INSTANCE.blockOverlayFillAlpha,
                v -> ConfigManager.INSTANCE.blockOverlayFillAlpha = (int) v, 5, 255, 5));
        visuals.add(Option.text("Overlay Color", "Основной цвет подсветки (#RRGGBB).",
                () -> ConfigManager.INSTANCE.blockOverlayColor,
                v -> ConfigManager.INSTANCE.blockOverlayColor = v));

        visuals.add(Option.bool("Aspect Ratio", "Растянуть картинку по горизонтали.",
                () -> ConfigManager.INSTANCE.enableAspectRatio,
                v -> ConfigManager.INSTANCE.enableAspectRatio = v));
        visuals.add(Option.sliderFloat("Aspect Multiplier", "0.75=4:3, 1.0=Vanilla, 1.33=21:9.",
                () -> ConfigManager.INSTANCE.aspectRatio,
                v -> ConfigManager.INSTANCE.aspectRatio = v, 0.5f, 2.0f, 0.05f));

        // Item Physics (Phantom port)
        visuals.add(Option.bool("Item Physics", "Реалистичные лежащие предметы на земле и 3D вращение.",
                () -> ConfigManager.INSTANCE.enableItemPhysic,
                v -> ConfigManager.INSTANCE.enableItemPhysic = v));
        visuals.add(Option.bool("Item 3D Tumble", "Кувыркание предметов в воздухе при падении.",
                () -> ConfigManager.INSTANCE.itemPhysicRotate,
                v -> ConfigManager.INSTANCE.itemPhysicRotate = v));

        // Hit Hurt Color (Phantom port)
        visuals.add(Option.bool("Hit Hurt Color", "Кастомный цвет вспышки сущностей при получении урона.",
                () -> ConfigManager.INSTANCE.enableHitColor,
                v -> ConfigManager.INSTANCE.enableHitColor = v));
        visuals.add(Option.dropdown("Hit Color Mode", "Режим цвета вспышки при уроне.",
                () -> switch (ConfigManager.INSTANCE.hitColorMode.toLowerCase()) {
                    case "custom" -> 1;
                    case "rainbow" -> 2;
                    case "accent" -> 3;
                    case "red" -> 4;
                    case "golden" -> 5;
                    default -> 0;
                },
                v -> ConfigManager.INSTANCE.hitColorMode = switch (v) {
                    case 1 -> "Custom";
                    case 2 -> "Rainbow";
                    case 3 -> "Accent";
                    case 4 -> "Red";
                    case 5 -> "Golden";
                    default -> "White";
                },
                new String[]{"White", "Custom", "Rainbow", "Accent", "Red", "Golden"}));
        visuals.add(Option.text("Hit Custom Color", "Кастомный HEX цвет вспышки урона (#RRGGBB).",
                () -> ConfigManager.INSTANCE.hitColorHex,
                v -> ConfigManager.INSTANCE.hitColorHex = v));

        // World Modulation
        visuals.add(Option.bool("World Modulation", "Кастомная атмосфера мира: время, погода и цветовая гамма.",
                () -> ConfigManager.INSTANCE.enableWorldModulation,
                v -> ConfigManager.INSTANCE.enableWorldModulation = v));
        visuals.add(Option.dropdown("World Time", "Фиксация клиентского времени суток.",
                () -> switch (ConfigManager.INSTANCE.worldTime.toLowerCase()) {
                    case "day" -> 1;
                    case "sunset" -> 2;
                    case "midnight" -> 3;
                    default -> 0;
                },
                v -> ConfigManager.INSTANCE.worldTime = switch (v) {
                    case 1 -> "Day";
                    case 2 -> "Sunset";
                    case 3 -> "Midnight";
                    default -> "Default";
                },
                new String[]{"Default", "Day", "Sunset", "Midnight"}));
        visuals.add(Option.dropdown("World Weather", "Фиксация погоды на клиенте.",
                () -> switch (ConfigManager.INSTANCE.worldWeather.toLowerCase()) {
                    case "clear" -> 1;
                    case "rain" -> 2;
                    case "thunder" -> 3;
                    default -> 0;
                },
                v -> ConfigManager.INSTANCE.worldWeather = switch (v) {
                    case 1 -> "Clear";
                    case 2 -> "Rain";
                    case 3 -> "Thunder";
                    default -> "Default";
                },
                new String[]{"Default", "Clear", "Rain", "Thunder"}));
        visuals.add(Option.dropdown("World Atmosphere Tint", "Цветовая атмосфера тумана и освещения мира.",
                () -> switch (ConfigManager.INSTANCE.worldTint.toLowerCase()) {
                    case "cyberpunk" -> 1;
                    case "cold ice" -> 2;
                    case "deep dark" -> 3;
                    case "warm sunset" -> 4;
                    default -> 0;
                },
                v -> ConfigManager.INSTANCE.worldTint = switch (v) {
                    case 1 -> "Cyberpunk";
                    case 2 -> "Cold Ice";
                    case 3 -> "Deep Dark";
                    case 4 -> "Warm Sunset";
                    default -> "None";
                },
                new String[]{"None", "Cyberpunk", "Cold Ice", "Deep Dark", "Warm Sunset"}));
        visuals.add(Option.bool("Low Fire", "Опускает огонь от первого лица для лучшей видимости.",
                () -> ConfigManager.INSTANCE.enableLowFire,
                v -> ConfigManager.INSTANCE.enableLowFire = v));

        categories.add(new Category("Visuals", visuals));

        // ===== VIEWMODEL =====
        List<Option> viewmodel = new ArrayList<>();

        // Animations & Swings
        viewmodel.add(Option.bool("Smooth Swing", "Плавный замах оружия в стиле читов.",
                () -> ConfigManager.INSTANCE.enableSmoothSwing,
                v -> ConfigManager.INSTANCE.enableSmoothSwing = v));
        viewmodel.add(Option.bool("Sync Swing Cooldown", "Тайминг замаха по КД оружия (пол-кд удар, пол-кд возврат).",
                () -> ConfigManager.INSTANCE.syncSwingCooldown,
                v -> ConfigManager.INSTANCE.syncSwingCooldown = v));
        viewmodel.add(Option.dropdown("Swing Mode", "Стиль замаха и анимации оружия.",
                () -> switch (ConfigManager.INSTANCE.swingMode) {
                    case "HORIZONTAL" -> 1;
                    case "BACKHAND" -> 2;
                    case "THRUST" -> 3;
                    case "CHOP" -> 4;
                    case "JAB" -> 5;
                    case "SPIN" -> 6;
                    case "SWIPE" -> 7;
                    case "SWIPE_BACK" -> 8;
                    case "SWIPE_DOWN" -> 9;
                    case "BLOCKHIT_1_7" -> 10;
                    case "BLOCKHIT_1_8" -> 11;
                    case "SMOOTH" -> 12;
                    default -> 0;
                },
                v -> ConfigManager.INSTANCE.swingMode = switch (v) {
                    case 1 -> "HORIZONTAL";
                    case 2 -> "BACKHAND";
                    case 3 -> "THRUST";
                    case 4 -> "CHOP";
                    case 5 -> "JAB";
                    case 6 -> "SPIN";
                    case 7 -> "SWIPE";
                    case 8 -> "SWIPE_BACK";
                    case 9 -> "SWIPE_DOWN";
                    case 10 -> "BLOCKHIT_1_7";
                    case 11 -> "BLOCKHIT_1_8";
                    case 12 -> "SMOOTH";
                    default -> "DIAGONAL";
                },
                new String[]{
                        "DIAGONAL", "HORIZONTAL", "BACKHAND", "THRUST", "CHOP", "JAB",
                        "SPIN", "SWIPE", "SWIPE_BACK", "SWIPE_DOWN", "BLOCKHIT_1_7", "BLOCKHIT_1_8", "SMOOTH"
                }));

        // Chams & Glow & Wetness
        viewmodel.add(Option.bool("Hand Chams", "Стеклянные/светящиеся руки (1-е лицо).",
                () -> ConfigManager.INSTANCE.enableHandChams,
                v -> ConfigManager.INSTANCE.enableHandChams = v));
        viewmodel.add(Option.dropdown("Chams Mode", "Режим процедурного эффекта рук.",
                () -> switch (ConfigManager.INSTANCE.handChamsMode) {
                    case "Flat" -> 1;
                    case "Wireframe" -> 2;
                    case "Hologram" -> 3;
                    case "Rainbow" -> 4;
                    case "Cyberpunk" -> 5;
                    case "Gold" -> 6;
                    case "Waves" -> 7;
                    case "Plasma" -> 8;
                    case "Fire" -> 9;
                    case "Lightning" -> 10;
                    case "Aurora" -> 11;
                    case "Wetness" -> 12;
                    default -> 0; // Glass
                },
                v -> ConfigManager.INSTANCE.handChamsMode = switch (v) {
                    case 1 -> "Flat";
                    case 2 -> "Wireframe";
                    case 3 -> "Hologram";
                    case 4 -> "Rainbow";
                    case 5 -> "Cyberpunk";
                    case 6 -> "Gold";
                    case 7 -> "Waves";
                    case 8 -> "Plasma";
                    case 9 -> "Fire";
                    case 10 -> "Lightning";
                    case 11 -> "Aurora";
                    case 12 -> "Wetness";
                    default -> "Glass";
                },
                new String[]{"Glass", "Flat", "Wireframe", "Hologram", "Rainbow", "Cyberpunk", "Gold", "Waves", "Plasma", "Fire", "Lightning", "Aurora", "Wetness"}));
        viewmodel.add(Option.text("Hand Chams Color", "Основной цвет рук (#RRGGBB).",
                () -> ConfigManager.INSTANCE.handChamsColor,
                v -> ConfigManager.INSTANCE.handChamsColor = v));
        viewmodel.add(Option.text("Chams Color 2", "Второй цвет градиента (#RRGGBB).",
                () -> ConfigManager.INSTANCE.handChamsColor2,
                v -> ConfigManager.INSTANCE.handChamsColor2 = v));
        viewmodel.add(Option.sliderFloat("Hand Chams Alpha", "Прозрачность рук.",
                () -> ConfigManager.INSTANCE.handChamsAlpha,
                v -> ConfigManager.INSTANCE.handChamsAlpha = v, 0.1f, 1.0f, 0.05f));
        viewmodel.add(Option.bool("Hand Chams Glow", "Мягкое свечение и аура вокруг рук.",
                () -> ConfigManager.INSTANCE.handChamsGlow,
                v -> ConfigManager.INSTANCE.handChamsGlow = v));
        viewmodel.add(Option.sliderFloat("Glow Intensity", "Сила свечения/блума.",
                () -> ConfigManager.INSTANCE.handChamsGlowIntensity,
                v -> ConfigManager.INSTANCE.handChamsGlowIntensity = v, 0.2f, 3.0f, 0.1f));
        viewmodel.add(Option.sliderFloat("Pattern Intensity", "Яркость и насыщенность узора.",
                () -> ConfigManager.INSTANCE.handChamsShaderIntensity,
                v -> ConfigManager.INSTANCE.handChamsShaderIntensity = v, 0.1f, 3.0f, 0.1f));
        viewmodel.add(Option.bool("Rainbow Chams", "Радужный RGB перелив свечения.",
                () -> ConfigManager.INSTANCE.handChamsRainbow,
                v -> ConfigManager.INSTANCE.handChamsRainbow = v));
        viewmodel.add(Option.bool("Item Chams", "Стеклянный/светящийся предмет в руке.",
                () -> ConfigManager.INSTANCE.enableItemChams,
                v -> ConfigManager.INSTANCE.enableItemChams = v));
        viewmodel.add(Option.bool("Model Wetness", "Реалистичные капли воды и глянец на руках и оружии.",
                () -> ConfigManager.INSTANCE.enableModelWetness,
                v -> ConfigManager.INSTANCE.enableModelWetness = v));

        // Compact Vector3 Coordinate Controls
        viewmodel.add(Option.bool("Main Hand VM", "Кастомные координаты правой руки.",
                () -> ConfigManager.INSTANCE.enableViewModel,
                v -> ConfigManager.INSTANCE.enableViewModel = v));
        viewmodel.add(Option.vector3("Main Offset [X,Y,Z]", "Смещение правой руки (drag/scroll).",
                () -> ConfigManager.INSTANCE.vmOffsetX, v -> ConfigManager.INSTANCE.vmOffsetX = v,
                () -> ConfigManager.INSTANCE.vmOffsetY, v -> ConfigManager.INSTANCE.vmOffsetY = v,
                () -> ConfigManager.INSTANCE.vmOffsetZ, v -> ConfigManager.INSTANCE.vmOffsetZ = v,
                -2f, 2f, 0.02f));
        viewmodel.add(Option.vector3("Main Rotation [X,Y,Z]", "Поворот правой руки по осям (deg).",
                () -> ConfigManager.INSTANCE.vmRotateX, v -> ConfigManager.INSTANCE.vmRotateX = v,
                () -> ConfigManager.INSTANCE.vmRotateY, v -> ConfigManager.INSTANCE.vmRotateY = v,
                () -> ConfigManager.INSTANCE.vmRotateZ, v -> ConfigManager.INSTANCE.vmRotateZ = v,
                -180f, 180f, 1.0f));
        viewmodel.add(Option.sliderFloat("Main Scale", "Масштаб правой руки.",
                () -> ConfigManager.INSTANCE.vmScale,
                v -> ConfigManager.INSTANCE.vmScale = v, 0.1f, 3.0f, 0.05f));

        viewmodel.add(Option.bool("Off Hand VM", "Кастомные координаты левой руки.",
                () -> ConfigManager.INSTANCE.enableOffHandViewModel,
                v -> ConfigManager.INSTANCE.enableOffHandViewModel = v));
        viewmodel.add(Option.vector3("Off Offset [X,Y,Z]", "Смещение левой руки (drag/scroll).",
                () -> ConfigManager.INSTANCE.offOffsetX, v -> ConfigManager.INSTANCE.offOffsetX = v,
                () -> ConfigManager.INSTANCE.offOffsetY, v -> ConfigManager.INSTANCE.offOffsetY = v,
                () -> ConfigManager.INSTANCE.offOffsetZ, v -> ConfigManager.INSTANCE.offOffsetZ = v,
                -2f, 2f, 0.02f));
        viewmodel.add(Option.vector3("Off Rotation [X,Y,Z]", "Поворот левой руки по осям (deg).",
                () -> ConfigManager.INSTANCE.offRotateX, v -> ConfigManager.INSTANCE.offRotateX = v,
                () -> ConfigManager.INSTANCE.offRotateY, v -> ConfigManager.INSTANCE.offRotateY = v,
                () -> ConfigManager.INSTANCE.offRotateZ, v -> ConfigManager.INSTANCE.offRotateZ = v,
                -180f, 180f, 1.0f));
        viewmodel.add(Option.sliderFloat("Off Scale", "Масштаб левой руки.",
                () -> ConfigManager.INSTANCE.offScale,
                v -> ConfigManager.INSTANCE.offScale = v, 0.1f, 3.0f, 0.05f));

        viewmodel.add(Option.bool("Free Look (Alt)", "Осмотр камерой при зажатом Alt.",
                () -> ConfigManager.INSTANCE.enableFreeLook,
                v -> ConfigManager.INSTANCE.enableFreeLook = v));
        categories.add(new Category("ViewModel", viewmodel));

        // ===== MISC =====
        List<Option> misc = new ArrayList<>();
        misc.add(Option.bool("Zoom", "Приближение (C).",
                () -> ConfigManager.INSTANCE.enableZoom,
                v -> ConfigManager.INSTANCE.enableZoom = v));
        misc.add(Option.slider("Zoom FOV", "FOV при зуме.",
                () -> ConfigManager.INSTANCE.zoomFov,
                v -> ConfigManager.INSTANCE.zoomFov = v, 5, 50, 1));
        misc.add(Option.slider("Zoom Speed", "Скорость зума.",
                () -> ConfigManager.INSTANCE.zoomSpeed,
                v -> ConfigManager.INSTANCE.zoomSpeed = v, 5, 100, 5));
        misc.add(Option.bool("Hit Sound", "Звук при ударе.",
                () -> ConfigManager.INSTANCE.enableHitSound,
                v -> ConfigManager.INSTANCE.enableHitSound = v));
        misc.add(Option.dropdown("Hit Sound Preset", "Пресет звука удара.",
                () -> indexOfHit(ConfigManager.INSTANCE.hitSoundPreset),
                v -> ConfigManager.INSTANCE.hitSoundPreset = HIT_PRESETS[v],
                HIT_LABELS));
        misc.add(Option.bool("Kill Sound", "Звук при убийстве.",
                () -> ConfigManager.INSTANCE.enableKillSound,
                v -> ConfigManager.INSTANCE.enableKillSound = v));
        misc.add(Option.dropdown("Kill Sound Preset", "Пресет звука убийства.",
                () -> indexOfHit(ConfigManager.INSTANCE.killSoundPreset),
                v -> ConfigManager.INSTANCE.killSoundPreset = HIT_PRESETS[v],
                HIT_LABELS));
        misc.add(Option.sliderFloat("Sound Volume", "Общая громкость звуков.",
                () -> ConfigManager.INSTANCE.hitSoundVolume,
                v -> ConfigManager.INSTANCE.hitSoundVolume = v, 0f, 2f, 0.05f));
        misc.add(Option.sliderFloat("Sound Pitch", "Общий питч звуков.",
                () -> ConfigManager.INSTANCE.hitSoundPitch,
                v -> ConfigManager.INSTANCE.hitSoundPitch = v, 0.5f, 2f, 0.05f));
        misc.add(Option.bool("Auto Sprint", "Автоспринт.",
                () -> ConfigManager.INSTANCE.enableAutoSprint,
                v -> ConfigManager.INSTANCE.enableAutoSprint = v));
        misc.add(Option.bool("Anti-AFK", "Против AFK-кика.",
                () -> ConfigManager.INSTANCE.enableAntiAFK,
                v -> ConfigManager.INSTANCE.enableAntiAFK = v));
        misc.add(Option.bool("Waypoints", "Метки в мире.",
                () -> ConfigManager.INSTANCE.enableWaypoints,
                v -> ConfigManager.INSTANCE.enableWaypoints = v));
        misc.add(Option.button("Resource Packs", "Открыть менеджер пакетов ресурсов.", () -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            mc.setScreen(new net.minecraft.client.gui.screen.pack.PackScreen(
                    mc.getResourcePackManager(),
                    manager -> {
                        var oldPacks = com.google.common.collect.ImmutableList.copyOf(mc.options.resourcePacks);
                        mc.options.refreshResourcePacks(manager);
                        var newPacks = com.google.common.collect.ImmutableList.copyOf(mc.options.resourcePacks);
                        if (!newPacks.equals(oldPacks)) {
                            mc.reloadResources();
                        }
                        mc.setScreen(new MacClientMenu());
                    },
                    mc.getResourcePackDir(),
                    net.minecraft.text.Text.translatable("resourcePack.title")
            ));
        }));
        categories.add(new Category("Misc", misc));

        updateMaxScroll();
    }

    @Override
    public void removed() {
        super.removed();
        MacClient.hudEditorOpen = false;
    }

    // ============================================================
    // LAYOUT MATH
    // ============================================================

    private void updateMaxScroll() {
        Category cat = categories.get(selectedCategory);
        int visibleHeight = PANEL_H - OPT_LIST_TOP - 20;
        int contentHeight = cat.options.size() * OPT_ROW_H;
        maxScroll = Math.max(0, contentHeight - visibleHeight);
    }

    private int getCategoriesWidth() {
        return categories.size() * TAB_SIZE + (categories.size() - 1) * TAB_GAP;
    }

    private int getMainDockWidth() {
        return getCategoriesWidth() + DOCK_PAD_X * 2;
    }

    private int getTabsBarTotalWidth() {
        return getMainDockWidth() + DETACHED_GAP + DETACHED_SIZE;
    }

    private int getTabsBarX() {
        return (width - getTabsBarTotalWidth()) / 2;
    }

    private int getTabsBarY() {
        return height - TABS_BOTTOM_MARGIN - TABS_BAR_H;
    }

    private OptionAnim anim(Option opt) {
        return anims.computeIfAbsent(opt, k -> new OptionAnim());
    }

    // ============================================================
    // BLUR REGIONS
    // ============================================================

    @Override
    public List<int[]> getBlurRegions() {
        List<int[]> list = new ArrayList<>();
        list.add(new int[]{ panelX - 12, panelY - 12, PANEL_W + 24, PANEL_H + 24 });
        list.add(new int[]{
                getTabsBarX() - 10,
                getTabsBarY() - 8,
                getTabsBarTotalWidth() + 20,
                TABS_BAR_H + 16
        });
        return list;
    }

    // ============================================================
    // RENDER
    // ============================================================

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int accent = parseAccent();

        // Panel open animation
        float openT = panelOpen.tick(delta);
        float scale = 0.94f + 0.06f * easeOutCubic(openT);
        int   slideY = (int) ((1f - openT) * 10f);
        int   openAlpha = (int) (openT * 255);

        // --- 2026 Ethereal Chromatic Background Mesh Shader ---
        net.macos.client.render.MenuShaderRenderer.render(openT, accent);

        // --- HUD editor grid ---
        if (ConfigManager.INSTANCE.hudEditorShowGrid) {
            int grid = Math.max(4, ConfigManager.INSTANCE.hudEditorGrid);
            int gridColor = (0x12 << 24) | 0xFFFFFF;
            for (int x = 0; x < width; x += grid) ctx.fill(x, 0, x + 1, height, gridColor);
            for (int y = 0; y < height; y += grid) ctx.fill(0, y, width, y + 1, gridColor);
        }

        // --- Panel with scale + slide ---
        ctx.getMatrices().push();
        float cx = panelX + PANEL_W / 2f;
        float cy = panelY + PANEL_H / 2f;
        ctx.getMatrices().translate(cx, cy + slideY, 0);
        ctx.getMatrices().scale(scale, scale, 1f);
        ctx.getMatrices().translate(-cx, -cy, 0);

        drawPanel(ctx, mouseX, mouseY, accent, openAlpha);
        drawOptionsList(ctx, mouseX, mouseY, delta, accent, openAlpha);

        ctx.getMatrices().pop();

        // --- Dropdown (rendered outside panel transform so it can overflow) ---
        if (openDropdown != null) {
            drawOpenDropdown(ctx, mouseX, mouseY, delta, accent);
        }

        // --- Tab dock (bottom, no scale) ---
        drawTabDock(ctx, mouseX, mouseY, delta, accent, openT);

        // --- HUD editor widgets (only when on Editor tab) ---
        boolean isEditorTab = categories.get(selectedCategory).name.equalsIgnoreCase("editor");
        MacClient.hudEditorOpen = isEditorTab;
        if (isEditorTab) {
            boolean mouseDown = org.lwjgl.glfw.GLFW.glfwGetMouseButton(
                    MinecraftClient.getInstance().getWindow().getHandle(),
                    org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT
            ) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
            net.macos.client.hud.WidgetManager.INSTANCE.renderAll(
                    ctx, mouseX, mouseY, delta, mouseDown
            );
        }

        ctx.draw();
    }

    // ============================================================
    // PANEL (header, background, buttons)
    // ============================================================

    private void drawPanel(DrawContext ctx, int mouseX, int mouseY, int accent, int openAlpha) {
        int px = panelX;
        int py = panelY;

        // Ambient deep drop shadow for window
        GlassRenderer.dropShadow(ctx, px, py, PANEL_W, PANEL_H, 20, 14,
                (int) (0x65 * (openAlpha / 255f)));

        // Glass background with blur backdrop sample
        if (ConfigManager.INSTANCE.enableGlassBlur) {
            GlassBackdrop.draw(ctx, px + 2, py + 2, PANEL_W - 4, PANEL_H - 4);
        }

        // Tinted glass acrylic fill
        int bgAlpha = (int) (0x75 * (openAlpha / 255f));
        drawRoundRect(ctx, px, py, PANEL_W, PANEL_H, 20,
                (bgAlpha << 24) | 0x101624);

        // Specular top highlight (macOS glass rim)
        GlassRenderer.specular(ctx, px, py, PANEL_W, PANEL_H, 20,
                ((int) (0x2A * (openAlpha / 255f)) << 24) | 0xFFFFFF);

        // Thin rounded border
        GlassRenderer.roundedBorder(ctx, px, py, PANEL_W, PANEL_H, 20,
                ((int) (0x22 * (openAlpha / 255f)) << 24) | 0xFFFFFF);

        // Subtle accent glow rim
        GlassRenderer.roundedBorder(ctx, px, py, PANEL_W, PANEL_H, 20,
                ((int) (0x18 * (openAlpha / 255f)) << 24) | accent);

        // --- macOS Traffic Lights ---
        int redX = px + 20, yellowX = px + 36, greenX = px + 52;
        int lightY = py + 18;
        boolean trafficHover = mouseX >= px + 12 && mouseX <= px + 62 && mouseY >= py + 10 && mouseY <= py + 26;

        drawCircle(ctx, redX, lightY, 5, (openAlpha << 24) | 0xFF5F56);
        drawCircle(ctx, yellowX, lightY, 5, (openAlpha << 24) | 0xFFBD2E);
        drawCircle(ctx, greenX, lightY, 5, (openAlpha << 24) | 0x27C93F);

        if (trafficHover) {
            AetherionFont.draw(ctx, "x", redX - 2, lightY - 4, (openAlpha << 24) | 0x804D0000);
            AetherionFont.draw(ctx, "-", yellowX - 2, lightY - 4, (openAlpha << 24) | 0x80995700);
            AetherionFont.draw(ctx, "+", greenX - 2, lightY - 4, (openAlpha << 24) | 0x80006500);
        }

        // Vertical divider after traffic lights
        ctx.fill(px + 68, py + 12, px + 69, py + 24, ((int) (0x1A * (openAlpha / 255f)) << 24) | 0xFFFFFF);

        // --- Logo Emblem + "AETHERION" ---
        int logoSize = 16;
        int logoX = px + 80;
        int logoY = py + 10;
        AetherionLogoRenderer.drawChromatic(ctx, logoX, logoY, logoSize, openAlpha / 255.0f);

        int titleX = logoX + logoSize + 6;
        AetherionFont.draw(ctx, "AETHERION", titleX, py + 13, (openAlpha << 24) | accent);

        // Version pill badge
        int verX = titleX + AetherionFont.width("AETHERION") + 8;
        int verW = AetherionFont.width("2026") + 10;
        drawRoundRect(ctx, verX, py + 11, verW, 14, 4, ((int) (0x22 * (openAlpha / 255f)) << 24) | accent);
        AetherionFont.draw(ctx, "2026", verX + 5, py + 14, (openAlpha << 24) | 0xFFFFFF);

        // Active status indicator with pulsing dot
        int statX = verX + verW + 8;
        drawCircle(ctx, statX + 3, py + 18, 3, (openAlpha << 24) | 0x4ADE80);
        AetherionFont.draw(ctx, "Active", statX + 10, py + 14, (openAlpha << 24) | 0x4ADE80);

        // --- Reset button ---
        int resetW = 52;
        int resetH = 20;
        int resetX = px + PANEL_W - 22 - resetW - 28;
        int resetY = py + 11;
        boolean resetHover = mouseX >= resetX && mouseX <= resetX + resetW
                && mouseY >= resetY && mouseY <= resetY + resetH;
        if (resetHover) {
            drawRoundRect(ctx, resetX, resetY, resetW, resetH, 6,
                    ((int) (0x24 * (openAlpha / 255f)) << 24) | 0xFFFFFF);
        }
        drawBorderRounded(ctx, resetX, resetY, resetW, resetH, 6,
                ((int) (0x1A * (openAlpha / 255f)) << 24) | 0xFFFFFF);
        String resetText = "Reset";
        int resetTextW = AetherionFont.width(resetText);
        AetherionFont.draw(ctx, resetText,
                resetX + (resetW - resetTextW) / 2, resetY + 6,
                (openAlpha << 24) | (resetHover ? C_TEXT_PRIMARY : C_TEXT_SECONDARY));

        // --- Close button ---
        int closeSize = 20;
        int closeX = px + PANEL_W - 22 - closeSize;
        int closeY = py + 11;
        boolean closeHover = mouseX >= closeX && mouseX <= closeX + closeSize
                && mouseY >= closeY && mouseY <= closeY + closeSize;
        if (closeHover) {
            drawRoundRect(ctx, closeX, closeY, closeSize, closeSize, 6,
                    (openAlpha << 24) | 0x35FF4455);
        }
        drawBorderRounded(ctx, closeX, closeY, closeSize, closeSize, 6,
                ((int) (0x1A * (openAlpha / 255f)) << 24) | 0xFFFFFF);
        IconRenderer.draw(ctx, MacIcons.CLOSE,
                closeX + 4, closeY + 5,
                (openAlpha << 24) | (closeHover ? 0xFF4455 : C_TEXT_SECONDARY));

        // --- Divider under header ---
        ctx.fill(px + 16, py + HEADER_H, px + PANEL_W - 16, py + HEADER_H + 1,
                ((int) (0x12 * (openAlpha / 255f)) << 24) | 0xFFFFFF);

        // --- Category Banner ---
        Category current = categories.get(selectedCategory);
        int catBoxX = px + 20;
        int catBoxY = py + HEADER_H + 8;
        int catBoxSize = 26;

        drawRoundRect(ctx, catBoxX, catBoxY, catBoxSize, catBoxSize, 7,
                (0x28 << 24) | accent);
        drawBorderRounded(ctx, catBoxX, catBoxY, catBoxSize, catBoxSize, 7,
                (0x40 << 24) | accent);
        IconRenderer.draw(ctx, getCategoryIcon(current.name),
                catBoxX + 5, catBoxY + 5, (openAlpha << 24) | accent);

        // Category title and description
        AetherionFont.draw(ctx, current.name,
                catBoxX + catBoxSize + 10, catBoxY + 3, (openAlpha << 24) | C_TEXT_PRIMARY);
        AetherionFont.draw(ctx, getCategoryDescription(current.name),
                catBoxX + catBoxSize + 10, catBoxY + 14, (openAlpha << 24) | C_TEXT_TERTIARY);

        // Count badge
        String countText = current.options.size() + " options";
        int countW = AetherionFont.width(countText);
        int badgeW = countW + 16;
        int badgeX = px + PANEL_W - 20 - badgeW;
        drawRoundRect(ctx, badgeX, catBoxY + 4, badgeW, 18, 9,
                ((int) (0x12 * (openAlpha / 255f)) << 24) | 0xFFFFFF);
        drawBorderRounded(ctx, badgeX, catBoxY + 4, badgeW, 18, 9,
                ((int) (0x16 * (openAlpha / 255f)) << 24) | 0xFFFFFF);
        AetherionFont.draw(ctx, countText,
                badgeX + 8, catBoxY + 8,
                (openAlpha << 24) | C_TEXT_SECONDARY);
    }

    // ============================================================
    // OPTIONS LIST
    // ============================================================

    private void drawOptionsList(DrawContext ctx, int mouseX, int mouseY,
                                 float delta, int accent, int openAlpha) {
        int px = panelX;
        int py = panelY;
        Category current = categories.get(selectedCategory);

        int listLeft = px + LIST_SIDE_PADDING;
        int listRight = px + PANEL_W - LIST_SIDE_PADDING;
        int listTop = py + OPT_LIST_TOP;
        int listBottom = py + PANEL_H - 16;
        int rowW = listRight - listLeft;

        float catT = categoryTransition.tick(delta);
        float catEase = easeOutCubic(catT);
        int   catSlideY = (int) ((1f - catEase) * 10f);
        int   catAlpha = (int) (openAlpha * catEase);

        Option hoveredOption = null;
        ctx.enableScissor(px + 12, listTop - 4, px + PANEL_W - 12, listBottom);
        try {

        for (int i = 0; i < current.options.size(); i++) {
            Option opt = current.options.get(i);
            int oy = listTop + i * OPT_ROW_H - (int) scrollOffset + catSlideY;

            if (oy + OPT_ROW_H < listTop - 4) continue;
            if (oy > listBottom) break;

            OptionAnim anim = anim(opt);
            if (anim.firstFrame) {
                anim.firstFrame = false;
                if (opt.isBool) anim.toggle.snapTo((boolean) opt.get() ? 1f : 0f);
                if (opt.isSlider) {
                    float v = ((Number) opt.get()).floatValue();
                    float min = opt.isFloat ? opt.floatMin : opt.min;
                    float max = opt.isFloat ? opt.floatMax : opt.max;
                    anim.slider.snapTo(clamp01((v - min) / (max - min)));
                }
            }

            boolean hover = mouseX >= listLeft && mouseX <= listRight
                    && mouseY >= oy && mouseY < oy + OPT_ROW_H
                    && mouseY >= listTop && mouseY <= listBottom;
            if (hover) hoveredOption = opt;

            anim.hover.setTarget(hover ? 1f : 0f);
            float hoverT = anim.hover.tick(delta);

            int cardX = listLeft;
            int cardY = oy + 2;
            int cardW = rowW;
            int cardH = OPT_ROW_H - 4;

            // Card background & smooth hover
            int cardBg = lerpColor(0x0CFFFFFF, 0x22FFFFFF, hoverT);
            int bgA = (int) ((cardBg >>> 24) * (catAlpha / 255f));
            drawRoundRect(ctx, cardX, cardY, cardW, cardH, 8, (bgA << 24) | (cardBg & 0xFFFFFF));
            int cardBorder = lerpColor(0x10FFFFFF, (0x35 << 24) | accent, hoverT);
            int borderA = (int) ((cardBorder >>> 24) * (catAlpha / 255f));
            drawBorderRounded(ctx, cardX, cardY, cardW, cardH, 8, (borderA << 24) | (cardBorder & 0xFFFFFF));

            // Option Icon badge
            int iconBoxX = cardX + 6;
            int iconBoxY = cardY + 5;
            drawRoundRect(ctx, iconBoxX, iconBoxY, 24, 24, 6, ((int) (0x15 * (catAlpha / 255f)) << 24) | 0xFFFFFF);
            drawBorderRounded(ctx, iconBoxX, iconBoxY, 24, 24, 6, ((int) (0x18 * (catAlpha / 255f)) << 24) | 0xFFFFFF);
            String optIcon = getOptionIcon(opt.name, current.name);
            int optIconW = IconRenderer.width(optIcon);
            IconRenderer.draw(ctx, optIcon, iconBoxX + (24 - optIconW) / 2, iconBoxY + 4,
                    (catAlpha << 24) | (hoverT > 0.05f ? accent : C_TEXT_SECONDARY));

            // Option label & description
            int textX = cardX + 36;
            int nameColor = blend(C_TEXT_PRIMARY, 0xFFFFFFFF, hoverT);
            AetherionFont.draw(ctx, opt.name, textX, cardY + 6,
                    (catAlpha << 24) | nameColor);
            String desc = opt.description != null ? opt.description : "";
            AetherionFont.draw(ctx, desc, textX, cardY + 18,
                    (catAlpha << 24) | C_TEXT_TERTIARY);

            // Control
            int ctrlRight = listRight - 6;
            if (opt.isBool) {
                drawToggle(ctx, opt, anim, ctrlRight, oy, delta, accent, catAlpha);
            } else if (opt.isSlider) {
                drawSlider(ctx, opt, anim, listLeft, listRight, oy, delta, accent, catAlpha);
            } else if (opt.isText) {
                drawTextOption(ctx, opt, ctrlRight, oy, catAlpha);
            } else if (opt.isDropdown) {
                drawDropdownButton(ctx, opt, anim, ctrlRight, oy, delta, accent, catAlpha);
            } else if (opt.isVector3) {
                drawVector3(ctx, opt, anim, listLeft, listRight, oy, delta, accent, catAlpha);
            } else if (opt.isButton) {
                int btnW = 76;
                int btnH = 22;
                int bx = ctrlRight - btnW;
                int by = oy + (OPT_ROW_H - btnH) / 2;
                boolean bHover = mouseX >= bx && mouseX <= bx + btnW && mouseY >= by && mouseY <= by + btnH;
                drawRoundRect(ctx, bx, by, btnW, btnH, 6,
                        ((int) ((bHover ? 0x45 : 0x22) * (catAlpha / 255f)) << 24) | accent);
                drawBorderRounded(ctx, bx, by, btnW, btnH, 6,
                        ((int) ((bHover ? 0x80 : 0x40) * (catAlpha / 255f)) << 24) | accent);
                String btnText = "Open";
                int btw = AetherionFont.width(btnText);
                AetherionFont.draw(ctx, btnText, bx + (btnW - btw) / 2, by + 6, (catAlpha << 24) | 0xFFFFFF);
            }
        }
        } finally {
            ctx.disableScissor();
        }

        // Scrollbar
        if (maxScroll > 0) {
            int sbX = px + PANEL_W - 8;
            int sbY = listTop;
            int sbH = listBottom - sbY;
            int barH = Math.max(24, (int) (sbH * (sbH / (float) (sbH + maxScroll))));
            int barY = sbY + (int) ((sbH - barH) * (scrollOffset / maxScroll));
            drawRoundRect(ctx, sbX, sbY, 3, sbH, 2, 0x14FFFFFF);
            drawRoundRect(ctx, sbX, barY, 3, barH, 2,
                    (openAlpha << 24) | accent);
        }

        // Tooltip
        if (hoveredOption != null && hoveredOption.description != null
                && openDropdown == null) {
            drawTooltip(ctx, mouseX, mouseY, hoveredOption.description);
        }

        ctx.draw();
    }

    // ============================================================
    // TOGGLE (iOS style)
    // ============================================================

    private void drawToggle(DrawContext ctx, Option opt, OptionAnim anim,
                            int rightX, int rowY, float delta,
                            int accent, int openAlpha) {
        boolean value = (boolean) opt.get();
        anim.toggle.setTarget(value ? 1f : 0f);
        float t = anim.toggle.tick(delta);

        int tw = ToggleSwitch.WIDTH, th = ToggleSwitch.HEIGHT;
        int tx = rightX - tw;
        int ty = rowY + (OPT_ROW_H - th) / 2;

        ToggleSwitch.draw(ctx, tx, ty, t, accent & 0xFFFFFF);
    }

    // ============================================================
    // SLIDER
    // ============================================================

    private void drawSlider(DrawContext ctx, Option opt, OptionAnim anim,
                            int listLeft, int listRight, int rowY, float delta,
                            int accent, int openAlpha) {
        float value = ((Number) opt.get()).floatValue();
        float min = opt.isFloat ? opt.floatMin : opt.min;
        float max = opt.isFloat ? opt.floatMax : opt.max;
        float pct = clamp01((value - min) / (max - min));

        anim.slider.setTarget(pct);
        float animPct = anim.slider.tick(delta);

        String display = opt.isFloat
                ? String.format("%.2f", value)
                : String.valueOf((int) value);

        // Glass value badge on the far right
        int badgeW = Math.max(36, AetherionFont.width(display) + 12);
        int badgeH = 20;
        int badgeX = listRight - badgeW;
        int badgeY = rowY + (OPT_ROW_H - badgeH) / 2;

        drawRoundRect(ctx, badgeX, badgeY, badgeW, badgeH, 6,
                ((int) (0x1A * (openAlpha / 255f)) << 24) | 0xFFFFFF);
        drawBorderRounded(ctx, badgeX, badgeY, badgeW, badgeH, 6,
                ((int) (0x22 * (openAlpha / 255f)) << 24) | 0xFFFFFF);
        int valW = AetherionFont.width(display);
        AetherionFont.draw(ctx, display,
                badgeX + (badgeW - valW) / 2, badgeY + 6,
                (openAlpha << 24) | C_TEXT_PRIMARY);

        // Slider track
        int trackRight = badgeX - 10;
        int trackW = 100;
        int trackLeft = trackRight - trackW;
        int trackH = 4;
        int trackY = rowY + (OPT_ROW_H - trackH) / 2;

        // Inactive track
        drawPill(ctx, trackLeft, trackY, trackW, trackH,
                ((int) (0x25 * (openAlpha / 255f)) << 24) | 0xFFFFFF);

        // Active fill
        int fillW = (int) (trackW * animPct);
        if (fillW > 0) {
            drawPill(ctx, trackLeft, trackY, Math.max(fillW, trackH), trackH,
                    (openAlpha << 24) | accent);
        }

        // Knob with drop shadow and shine
        int knobX = trackLeft + (int) (trackW * animPct);
        int knobCY = trackY + trackH / 2;
        drawCircle(ctx, knobX, knobCY + 1, 6,
                ((int) (0x45 * (openAlpha / 255f)) << 24));
        drawCircle(ctx, knobX, knobCY, 5,
                (openAlpha << 24) | C_KNOB);
        drawCircle(ctx, knobX, knobCY - 1, 2,
                ((int) (0x60 * (openAlpha / 255f)) << 24) | 0xFFFFFF);
    }

    // ============================================================
    // TEXT (colour picker)
    // ============================================================

    private void drawTextOption(DrawContext ctx, Option opt, int rightX, int rowY, int openAlpha) {
        String value = (String) opt.get();
        String text;
        int swatchColor = 0xFF000000;
        try {
            int c = 0xFF000000 | Integer.parseInt(value.replace("#", ""), 16);
            swatchColor = c;
            text = value.toUpperCase();
        } catch (Exception e) {
            text = value;
        }

        int textW = AetherionFont.width(text);
        int swatchSize = 14;
        int totalW = swatchSize + 8 + textW;
        int startX = rightX - totalW;
        int cy = rowY + OPT_ROW_H / 2;

        // Swatch
        drawRoundRect(ctx, startX, cy - swatchSize / 2,
                swatchSize, swatchSize, 4, (openAlpha << 24) | (swatchColor & 0xFFFFFF));
        drawBorderRounded(ctx, startX, cy - swatchSize / 2,
                swatchSize, swatchSize, 4,
                ((int) (0x30 * (openAlpha / 255f)) << 24) | 0xFFFFFF);

        // Text
        AetherionFont.draw(ctx, text,
                startX + swatchSize + 8, rowY + (OPT_ROW_H - 9) / 2,
                (openAlpha << 24) | C_TEXT_SECONDARY);
    }

    // ============================================================
    // DROPDOWN (button in row)
    // ============================================================

    private void drawDropdownButton(DrawContext ctx, Option opt, OptionAnim anim,
                                    int rightX, int rowY, float delta,
                                    int accent, int openAlpha) {
        int value = (int) opt.get();
        String label = opt.choices[value];

        int btnW = 140;
        int btnH = 24;
        int bx = rightX - btnW;
        int by = rowY + (OPT_ROW_H - btnH) / 2;

        boolean isOpen = (openDropdown == opt);
        anim.dropdown.setTarget(isOpen ? 1f : 0f);
        float t = anim.dropdown.tick(delta);

        // Background
        int bgColor = ((int) (0x20 * (openAlpha / 255f)) << 24) | 0xFFFFFF;
        if (t > 0.01f) {
            bgColor = lerpColor(bgColor,
                    (openAlpha << 24) | ((accent & 0xFFFFFF) | 0x20000000),
                    t * 0.5f);
        }
        drawRoundRect(ctx, bx, by, btnW, btnH, 8, bgColor);

        // Border
        drawBorderRounded(ctx, bx, by, btnW, btnH, 8,
                ((int) (0x25 * (openAlpha / 255f)) << 24) | 0xFFFFFF);

        // Label
        AetherionFont.draw(ctx, label,
                bx + 10, by + (btnH - 9) / 2, (openAlpha << 24) | C_TEXT_PRIMARY);

        // Chevron
        int chX = bx + btnW - 14;
        int chY = by + btnH / 2;
        drawChevron(ctx, chX, chY, 4, !isOpen, (openAlpha << 24) | C_TEXT_SECONDARY);
    }

    // ============================================================
    // VECTOR3 (Compact 3-Axis Pill Steppers)
    // ============================================================

    private void drawVector3(DrawContext ctx, Option opt, OptionAnim anim,
                             int listLeft, int listRight, int rowY, float delta,
                             int accent, int openAlpha) {
        int boxW = 50, boxH = 20, gap = 6;
        int totalW = 3 * boxW + 2 * gap;
        int startX = listRight - totalW - 6;
        int boxY = rowY + (OPT_ROW_H - boxH) / 2;

        int[] axisColors = { 0xFFFF6B6B, 0xFF6BCB77, 0xFF4D96FF };
        String[] axisNames = { "X", "Y", "Z" };

        for (int i = 0; i < 3; i++) {
            int bx = startX + i * (boxW + gap);
            float val = opt.getAxis(i);
            String valStr = (val > 0.001f ? "+" : "") + (opt.floatStep >= 1.0f ? String.format("%.0f", val) : String.format("%.2f", val));

            boolean isDragging = (draggingVecOption == opt && draggingVecAxis == i);
            int bgC = isDragging ? ((int) (0x45 * (openAlpha / 255f)) << 24) | 0x1E2436
                                 : ((int) (0x22 * (openAlpha / 255f)) << 24) | 0x121724;
            drawRoundRect(ctx, bx, boxY, boxW, boxH, 5, bgC);

            int borderC = isDragging ? (openAlpha << 24) | accent
                                     : ((int) (0x28 * (openAlpha / 255f)) << 24) | 0xFFFFFF;
            drawBorderRounded(ctx, bx, boxY, boxW, boxH, 5, borderC);

            AetherionFont.draw(ctx, axisNames[i], bx + 4, boxY + 6,
                    (openAlpha << 24) | axisColors[i]);

            int valW = AetherionFont.width(valStr);
            AetherionFont.draw(ctx, valStr, bx + boxW - valW - 4, boxY + 6,
                    (openAlpha << 24) | C_TEXT_PRIMARY);
        }
    }

    // ============================================================
    // OPEN DROPDOWN LIST
    // ============================================================

    private void drawOpenDropdown(DrawContext ctx, int mouseX, int mouseY,
                                  float delta, int accent) {
        Category current = categories.get(selectedCategory);
        int idx = current.options.indexOf(openDropdown);
        if (idx < 0) return;

        int px = panelX;
        int py = panelY;
        int listTop = py + OPT_LIST_TOP;
        int oy = listTop + idx * OPT_ROW_H - (int) scrollOffset;

        // Layout of the row's dropdown button
        int listRight = px + PANEL_W - LIST_SIDE_PADDING;
        int ctrlRight = listRight - 4;
        int btnW = 140;
        int btnH = 24;
        int bx = ctrlRight - btnW;
        int by = oy + (OPT_ROW_H - btnH) / 2;

        int ddX = bx;
        int ddY = by + btnH + 4;
        int ddW = btnW;

        int itemH = 22;
        int visible = Math.min(DROPDOWN_MAX_VISIBLE, openDropdown.choices.length);
        int listH = visible * itemH;

        int maxScrollLocal = Math.max(0, openDropdown.choices.length - DROPDOWN_MAX_VISIBLE);
        dropdownScroll = Math.max(0, Math.min(maxScrollLocal, dropdownScroll));

        OptionAnim anim = anim(openDropdown);
        float openT = anim.dropdown.get();

        // Scale + fade
        ctx.getMatrices().push();
        float cx = ddX + ddW / 2f;
        float cy = ddY;
        float scale = 0.92f + 0.08f * easeOutCubic(openT);
        ctx.getMatrices().translate(cx, cy, 0);
        ctx.getMatrices().scale(scale, scale, 1f);
        ctx.getMatrices().translate(-cx, -cy, 0);

        // Background
        int bgAlpha = (int) (0xE0 * openT);
        drawRoundRect(ctx, ddX, ddY, ddW, listH, 10,
                (bgAlpha << 24) | 0x141820);
        GlassRenderer.specular(ctx, ddX, ddY, ddW, listH, 10,
                ((int) (0x24 * openT) << 24) | 0xFFFFFF);
        drawBorderRounded(ctx, ddX, ddY, ddW, listH, 10,
                ((int) (0x28 * openT) << 24) | 0xFFFFFF);

        // Items
        ctx.enableScissor(ddX + 1, ddY + 1, ddX + ddW - 1, ddY + listH - 1);
        try {
            for (int i = 0; i < openDropdown.choices.length; i++) {
                int iy = ddY + (i - dropdownScroll) * itemH;
                if (iy + itemH < ddY || iy > ddY + listH) continue;

                boolean hov = mouseX >= ddX && mouseX <= ddX + ddW
                        && mouseY >= iy && mouseY <= iy + itemH
                        && mouseY >= ddY && mouseY <= ddY + listH;

                if (hov) {
                    drawRoundRect(ctx, ddX + 3, iy + 1, ddW - 6, itemH - 2, 6,
                            ((int) (0x20 * openT) << 24) | 0xFFFFFF);
                }

                boolean selected = (i == (int) openDropdown.get());
                int col = selected ? accent : C_TEXT_PRIMARY;
                AetherionFont.draw(ctx, openDropdown.choices[i],
                        ddX + 10, iy + (itemH - 9) / 2, ((int) (openT * 255) << 24) | col);
            }
        } finally {
            ctx.disableScissor();
        }

        // Scrollbar
        if (maxScrollLocal > 0) {
            int sbX = ddX + ddW - 4;
            int barH = Math.max(12, (int) (listH * (DROPDOWN_MAX_VISIBLE / (float) openDropdown.choices.length)));
            int barY = ddY + (int) ((listH - barH) * (dropdownScroll / (float) maxScrollLocal));
            drawRoundRect(ctx, sbX, barY, 2, barH, 1,
                    ((int) (0x80 * openT) << 24) | accent);
        }

        ctx.getMatrices().pop();
    }

    // ============================================================
    // TAB DOCK
    // ============================================================

    private void drawTabDock(DrawContext ctx, int mouseX, int mouseY,
                             float delta, int accent, float openT) {
        int tbX = getTabsBarX();
        int tbY = getTabsBarY();
        int mainW = getMainDockWidth();
        int tbH = TABS_BAR_H;

        // 1. Ambient drop shadow under dock capsule
        GlassRenderer.dropShadow(ctx, tbX, tbY, mainW, tbH, 18, 8, (int) (0x55 * openT));

        // 2. Dock capsule acrylic fill
        drawRoundRect(ctx, tbX, tbY, mainW, tbH, 18,
                ((int) (0x85 * openT) << 24) | 0x111624);
        GlassRenderer.specular(ctx, tbX, tbY, mainW, tbH, 18,
                ((int) (0x30 * openT) << 24) | 0xFFFFFF);
        drawBorderRounded(ctx, tbX, tbY, mainW, tbH, 18,
                ((int) (0x28 * openT) << 24) | 0xFFFFFF);

        // 3. Animated selection pill
        float targetX = DOCK_PAD_X + selectedCategory * (TAB_SIZE + TAB_GAP);
        tabSelectionX.setTarget(targetX);
        float pillX = tabSelectionX.tick(delta);

        if (openT > 0.1f) {
            int selX = tbX + (int) pillX;
            // Pill background with subtle accent glow
            drawRoundRect(ctx, selX, tbY + DOCK_PAD_Y, TAB_SIZE, TAB_SIZE, 9,
                    ((int) (0x35 * openT) << 24) | accent);
            drawBorderRounded(ctx, selX, tbY + DOCK_PAD_Y, TAB_SIZE, TAB_SIZE, 9,
                    ((int) (0x55 * openT) << 24) | accent);

            // macOS bottom running dash indicator
            int dashW = 8;
            int dashH = 2;
            int dashX = selX + (TAB_SIZE - dashW) / 2;
            int dashY = tbY + tbH - 3;
            drawRoundRect(ctx, dashX, dashY, dashW, dashH, 1,
                    ((int) (openT * 255) << 24) | accent);
        }

        // 4. Tabs items (icons only!)
        String hoveredTabName = null;
        int hoverTooltipX = 0, hoverTooltipY = 0;

        for (int i = 0; i < categories.size(); i++) {
            Category cat = categories.get(i);
            int tx = tbX + DOCK_PAD_X + i * (TAB_SIZE + TAB_GAP);
            int ty = tbY + DOCK_PAD_Y;
            boolean selected = (i == selectedCategory);
            boolean hovered = mouseX >= tx && mouseX <= tx + TAB_SIZE
                    && mouseY >= ty && mouseY <= ty + TAB_SIZE;

            if (hovered) {
                hoveredTabName = cat.name;
                hoverTooltipX = tx + TAB_SIZE / 2;
                hoverTooltipY = tbY - 22;
            }

            // Hover pill for non-selected
            if (hovered && !selected) {
                drawRoundRect(ctx, tx, ty, TAB_SIZE, TAB_SIZE, 9,
                        ((int) (0x18 * openT) << 24) | 0xFFFFFF);
            }

            int iconFloat = (hovered && !selected ? 1 : 0);
            int iconCol = selected ? accent : (hovered ? C_TEXT_PRIMARY : C_TEXT_SECONDARY);
            String icon = getCategoryIcon(cat.name);
            int iconW = IconRenderer.width(icon);

            IconRenderer.draw(ctx, icon,
                    tx + (TAB_SIZE - iconW) / 2, ty + (TAB_SIZE - 16) / 2 - iconFloat,
                    ((int) (openT * 255) << 24) | iconCol);
        }

        // 5. Detached Squircle Button on the right (Resource Packs!)
        int detX = tbX + mainW + DETACHED_GAP;
        int detY = tbY;
        int detSize = DETACHED_SIZE;
        boolean detHover = mouseX >= detX && mouseX <= detX + detSize
                && mouseY >= detY && mouseY <= detY + detSize;

        if (detHover) {
            hoveredTabName = "Resource Packs";
            hoverTooltipX = detX + detSize / 2;
            hoverTooltipY = tbY - 22;
        }

        // Shadow & acrylic for detached button
        GlassRenderer.dropShadow(ctx, detX, detY, detSize, detSize, 14, 8, (int) (0x55 * openT));
        drawRoundRect(ctx, detX, detY, detSize, detSize, 14,
                ((int) ((detHover ? 0xA0 : 0x85) * openT) << 24) | 0x111624);
        GlassRenderer.specular(ctx, detX, detY, detSize, detSize, 14,
                ((int) (0x30 * openT) << 24) | 0xFFFFFF);
        drawBorderRounded(ctx, detX, detY, detSize, detSize, 14,
                ((int) ((detHover ? 0x50 : 0x28) * openT) << 24) | (detHover ? accent : 0xFFFFFF));

        String packIcon = MacIcons.FOLDER;
        int packIconW = IconRenderer.width(packIcon);
        IconRenderer.draw(ctx, packIcon,
                detX + (detSize - packIconW) / 2, detY + (detSize - 16) / 2 - (detHover ? 1 : 0),
                ((int) (openT * 255) << 24) | (detHover ? accent : C_TEXT_SECONDARY));

        // 6. Floating Liquid Glass Tooltip above hovered tab
        if (hoveredTabName != null && openT > 0.5f) {
            int tw = AetherionFont.width(hoveredTabName) + 16;
            int th = 18;
            int tx = hoverTooltipX - tw / 2;
            int ty = hoverTooltipY;

            drawRoundRect(ctx, tx, ty, tw, th, 6, 0xEE101622);
            drawBorderRounded(ctx, tx, ty, tw, th, 6, 0x30FFFFFF);
            GlassRenderer.specular(ctx, tx, ty, tw, th, 6, 0x20FFFFFF);
            AetherionFont.draw(ctx, hoveredTabName, tx + 8, ty + 5, 0xFFFFFFFF);
        }

        ctx.draw();
    }

    // ============================================================
    // TOOLTIP
    // ============================================================

    private void drawTooltip(DrawContext ctx, int mouseX, int mouseY, String description) {
        int tw = AetherionFont.width(description) + 16;
        int th = 22;
        int tx = Math.min(mouseX + 12, width - tw - 6);
        int ty = mouseY + 14;

        drawRoundRect(ctx, tx, ty, tw, th, 8, 0xE6101820);
        drawBorderRounded(ctx, tx, ty, tw, th, 8, 0x28FFFFFF);
        GlassRenderer.specular(ctx, tx, ty, tw, th, 8, 0x20FFFFFF);

        AetherionFont.draw(ctx, description,
                tx + 8, ty + (th - 9) / 2, C_TEXT_SECONDARY);
    }

    // ============================================================
    // MOUSE
    // ============================================================

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);

        int px = panelX;
        int py = panelY;

        // --- Dropdown open: handle click first ---
        if (openDropdown != null) {
            Category current = categories.get(selectedCategory);
            int idx = current.options.indexOf(openDropdown);
            if (idx >= 0) {
                int listRight = px + PANEL_W - LIST_SIDE_PADDING;
                int ctrlRight = listRight - 4;
                int listTop = py + OPT_LIST_TOP;
                int oy = listTop + idx * OPT_ROW_H - (int) scrollOffset;
                int btnW = 140;
                int btnH = 24;
                int bx = ctrlRight - btnW;
                int by = oy + (OPT_ROW_H - btnH) / 2;
                int ddX = bx;
                int ddY = by + btnH + 4;
                int ddW = btnW;
                int itemH = 22;
                int listH = Math.min(DROPDOWN_MAX_VISIBLE, openDropdown.choices.length) * itemH;

                if (mx >= ddX && mx <= ddX + ddW && my >= ddY && my <= ddY + listH) {
                    for (int i = 0; i < openDropdown.choices.length; i++) {
                        int iy = ddY + (i - dropdownScroll) * itemH;
                        if (iy + itemH < ddY || iy > ddY + listH) continue;
                        if (my >= iy && my <= iy + itemH) {
                            openDropdown.set(i);
                            ConfigManager.save();
                            openDropdown = null;
                            dropdownScroll = 0;
                            return true;
                        }
                    }
                    return true;
                }
            }
            openDropdown = null;
            dropdownScroll = 0;
            return true;
        }

        // --- Traffic lights clicks ---
        if (mx >= px + 14 && mx <= px + 26 && my >= py + 12 && my <= py + 24) {
            this.close();
            return true;
        }
        if (mx >= px + 46 && mx <= px + 58 && my >= py + 12 && my <= py + 24) {
            panelX = (width - PANEL_W) / 2;
            panelY = (height - PANEL_H) / 2 - 10;
            return true;
        }

        // --- Close button ---
        int closeSize = 20;
        int closeX = px + PANEL_W - 22 - closeSize;
        int closeY = py + 11;
        if (mx >= closeX && mx <= closeX + closeSize
                && my >= closeY && my <= closeY + closeSize) {
            this.close();
            return true;
        }

        // --- Reset ---
        int resetW = 52;
        int resetH = 20;
        int resetX = px + PANEL_W - 22 - resetW - 28;
        int resetY = py + 11;
        if (mx >= resetX && mx <= resetX + resetW
                && my >= resetY && my <= resetY + resetH) {
            for (WidgetEntry w : widgetEntries) w.resetPos();
            ConfigManager.save();
            return true;
        }

        // --- Panel header drag ---
        if (mx >= px && mx <= px + PANEL_W - 140
                && my >= py && my <= py + HEADER_H) {
            draggingPanel = true;
            dragOffX = (int) mx - px;
            dragOffY = (int) my - py;
            return true;
        }

        // --- Tab dock ---
        int tbX = getTabsBarX();
        int tbY = getTabsBarY();
        for (int i = 0; i < categories.size(); i++) {
            int tx = tbX + DOCK_PAD_X + i * (TAB_SIZE + TAB_GAP);
            int ty = tbY + DOCK_PAD_Y;
            if (mx >= tx && mx <= tx + TAB_SIZE && my >= ty && my <= ty + TAB_SIZE) {
                if (selectedCategory != i) {
                    selectedCategory = i;
                    scrollOffset = 0;
                    categoryTransition.snapTo(0f);
                    categoryTransition.setTarget(1f);
                    updateMaxScroll();
                }
                return true;
            }
        }

        // --- Detached button (Resource Packs) ---
        int detX = tbX + getMainDockWidth() + DETACHED_GAP;
        int detY = tbY;
        if (mx >= detX && mx <= detX + DETACHED_SIZE && my >= detY && my <= detY + DETACHED_SIZE) {
            MinecraftClient mc = MinecraftClient.getInstance();
            mc.setScreen(new net.minecraft.client.gui.screen.pack.PackScreen(
                    mc.getResourcePackManager(),
                    manager -> {
                        var oldPacks = com.google.common.collect.ImmutableList.copyOf(mc.options.resourcePacks);
                        mc.options.refreshResourcePacks(manager);
                        var newPacks = com.google.common.collect.ImmutableList.copyOf(mc.options.resourcePacks);
                        if (!newPacks.equals(oldPacks)) {
                            mc.reloadResources();
                        }
                        mc.setScreen(new MacClientMenu());
                    },
                    mc.getResourcePackDir(),
                    net.minecraft.text.Text.translatable("resourcePack.title")
            ));
            return true;
        }

        // --- Options ---
        int listLeft = px + LIST_SIDE_PADDING;
        int listRight = px + PANEL_W - LIST_SIDE_PADDING;
        int listTop = py + OPT_LIST_TOP;
        int listBottom = py + PANEL_H - 16;

        if (my >= listTop - 4 && my <= listBottom) {
            Category current = categories.get(selectedCategory);
            for (int i = 0; i < current.options.size(); i++) {
                Option opt = current.options.get(i);
                int oy = listTop + i * OPT_ROW_H - (int) scrollOffset;
                if (oy + OPT_ROW_H < listTop - 4) continue;
                if (oy > listBottom) break;

                int ctrlRight = listRight - 6;

                if (opt.isBool) {
                    int tw = ToggleSwitch.WIDTH, th = ToggleSwitch.HEIGHT;
                    int tx = ctrlRight - tw;
                    int ty = oy + (OPT_ROW_H - th) / 2;
                    if (mx >= tx && mx <= tx + tw && my >= ty && my <= ty + th) {
                        opt.set(!(boolean) opt.get());
                        ConfigManager.save();
                        return true;
                    }
                } else if (opt.isSlider) {
                    float value = ((Number) opt.get()).floatValue();
                    String display = opt.isFloat
                            ? String.format("%.2f", value)
                            : String.valueOf((int) value);
                    int badgeW = Math.max(36, AetherionFont.width(display) + 12);
                    int badgeX = listRight - badgeW;
                    int trackRight = badgeX - 10;
                    int trackW = 100;
                    int trackLeft = trackRight - trackW;
                    int trackY = oy + (OPT_ROW_H - 4) / 2;

                    if (mx >= trackLeft - 6 && mx <= trackRight + 6
                            && my >= trackY - 10 && my <= trackY + 14) {
                        draggingSlider = opt;
                        sliderX = trackLeft;
                        sliderW = trackW;
                        applySlider(mx);
                        return true;
                    }
                } else if (opt.isText) {
                    String value = (String) opt.get();
                    int textW = AetherionFont.width(value.toUpperCase());
                    int swatchSize = 14;
                    int totalW = swatchSize + 8 + textW;
                    int startX = ctrlRight - totalW;
                    if (mx >= startX && mx <= ctrlRight
                            && my >= oy && my <= oy + OPT_ROW_H) {
                        final Option targetOpt = opt;
                        try {
                            int initial = 0xFF000000 | Integer.parseInt(
                                    value.replace("#", ""), 16);
                            MinecraftClient.getInstance()
                                    .setScreen(new ColorPickerScreen(this, initial, hex -> {
                                        targetOpt.setter.accept(hex);
                                        ConfigManager.save();
                                    }));
                        } catch (Exception e) {
                            MinecraftClient.getInstance()
                                    .setScreen(new ColorPickerScreen(this, 0xFF00D4FF, hex -> {
                                        targetOpt.setter.accept(hex);
                                        ConfigManager.save();
                                    }));
                        }
                        return true;
                    }
                } else if (opt.isDropdown) {
                    int btnW = 140;
                    int btnH = 24;
                    int bx = ctrlRight - btnW;
                    int by = oy + (OPT_ROW_H - btnH) / 2;
                    if (mx >= bx && mx <= bx + btnW && my >= by && my <= by + btnH) {
                        openDropdown = opt;
                        dropdownScroll = 0;
                        // Snap anim to 0 so it opens from 0→1 smoothly
                        anim(opt).dropdown.snapTo(0f);
                        return true;
                    }
                } else if (opt.isVector3) {
                    int boxW = 50, boxH = 20, gap = 6;
                    int totalW = 3 * boxW + 2 * gap;
                    int startX = listRight - totalW - 6;
                    int boxY = oy + (OPT_ROW_H - boxH) / 2;
                    if (my >= boxY && my <= boxY + boxH) {
                        for (int a = 0; a < 3; a++) {
                            int bx = startX + a * (boxW + gap);
                            if (mx >= bx && mx <= bx + boxW) {
                                draggingVecOption = opt;
                                draggingVecAxis = a;
                                vecDragStartX = mx;
                                vecDragStartVal = opt.getAxis(a);
                                return true;
                            }
                        }
                    }
                } else if (opt.isButton) {
                    int btnW = 76;
                    int btnH = 22;
                    int bx = ctrlRight - btnW;
                    int by = oy + (OPT_ROW_H - btnH) / 2;
                    if (mx >= bx && mx <= bx + btnW && my >= by && my <= by + btnH) {
                        if (opt.action != null) opt.action.run();
                        return true;
                    }
                }
            }
        }

        // --- HUD widget drag (outside panel) ---
        boolean insidePanel = mx >= px && mx <= px + PANEL_W
                && my >= py && my <= py + PANEL_H;
        boolean insideDock = mx >= tbX - 12 && mx <= tbX + getTabsBarTotalWidth() + 12
                && my >= tbY - 6 && my <= tbY + TABS_BAR_H + 6;
        if (!insidePanel && !insideDock) {
            for (WidgetEntry w : widgetEntries) {
                int wx = w.getX(), wy = w.getY();
                int ww = w.getWidth(), wh = w.getHeight();
                if (mx >= wx && mx <= wx + ww && my >= wy && my <= wy + wh) {
                    int idx = widgetEntries.indexOf(w);
                    if (idx >= 0) {
                        draggingPanel = false;
                    }
                    return true;
                }
            }
        }

        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int px = panelX;
        int py = panelY;

        if (openDropdown != null) {
            Category current = categories.get(selectedCategory);
            int idx = current.options.indexOf(openDropdown);
            if (idx >= 0) {
                int listRight = px + PANEL_W - LIST_SIDE_PADDING;
                int ctrlRight = listRight - 4;
                int listTop = py + OPT_LIST_TOP;
                int oy = listTop + idx * OPT_ROW_H - (int) scrollOffset;
                int btnW = 140;
                int btnH = 24;
                int by = oy + (OPT_ROW_H - btnH) / 2;
                int ddX = ctrlRight - btnW;
                int ddY = by + btnH + 4;
                int ddW = btnW;
                int listH = Math.min(DROPDOWN_MAX_VISIBLE, openDropdown.choices.length) * 22;

                if (mx >= ddX && mx <= ddX + ddW && my >= ddY && my <= ddY + listH) {
                    int maxScrollLocal = Math.max(0,
                            openDropdown.choices.length - DROPDOWN_MAX_VISIBLE);
                    dropdownScroll -= (int) delta;
                    dropdownScroll = Math.max(0, Math.min(maxScrollLocal, dropdownScroll));
                    return true;
                }
            }
        }

        // Vector3 mouse wheel adjust
        if (my >= py + OPT_LIST_TOP && my <= py + PANEL_H) {
            Category current = categories.get(selectedCategory);
            for (int i = 0; i < current.options.size(); i++) {
                Option opt = current.options.get(i);
                if (!opt.isVector3) continue;
                int oy = py + OPT_LIST_TOP + i * OPT_ROW_H - (int) scrollOffset;
                int boxH = 20;
                int boxY = oy + (OPT_ROW_H - boxH) / 2;
                if (my >= boxY && my <= boxY + boxH) {
                    int listRight = px + PANEL_W - LIST_SIDE_PADDING;
                    int boxW = 50, gap = 6;
                    int startX = listRight - (3 * boxW + 2 * gap) - 6;
                    for (int a = 0; a < 3; a++) {
                        int bx = startX + a * (boxW + gap);
                        if (mx >= bx && mx <= bx + boxW) {
                            float cur = opt.getAxis(a);
                            opt.setAxis(a, cur + (float) (delta * opt.floatStep));
                            ConfigManager.save();
                            return true;
                        }
                    }
                }
            }
        }

        if (mx >= px && mx <= px + PANEL_W && my >= py + OPT_LIST_TOP && my <= py + PANEL_H) {
            scrollOffset -= delta * 24;
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset));
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (button != 0) return super.mouseDragged(mx, my, button, dx, dy);

        if (draggingPanel) {
            panelX = (int) mx - dragOffX;
            panelY = (int) my - dragOffY;
            return true;
        }
        if (draggingSlider != null) {
            applySlider(mx);
            return true;
        }
        if (draggingVecOption != null) {
            float deltaPx = (float) (mx - vecDragStartX);
            float step = draggingVecOption.floatStep;
            float nv = vecDragStartVal + deltaPx * step * 0.5f;
            draggingVecOption.setAxis(draggingVecAxis, nv);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (draggingPanel) { draggingPanel = false; return true; }
        if (draggingSlider != null) { draggingSlider = null; ConfigManager.save(); return true; }
        if (draggingVecOption != null) { draggingVecOption = null; ConfigManager.save(); return true; }
        return super.mouseReleased(mx, my, button);
    }

    private void applySlider(double mx) {
        Option opt = draggingSlider;
        if (opt == null) return;
        float pct = (float) ((mx - sliderX) / sliderW);
        pct = clamp01(pct);

        if (opt.isFloat) {
            float nv = opt.floatMin + pct * (opt.floatMax - opt.floatMin);
            if (opt.floatStep > 0) nv = Math.round(nv / opt.floatStep) * opt.floatStep;
            nv = Math.max(opt.floatMin, Math.min(opt.floatMax, nv));
            opt.set(nv);
        } else {
            int nv = opt.min + (int) (pct * (opt.max - opt.min));
            if (opt.step > 1) nv = Math.round((float) nv / opt.step) * opt.step;
            nv = Math.max(opt.min, Math.min(opt.max, nv));
            opt.set(nv);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private static int parseAccent() {
        try {
            return 0xFF000000 | Integer.parseInt(
                    ConfigManager.INSTANCE.accentColor.replace("#", ""), 16);
        } catch (Exception e) {
            return 0xFF000000 | C_ACCENT_FALLBACK;
        }
    }

    private static float clamp01(float t) {
        return t < 0 ? 0 : (t > 1 ? 1 : t);
    }

    private static float easeOutCubic(float t) {
        t = clamp01(t);
        float inv = 1f - t;
        return 1f - inv * inv * inv;
    }

    private static int lerpColor(int a, int b, float t) {
        t = clamp01(t);
        int aa = (a >>> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int na = (int) (aa + (ba - aa) * t);
        int nr = (int) (ar + (br - ar) * t);
        int ng = (int) (ag + (bg - ag) * t);
        int nb = (int) (ab + (bb - ab) * t);
        return (na << 24) | (nr << 16) | (ng << 8) | nb;
    }

    private static int blend(int a, int b, float t) {
        return lerpColor(a, b, t) & 0xFFFFFF;
    }

    private static void drawRoundRect(DrawContext ctx, int x, int y, int w, int h,
                                      int r, int color) {
        SquircleRenderer.fill(ctx, x, y, w, h, r, color);
    }

    private static void drawPill(DrawContext ctx, int x, int y, int w, int h, int color) {
        SquircleRenderer.pill(ctx, x, y, w, h, color);
    }

    private static void drawCircle(DrawContext ctx, int cx, int cy, int r, int color) {
        SquircleRenderer.circle(ctx, cx, cy, r, color);
    }

    private static void drawBorderRounded(DrawContext ctx, int x, int y, int w, int h,
                                          int radius, int color) {
        SquircleRenderer.border(ctx, x, y, w, h, radius, 1.0f, color);
    }

    /** Small "v" or "^" chevron drawn procedurally. */
    private static void drawChevron(DrawContext ctx, int cx, int cy, int size,
                                    boolean down, int color) {
        if (down) {
            for (int i = 0; i < size; i++) {
                ctx.fill(cx - i, cy - size / 2 + i, cx - i + 1, cy - size / 2 + i + 1, color);
                ctx.fill(cx + i, cy - size / 2 + i, cx + i + 1, cy - size / 2 + i + 1, color);
            }
        } else {
            for (int i = 0; i < size; i++) {
                ctx.fill(cx - i, cy + size / 2 - i, cx - i + 1, cy + size / 2 - i + 1, color);
                ctx.fill(cx + i, cy + size / 2 - i, cx + i + 1, cy + size / 2 - i + 1, color);
            }
        }
    }

    // ============================================================
    // INNER TYPES
    // ============================================================

    private static class Category {
        final String name;
        final List<Option> options;
        Category(String name, List<Option> options) {
            this.name = name;
            this.options = options;
        }
    }

    private static class WidgetEntry {
        String name;
        Supplier<Integer> xSup, ySup, wSup, hSup;
        BiConsumer<Integer, Integer> setter;
        Runnable reset;
        WidgetEntry(String name,
                    Supplier<Integer> xSup, Supplier<Integer> ySup,
                    Supplier<Integer> wSup, Supplier<Integer> hSup,
                    BiConsumer<Integer, Integer> setter,
                    Runnable reset) {
            this.name = name;
            this.xSup = xSup; this.ySup = ySup;
            this.wSup = wSup; this.hSup = hSup;
            this.setter = setter;
            this.reset = reset;
        }
        int getX() { return xSup.get(); }
        int getY() { return ySup.get(); }
        int getWidth() { return wSup.get(); }
        int getHeight() { return hSup.get(); }
        void setPos(int x, int y) { setter.accept(x, y); }
        void resetPos() { reset.run(); }
    }

    private static class Option {
        String name;
        String description;
        java.util.function.Supplier<Object> getter;
        java.util.function.Consumer<Object> setter;
        int min, max, step;
        boolean isFloat = false;
        float floatMin, floatMax, floatStep;
        String[] choices;
        boolean isBool, isSlider, isText, isDropdown, isVector3, isButton;
        Supplier<Float> xGet, yGet, zGet;
        java.util.function.Consumer<Float> xSet, ySet, zSet;
        Runnable action;

        static Option button(String n, String d, Runnable action) {
            Option o = new Option();
            o.name = n; o.description = d;
            o.isButton = true;
            o.action = action;
            return o;
        }

        static Option bool(String n, String d,
                           Supplier<Boolean> g, java.util.function.Consumer<Boolean> s) {
            Option o = new Option();
            o.name = n; o.description = d;
            o.getter = g::get;
            o.setter = v -> s.accept((Boolean) v);
            o.isBool = true;
            return o;
        }

        static Option slider(String n, String d,
                             Supplier<Integer> g, java.util.function.Consumer<Integer> s,
                             int min, int max, int step) {
            Option o = new Option();
            o.name = n; o.description = d;
            o.getter = g::get;
            o.setter = v -> s.accept((Integer) v);
            o.isSlider = true;
            o.min = min; o.max = max; o.step = step;
            return o;
        }

        static Option sliderFloat(String n, String d,
                                  Supplier<Float> g, java.util.function.Consumer<Float> s,
                                  float min, float max, float step) {
            Option o = new Option();
            o.name = n; o.description = d;
            o.getter = g::get;
            o.setter = v -> s.accept((Float) v);
            o.isSlider = true; o.isFloat = true;
            o.floatMin = min; o.floatMax = max; o.floatStep = step;
            return o;
        }

        static Option vector3(String n, String d,
                              Supplier<Float> xg, java.util.function.Consumer<Float> xs,
                              Supplier<Float> yg, java.util.function.Consumer<Float> ys,
                              Supplier<Float> zg, java.util.function.Consumer<Float> zs,
                              float min, float max, float step) {
            Option o = new Option();
            o.name = n; o.description = d;
            o.isVector3 = true;
            o.xGet = xg; o.xSet = xs;
            o.yGet = yg; o.ySet = ys;
            o.zGet = zg; o.zSet = zs;
            o.floatMin = min; o.floatMax = max; o.floatStep = step;
            return o;
        }

        float getAxis(int axis) {
            return switch (axis) {
                case 0 -> xGet != null ? xGet.get() : 0f;
                case 1 -> yGet != null ? yGet.get() : 0f;
                case 2 -> zGet != null ? zGet.get() : 0f;
                default -> 0f;
            };
        }

        void setAxis(int axis, float val) {
            float clamped = Math.max(floatMin, Math.min(floatMax, val));
            if (floatStep > 0f) {
                clamped = Math.round(clamped / floatStep) * floatStep;
            }
            switch (axis) {
                case 0 -> { if (xSet != null) xSet.accept(clamped); }
                case 1 -> { if (ySet != null) ySet.accept(clamped); }
                case 2 -> { if (zSet != null) zSet.accept(clamped); }
            }
        }

        static Option text(String n, String d,
                           Supplier<String> g, java.util.function.Consumer<String> s) {
            Option o = new Option();
            o.name = n; o.description = d;
            o.getter = g::get;
            o.setter = v -> s.accept((String) v);
            o.isText = true;
            return o;
        }

        static Option dropdown(String n, String d,
                               Supplier<Integer> g, java.util.function.Consumer<Integer> s,
                               String[] choices) {
            Option o = new Option();
            o.name = n; o.description = d;
            o.getter = g::get;
            o.setter = v -> s.accept((Integer) v);
            o.isDropdown = true;
            o.choices = choices;
            return o;
        }

        Object get() { return getter != null ? getter.get() : null; }
        void set(Object v) { if (setter != null) setter.accept(v); }
    }

    private static class OptionAnim {
        final AnimationState hover = new AnimationState(0f, 16f);
        final AnimationState toggle = new AnimationState(0f, 18f);
        final AnimationState slider = new AnimationState(0f, 22f);
        final AnimationState dropdown = new AnimationState(0f, 16f);
        boolean firstFrame = true;
    }

    // ============================================================
    // PRESETS
    // ============================================================

    private static final String[] HIT_PRESETS = {
            "aimbooster", "applepay", "bonk", "boykisser", "brick", "bring",
            "bump", "click", "coin", "glass", "hitsound", "magicsquash",
            "meow", "moan", "nya", "osu", "pop", "schoolboy", "skeet",
            "slap", "soft", "squash", "tf2crit", "tung", "uwu", "rust", "off"
    };

    private static final String[] HIT_LABELS = {
            "Aimbooster", "ApplePay", "Bonk", "Boykisser", "Brick", "Bring",
            "Bump", "Click", "Coin", "Glass", "Hitsound", "Magic Squash",
            "Meow", "Moan", "Nya", "Osu", "Pop", "Schoolboy", "Skeet",
            "Slap", "Soft", "Squash", "TF2 Crit", "Tung", "Uwu", "Rust", "OFF"
    };

    private static int indexOfHit(String preset) {
        if (preset == null) return 16;
        for (int i = 0; i < HIT_PRESETS.length; i++) {
            if (HIT_PRESETS[i].equalsIgnoreCase(preset)) return i;
        }
        return 16;
    }

    private static final String[] FX_NAMES = {
            "spark", "star", "point", "rhombus", "snowflake",
            "crown", "heart", "dollar", "glow", "lightning", "none"
    };

    private static final String[] FX_LABELS = {
            "Spark", "Star", "Point", "Rhombus", "Snowflake",
            "Crown", "Heart", "Dollar", "Glow", "Lightning", "OFF"
    };

    private static int indexOfFX(String name) {
        if (name == null) return 0;
        for (int i = 0; i < FX_NAMES.length; i++) {
            if (FX_NAMES[i].equalsIgnoreCase(name)) return i;
        }
        return 0;
    }

    public static String getCategoryIcon(String name) {
        if (name == null) return MacIcons.GENERAL;
        return switch (name.toLowerCase()) {
            case "general" -> MacIcons.GENERAL;
            case "editor" -> MacIcons.EDITOR;
            case "hud" -> MacIcons.HUD;
            case "visuals" -> MacIcons.VISUALS;
            case "viewmodel" -> MacIcons.VIEWMODEL;
            case "misc" -> MacIcons.MISC;
            default -> MacIcons.GENERAL;
        };
    }

    public static String getCategoryDescription(String name) {
        if (name == null) return "Client settings and preferences";
        return switch (name.toLowerCase()) {
            case "general" -> "Liquid glass theme, blur radius and colors";
            case "editor" -> "HUD layout editor grid and magnetic snapping";
            case "hud" -> "On-screen widgets, indicators and crosshair";
            case "visuals" -> "Post-processing, hit particles and animations";
            case "viewmodel" -> "First-person hand offsets, rotation and scale";
            case "misc" -> "Movement helpers, audio presets and zoom";
            default -> "Configuration options";
        };
    }

    public static String getOptionIcon(String optName, String catName) {
        if (optName == null) return MacIcons.GENERAL;
        String lower = optName.toLowerCase();
        if (lower.contains("blur")) return MacIcons.DROPLET;
        if (lower.contains("hotbar")) return MacIcons.GENERAL;
        if (lower.contains("corner") || lower.contains("radius")) return MacIcons.WINDOWS;
        if (lower.contains("accent") || lower.contains("color")) return MacIcons.SUN;
        if (lower.contains("snap") || lower.contains("grid")) return MacIcons.EDITOR;
        if (lower.contains("watermark") || lower.contains("profile")) return MacIcons.PROFILE;
        if (lower.contains("keystrokes")) return MacIcons.KEYBOARD;
        if (lower.contains("combo")) return MacIcons.LIGHTNING;
        if (lower.contains("target")) return MacIcons.TARGET;
        if (lower.contains("armor")) return MacIcons.SHIELD;
        if (lower.contains("cooldown") || lower.contains("clock") || lower.contains("time")) return MacIcons.CLOCK;
        if (lower.contains("hit indicator") || lower.contains("indicator")) return MacIcons.WARNING;
        if (lower.contains("crosshair")) return MacIcons.TARGET;
        if (lower.contains("potion") || lower.contains("effect")) return MacIcons.PILLS;
        if (lower.contains("bright") || lower.contains("fullbright")) return MacIcons.SUN;
        if (lower.contains("fog") || lower.contains("weather")) return MacIcons.CLOUD;
        if (lower.contains("glass") || lower.contains("chams")) return MacIcons.EYE;
        if (lower.contains("camera") || lower.contains("hurt") || lower.contains("heart")) return MacIcons.HEART;
        if (lower.contains("physics") || lower.contains("item")) return MacIcons.FOLDER;
        if (lower.contains("crit") || lower.contains("fire")) return MacIcons.FIRE;
        if (lower.contains("kill")) return MacIcons.HEART_FILLED;
        if (lower.contains("bloom")) return MacIcons.BLOOM;
        if (lower.contains("motion")) return MacIcons.GAUGE;
        if (lower.contains("swing") || lower.contains("hand") || lower.contains("vm")) return MacIcons.VIEWMODEL;
        if (lower.contains("offset") || lower.contains("rotate") || lower.contains("scale") || lower.contains("[x,y,z]")) return MacIcons.SLIDERS;
        if (lower.contains("wetness")) return MacIcons.DROPLET;
        if (lower.contains("free look") || lower.contains("look")) return MacIcons.EYE_FILLED;
        if (lower.contains("zoom")) return MacIcons.SEARCH;
        if (lower.contains("sound") || lower.contains("volume") || lower.contains("pitch")) return MacIcons.BELL;
        if (lower.contains("sprint")) return MacIcons.LIGHTNING;
        if (lower.contains("afk")) return MacIcons.CLOCK_FILLED;
        if (lower.contains("waypoint")) return MacIcons.FOLDER;
        if (lower.contains("chat")) return MacIcons.CHAT;
        return getCategoryIcon(catName);
    }
}