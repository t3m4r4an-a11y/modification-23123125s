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

    private static final int PANEL_W    = 540;
    private static final int PANEL_H    = 400;
    private static final int HEADER_H   = 54;
    private static final int OPT_ROW_H  = 36;
    private static final int OPT_LIST_TOP = HEADER_H + 38;
    private static final int LIST_SIDE_PADDING = 20;

    private static final int TABS_BAR_H        = 52;
    private static final int TAB_W             = 110;
    private static final int TAB_GAP           = 8;
    private static final int TABS_BOTTOM_MARGIN = 28;

    private static final int DROPDOWN_MAX_VISIBLE = 7;

    // ============================================================
    // PALETTE
    // ============================================================

    private static final int C_TEXT_PRIMARY   = 0xF5F5F7;
    private static final int C_TEXT_SECONDARY = 0xA1A1A6;
    private static final int C_TEXT_TERTIARY  = 0x6E6E73;
    private static final int C_ACCENT_FALLBACK = 0x00D4FF;

    private static final int C_PANEL_BG     = 0x55101828;
    private static final int C_PANEL_BORDER = 0x18FFFFFF;
    private static final int C_SPECULAR     = 0x22FFFFFF;
    private static final int C_DIVIDER      = 0x14FFFFFF;

    private static final int C_ROW_HOVER    = 0x14FFFFFF;
    private static final int C_TRACK        = 0x25FFFFFF;
    private static final int C_KNOB         = 0xF5F5F7;
    private static final int C_KNOB_SHADOW  = 0x55000000;

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

    private Option openDropdown = null;
    private int dropdownScroll = 0;

    private final List<Category> categories = new ArrayList<>();
    private final List<WidgetEntry> widgetEntries = new ArrayList<>();

    // Per-option animation cache
    private final Map<Option, OptionAnim> anims = new HashMap<>();

    // Panel open animation
    private final AnimationState panelOpen = new AnimationState(0f, 9f);

    // Tab selection pill animation
    private final AnimationState tabSelectionX = new AnimationState(0f, 14f);

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
        tabSelectionX.snapTo(selectedCategory * (TAB_W + TAB_GAP));

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
                () -> ConfigManager.INSTANCE.enableWatermark,
                v -> ConfigManager.INSTANCE.enableWatermark = v));
        hud.add(Option.bool("Keystrokes", "WASD + LMB/RMB.",
                () -> ConfigManager.INSTANCE.enableKeystrokesWidget,
                v -> ConfigManager.INSTANCE.enableKeystrokesWidget = v));
        hud.add(Option.bool("Combo Counter", "Счётчик комбо.",
                () -> ConfigManager.INSTANCE.enableComboCounter,
                v -> ConfigManager.INSTANCE.enableComboCounter = v));
        hud.add(Option.bool("Target HUD", "Инфо о цели.",
                () -> ConfigManager.INSTANCE.enableTargetHUD,
                v -> ConfigManager.INSTANCE.enableTargetHUD = v));
        hud.add(Option.bool("Armor Bar", "Прочность брони.",
                () -> ConfigManager.INSTANCE.enableArmorBar,
                v -> ConfigManager.INSTANCE.enableArmorBar = v));
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
                v -> ConfigManager.INSTANCE.hitIndicatorRadius = v, 20, 100, 1));
        hud.add(Option.bool("Custom Crosshair", "Свой прицел.",
                () -> ConfigManager.INSTANCE.enableCustomCrosshair,
                v -> ConfigManager.INSTANCE.enableCustomCrosshair = v));
        hud.add(Option.dropdown("Crosshair Style", "Стиль прицела.",
                () -> ConfigManager.INSTANCE.crosshairStyle,
                v -> ConfigManager.INSTANCE.crosshairStyle = v,
                new String[]{"Cross", "Dot", "Circle", "Cross+Dot"}));
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
                () -> ConfigManager.INSTANCE.enablePotionHud,
                v -> ConfigManager.INSTANCE.enablePotionHud = v));
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
        categories.add(new Category("HUD", hud));

        // ===== VISUALS =====
        List<Option> visuals = new ArrayList<>();
        visuals.add(Option.bool("Fullbright", "Освещение в темноте.",
                () -> ConfigManager.INSTANCE.enableFullbright,
                v -> ConfigManager.INSTANCE.enableFullbright = v));
        visuals.add(Option.bool("No Fog", "Убирает туман.",
                () -> ConfigManager.INSTANCE.enableNoFog,
                v -> ConfigManager.INSTANCE.enableNoFog = v));
        visuals.add(Option.bool("Glass Chams", "Прозрачные предметы.",
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
        visuals.add(Option.text("Crit FX Color", "Цвет партикла.",
                () -> ConfigManager.INSTANCE.critEffectColor,
                v -> ConfigManager.INSTANCE.critEffectColor = v));
        visuals.add(Option.dropdown("Kill Effect Type", "Тип эффекта.",
                () -> {
                    String[] types = {"ring", "lightning", "spiral", "ghost", "none"};
                    for (int i = 0; i < types.length; i++) {
                        if (types[i].equalsIgnoreCase(ConfigManager.INSTANCE.killEffectType)) return i;
                    }
                    return 0;
                },
                v -> {
                    String[] types = {"ring", "lightning", "spiral", "ghost", "none"};
                    if (v >= 0 && v < types.length) ConfigManager.INSTANCE.killEffectType = types[v];
                },
                new String[]{"Ring", "Lightning", "Spiral", "Ghost", "OFF"}));
        visuals.add(Option.bool("Smooth Swing", "Плавный замах в стиле читов.",
                () -> ConfigManager.INSTANCE.enableSmoothSwing,
                v -> ConfigManager.INSTANCE.enableSmoothSwing = v));
        visuals.add(Option.dropdown("Swing Mode", "Стиль замаха.",
                () -> switch (ConfigManager.INSTANCE.swingMode) {
                    case "HORIZONTAL" -> 1;
                    case "BACKHAND" -> 2;
                    case "THRUST" -> 3;
                    case "CHOP" -> 4;
                    case "JAB" -> 5;
                    default -> 0;
                },
                v -> ConfigManager.INSTANCE.swingMode = switch (v) {
                    case 1 -> "HORIZONTAL";
                    case 2 -> "BACKHAND";
                    case 3 -> "THRUST";
                    case 4 -> "CHOP";
                    case 5 -> "JAB";
                    default -> "DIAGONAL";
                },
                new String[]{"DIAGONAL", "HORIZONTAL", "BACKHAND", "THRUST", "CHOP", "JAB"}));
        visuals.add(Option.bool("Aspect Ratio", "Растянуть картинку по горизонтали.",
                () -> ConfigManager.INSTANCE.enableAspectRatio,
                v -> ConfigManager.INSTANCE.enableAspectRatio = v));
        visuals.add(Option.sliderFloat("Aspect Multiplier", "0.75=4:3, 1.0=Vanilla, 1.33=21:9.",
                () -> ConfigManager.INSTANCE.aspectRatio,
                v -> ConfigManager.INSTANCE.aspectRatio = v, 0.5f, 2.0f, 0.05f));
        categories.add(new Category("Visuals", visuals));

        // ===== VIEWMODEL =====
        List<Option> viewmodel = new ArrayList<>();
        viewmodel.add(Option.bool("Main Hand VM", "Настройки правой руки (главной).",
                () -> ConfigManager.INSTANCE.enableViewModel,
                v -> ConfigManager.INSTANCE.enableViewModel = v));
        viewmodel.add(Option.sliderFloat("Main Offset X", "Смещение главной руки по X.",
                () -> ConfigManager.INSTANCE.vmOffsetX,
                v -> ConfigManager.INSTANCE.vmOffsetX = v, -2f, 2f, 0.01f));
        viewmodel.add(Option.sliderFloat("Main Offset Y", "Смещение главной руки по Y.",
                () -> ConfigManager.INSTANCE.vmOffsetY,
                v -> ConfigManager.INSTANCE.vmOffsetY = v, -2f, 2f, 0.01f));
        viewmodel.add(Option.sliderFloat("Main Offset Z", "Смещение главной руки по Z.",
                () -> ConfigManager.INSTANCE.vmOffsetZ,
                v -> ConfigManager.INSTANCE.vmOffsetZ = v, -2f, 2f, 0.01f));
        viewmodel.add(Option.sliderFloat("Main Scale", "Масштаб главной руки.",
                () -> ConfigManager.INSTANCE.vmScale,
                v -> ConfigManager.INSTANCE.vmScale = v, 0.1f, 3f, 0.05f));
        viewmodel.add(Option.sliderFloat("Main Rotate X", "Поворот главной руки по X.",
                () -> ConfigManager.INSTANCE.vmRotateX,
                v -> ConfigManager.INSTANCE.vmRotateX = v, -180f, 180f, 1f));
        viewmodel.add(Option.sliderFloat("Main Rotate Y", "Поворот главной руки по Y.",
                () -> ConfigManager.INSTANCE.vmRotateY,
                v -> ConfigManager.INSTANCE.vmRotateY = v, -180f, 180f, 1f));
        viewmodel.add(Option.sliderFloat("Main Rotate Z", "Поворот главной руки по Z.",
                () -> ConfigManager.INSTANCE.vmRotateZ,
                v -> ConfigManager.INSTANCE.vmRotateZ = v, -180f, 180f, 1f));
        viewmodel.add(Option.bool("Off Hand VM", "Настройки левой руки (второй).",
                () -> ConfigManager.INSTANCE.enableOffHandViewModel,
                v -> ConfigManager.INSTANCE.enableOffHandViewModel = v));
        viewmodel.add(Option.sliderFloat("Off Offset X", "Смещение второй руки по X.",
                () -> ConfigManager.INSTANCE.offOffsetX,
                v -> ConfigManager.INSTANCE.offOffsetX = v, -2f, 2f, 0.01f));
        viewmodel.add(Option.sliderFloat("Off Offset Y", "Смещение второй руки по Y.",
                () -> ConfigManager.INSTANCE.offOffsetY,
                v -> ConfigManager.INSTANCE.offOffsetY = v, -2f, 2f, 0.01f));
        viewmodel.add(Option.sliderFloat("Off Offset Z", "Смещение второй руки по Z.",
                () -> ConfigManager.INSTANCE.offOffsetZ,
                v -> ConfigManager.INSTANCE.offOffsetZ = v, -2f, 2f, 0.01f));
        viewmodel.add(Option.sliderFloat("Off Scale", "Масштаб второй руки.",
                () -> ConfigManager.INSTANCE.offScale,
                v -> ConfigManager.INSTANCE.offScale = v, 0.1f, 3f, 0.05f));
        viewmodel.add(Option.sliderFloat("Off Rotate X", "Поворот второй руки по X.",
                () -> ConfigManager.INSTANCE.offRotateX,
                v -> ConfigManager.INSTANCE.offRotateX = v, -180f, 180f, 1f));
        viewmodel.add(Option.sliderFloat("Off Rotate Y", "Поворот второй руки по Y.",
                () -> ConfigManager.INSTANCE.offRotateY,
                v -> ConfigManager.INSTANCE.offRotateY = v, -180f, 180f, 1f));
        viewmodel.add(Option.sliderFloat("Off Rotate Z", "Поворот второй руки по Z.",
                () -> ConfigManager.INSTANCE.offRotateZ,
                v -> ConfigManager.INSTANCE.offRotateZ = v, -180f, 180f, 1f));
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

    private int getTabsBarTotalWidth() {
        return categories.size() * TAB_W + (categories.size() - 1) * TAB_GAP;
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
                getTabsBarX() - 16,
                getTabsBarY() - 12,
                getTabsBarTotalWidth() + 32,
                TABS_BAR_H + 24
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

        // --- HUD editor widgets ---
        if (MacClient.hudEditorOpen) {
            boolean mouseDown = org.lwjgl.glfw.GLFW.glfwGetMouseButton(
                    MinecraftClient.getInstance().getWindow().getHandle(),
                    org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT
            ) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
            net.macos.client.hud.WidgetManager.INSTANCE.renderAll(
                    ctx, mouseX, mouseY, delta, mouseDown
            );
        }
    }

    // ============================================================
    // PANEL (header, background, buttons)
    // ============================================================

    private void drawPanel(DrawContext ctx, int mouseX, int mouseY, int accent, int openAlpha) {
        int px = panelX;
        int py = panelY;

        // Glass background
        int bgAlpha = (int) (0x55 * (openAlpha / 255f));
        drawRoundRect(ctx, px, py, PANEL_W, PANEL_H, 18,
                (bgAlpha << 24) | 0x101828);

        // Specular top highlight
        GlassRenderer.specular(ctx, px, py, PANEL_W, PANEL_H, 18,
                ((int) (0x22 * (openAlpha / 255f)) << 24) | 0xFFFFFF);

        // Border
        drawBorderRounded(ctx, px, py, PANEL_W, PANEL_H, 18,
                ((int) (0x18 * (openAlpha / 255f)) << 24) | 0xFFFFFF);

        // --- Logo icon ---
        int iconX = px + 22;
        int iconY = py + 18;
        IconRenderer.draw(ctx, MacIcons.GENERAL, iconX, iconY,
                (openAlpha << 24) | accent);

        // --- Title "Aetherion" ---
        int titleX = iconX + 16 + 6;
        AetherionFont.draw(ctx, "Aetherion",
                titleX, iconY + 1, (openAlpha << 24) | C_TEXT_PRIMARY);

        // --- Subtitle "Settings" ---
        int subtitleX = titleX + AetherionFont.width("Aetherion") + 10;
        AetherionFont.draw(ctx, "Settings",
                subtitleX, iconY + 1, (openAlpha << 24) | C_TEXT_TERTIARY);

        // --- Reset button ---
        int resetW = 56;
        int resetH = 22;
        int resetX = px + PANEL_W - 26 - resetW - 34;
        int resetY = py + 16;
        boolean resetHover = mouseX >= resetX && mouseX <= resetX + resetW
                && mouseY >= resetY && mouseY <= resetY + resetH;
        if (resetHover) {
            drawRoundRect(ctx, resetX, resetY, resetW, resetH, 6,
                    0x20FFFFFF);
        }
        String resetText = "Reset";
        int resetTextW = AetherionFont.width(resetText);
        AetherionFont.draw(ctx, resetText,
                resetX + (resetW - resetTextW) / 2, resetY + 7,
                (openAlpha << 24) | (resetHover ? C_TEXT_PRIMARY : C_TEXT_SECONDARY));

        // --- Close button ---
        int closeSize = 22;
        int closeX = px + PANEL_W - 26 - closeSize;
        int closeY = py + 16;
        boolean closeHover = mouseX >= closeX && mouseX <= closeX + closeSize
                && mouseY >= closeY && mouseY <= closeY + closeSize;
        if (closeHover) {
            drawRoundRect(ctx, closeX, closeY, closeSize, closeSize, 6,
                    (openAlpha << 24) | 0x30FF4455);
        }
        IconRenderer.draw(ctx, MacIcons.CLOSE,
                closeX + 5, closeY + 6,
                (openAlpha << 24) | (closeHover ? 0xFF4455 : C_TEXT_SECONDARY));

        // --- Divider under header ---
        ctx.fill(px + 18, py + HEADER_H, px + PANEL_W - 18, py + HEADER_H + 1,
                ((int) (0x14 * (openAlpha / 255f)) << 24) | 0xFFFFFF);

        // --- Category label ---
        Category current = categories.get(selectedCategory);
        AetherionFont.draw(ctx, current.name,
                px + 24, py + HEADER_H + 12, (openAlpha << 24) | accent);

        // Count
        String countText = current.options.size() + " options";
        int countW = AetherionFont.width(countText);
        AetherionFont.draw(ctx, countText,
                px + PANEL_W - 24 - countW, py + HEADER_H + 12,
                (openAlpha << 24) | C_TEXT_TERTIARY);
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

        ctx.enableScissor(px + 12, listTop - 4, px + PANEL_W - 12, listBottom);

        Option hoveredOption = null;

        for (int i = 0; i < current.options.size(); i++) {
            Option opt = current.options.get(i);
            int oy = listTop + i * OPT_ROW_H - (int) scrollOffset;

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

            // Row hover background
            if (hoverT > 0.01f) {
                int a = (int) (0x14 * hoverT * (openAlpha / 255f));
                drawRoundRect(ctx, listLeft - 4, oy + 4, rowW + 8,
                        OPT_ROW_H - 8, 8, (a << 24) | 0xFFFFFF);
            }

            // Label
            int textY = oy + (OPT_ROW_H - 9) / 2;
            int nameColor = blend(C_TEXT_PRIMARY, 0xFFFFFFFF, hoverT);
            AetherionFont.draw(ctx, opt.name, listLeft, textY,
                    (openAlpha << 24) | nameColor);

            // Control
            int ctrlRight = listRight - 4;
            if (opt.isBool) {
                drawToggle(ctx, opt, anim, ctrlRight, oy, delta, accent, openAlpha);
            } else if (opt.isSlider) {
                drawSlider(ctx, opt, anim, listLeft, listRight, oy, delta, accent, openAlpha);
            } else if (opt.isText) {
                drawTextOption(ctx, opt, ctrlRight, oy, openAlpha);
            } else if (opt.isDropdown) {
                drawDropdownButton(ctx, opt, anim, ctrlRight, oy, delta, accent, openAlpha);
            }
        }

        ctx.disableScissor();

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

        int tw = 36, th = 20;
        int tx = rightX - tw;
        int ty = rowY + (OPT_ROW_H - th) / 2;

        // Track: lerp between off/on colors
        int trackOff = 0x30FFFFFF;
        int trackOn  = (openAlpha << 24) | accent;
        int trackColor = lerpColor(trackOff, trackOn, t);

        drawPill(ctx, tx, ty, tw, th, trackColor);

        // Knob shadow
        int knobD = 16;
        int knobX = tx + 2 + (int) ((tw - knobD - 4) * t);
        int knobY = ty + 2;
        drawCircle(ctx, knobX + knobD / 2, knobY + knobD / 2 + 1, knobD / 2,
                (int) (0x40 * (openAlpha / 255f)) << 24);
        // Knob
        drawCircle(ctx, knobX + knobD / 2, knobY + knobD / 2, knobD / 2,
                (openAlpha << 24) | C_KNOB);
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

        // Value text width
        String display = opt.isFloat
                ? String.format("%.2f", value)
                : String.valueOf((int) value);
        int valW = AetherionFont.width(display);

        int valX = listRight - valW;
        int trackRight = valX - 12;
        int trackLeft = listLeft + (listRight - listLeft) / 2;
        int trackW = trackRight - trackLeft;
        int trackH = 4;
        int trackY = rowY + (OPT_ROW_H - trackH) / 2;

        // Track
        drawPill(ctx, trackLeft, trackY, trackW, trackH,
                ((int) (0x25 * (openAlpha / 255f)) << 24) | 0xFFFFFF);

        // Fill
        int fillW = (int) (trackW * animPct);
        if (fillW > 0) {
            drawPill(ctx, trackLeft, trackY, Math.max(fillW, trackH), trackH,
                    (openAlpha << 24) | accent);
        }

        // Knob
        int knobX = trackLeft + (int) (trackW * animPct);
        int knobCY = trackY + trackH / 2;
        // Shadow
        drawCircle(ctx, knobX, knobCY + 1, 6, (0x40 * (openAlpha / 255f)) > 0
                ? ((int) (0x40 * (openAlpha / 255f)) << 24) : 0);
        // Body
        drawCircle(ctx, knobX, knobCY, 5, (openAlpha << 24) | C_KNOB);

        // Value
        AetherionFont.draw(ctx, display, valX, rowY + (OPT_ROW_H - 9) / 2,
                (openAlpha << 24) | C_TEXT_SECONDARY);
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
        ctx.disableScissor();

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
        int tbW = getTabsBarTotalWidth();
        int tbH = TABS_BAR_H;

        int alpha = (int) (openT * 255);

        // Dock capsule
        drawRoundRect(ctx, tbX - 12, tbY - 6, tbW + 24, tbH + 12, 22,
                ((int) (0x99 * openT) << 24) | 0x101828);
        GlassRenderer.specular(ctx, tbX - 12, tbY - 6, tbW + 24, tbH + 12, 22,
                ((int) (0x28 * openT) << 24) | 0xFFFFFF);
        drawBorderRounded(ctx, tbX - 12, tbY - 6, tbW + 24, tbH + 12, 22,
                ((int) (0x20 * openT) << 24) | 0xFFFFFF);

        // Animated selection pill
        float targetX = selectedCategory * (TAB_W + TAB_GAP);
        tabSelectionX.setTarget(targetX);
        float pillX = tabSelectionX.tick(delta);

        if (openT > 0.5f) {
            drawRoundRect(ctx,
                    tbX + (int) pillX, tbY, TAB_W, tbH, 16,
                    ((int) (openT * 255) << 24) | accent);
        }

        // Tab labels
        for (int i = 0; i < categories.size(); i++) {
            Category cat = categories.get(i);
            int tx = tbX + i * (TAB_W + TAB_GAP);
            boolean selected = (i == selectedCategory);
            boolean hovered = mouseX >= tx && mouseX <= tx + TAB_W
                    && mouseY >= tbY && mouseY <= tbY + tbH;

            // Hover pill for non-selected
            if (hovered && !selected) {
                drawRoundRect(ctx, tx, tbY, TAB_W, tbH, 16,
                        ((int) (0x18 * openT) << 24) | 0xFFFFFF);
            }

            int tc = selected ? 0xFF101018
                    : ((int) (0xE0 * openT) << 24) | 0xFFFFFF;
            int textW = AetherionFont.width(cat.name);
            AetherionFont.draw(ctx, cat.name,
                    tx + (TAB_W - textW) / 2, tbY + (tbH - 9) / 2, tc);
        }
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

        // --- Close button ---
        int closeSize = 22;
        int closeX = px + PANEL_W - 26 - closeSize;
        int closeY = py + 16;
        if (mx >= closeX && mx <= closeX + closeSize
                && my >= closeY && my <= closeY + closeSize) {
            this.close();
            return true;
        }

        // --- Reset ---
        int resetW = 56;
        int resetH = 22;
        int resetX = px + PANEL_W - 26 - resetW - 34;
        int resetY = py + 16;
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
            int tx = tbX + i * (TAB_W + TAB_GAP);
            if (mx >= tx && mx <= tx + TAB_W && my >= tbY && my <= tbY + TABS_BAR_H) {
                selectedCategory = i;
                scrollOffset = 0;
                updateMaxScroll();
                return true;
            }
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

                int ctrlRight = listRight - 4;

                if (opt.isBool) {
                    int tw = 36, th = 20;
                    int tx = ctrlRight - tw;
                    int ty = oy + (OPT_ROW_H - th) / 2;
                    if (mx >= tx && mx <= tx + tw && my >= ty && my <= ty + th) {
                        opt.set(!(boolean) opt.get());
                        ConfigManager.save();
                        return true;
                    }
                } else if (opt.isSlider) {
                    float value = ((Number) opt.get()).floatValue();
                    float min = opt.isFloat ? opt.floatMin : opt.min;
                    float max = opt.isFloat ? opt.floatMax : opt.max;
                    String display = opt.isFloat
                            ? String.format("%.2f", value)
                            : String.valueOf((int) value);
                    int valW = AetherionFont.width(display);
                    int valX = listRight - valW;
                    int trackRight = valX - 12;
                    int trackLeft = listLeft + (listRight - listLeft) / 2;
                    int trackW = trackRight - trackLeft;
                    int trackY = oy + (OPT_ROW_H - 4) / 2;

                    if (mx >= trackLeft - 6 && mx <= trackRight + 6
                            && my >= trackY - 8 && my <= trackY + 12) {
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
                        try {
                            int initial = 0xFF000000 | Integer.parseInt(
                                    value.replace("#", ""), 16);
                            MinecraftClient.getInstance()
                                    .setScreen(new ColorPickerScreen(this, initial));
                        } catch (Exception e) {
                            MinecraftClient.getInstance()
                                    .setScreen(new ColorPickerScreen(this, 0xFF00D4FF));
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
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (draggingPanel) { draggingPanel = false; return true; }
        if (draggingSlider != null) { draggingSlider = null; ConfigManager.save(); return true; }
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
        if (w <= 0 || h <= 0) return;
        int a = (color >>> 24) & 0xFF;
        if (a <= 0) return;

        if (r <= 0) {
            ctx.fill(x, y, x + w, y + h, color);
            return;
        }
        r = Math.min(r, Math.min(w / 2, h / 2));

        // Тело
        ctx.fill(x + r, y, x + w - r, y + h, color);
        ctx.fill(x, y + r, x + w, y + h - r, color);

        // 4 угла — по одной полосе на строку
        for (int i = 0; i < r; i++) {
            int dy = r - i - 1;
            int span = (int) Math.sqrt(r * r - dy * dy);
            ctx.fill(x + r - span, y + i, x + r, y + i + 1, color);
            ctx.fill(x + w - r, y + i, x + w - r + span, y + i + 1, color);
            ctx.fill(x + r - span, y + h - 1 - i, x + r, y + h - i, color);
            ctx.fill(x + w - r, y + h - 1 - i, x + w - r + span, y + h - i, color);
        }
    }

    private static void drawPill(DrawContext ctx, int x, int y, int w, int h, int color) {
        drawRoundRect(ctx, x, y, w, h, h / 2, color);
    }

    private static void drawCircle(DrawContext ctx, int cx, int cy, int r, int color) {
        drawRoundRect(ctx, cx - r, cy - r, r * 2, r * 2, r, color);
    }

    /** 1px border along a rounded rect. */
    private static void drawBorderRounded(DrawContext ctx, int x, int y, int w, int h,
                                          int radius, int color) {
        int a = (color >>> 24) & 0xFF;
        if (a <= 0) return;
        ctx.fill(x + radius, y, x + w - radius, y + 1, color);
        ctx.fill(x + radius, y + h - 1, x + w - radius, y + h, color);
        ctx.fill(x, y + radius, x + 1, y + h - radius, color);
        ctx.fill(x + w - 1, y + radius, x + w, y + h - radius, color);

        for (int i = 0; i < radius; i++) {
            float dx = radius - i - 0.5f;
            for (int j = 0; j < radius; j++) {
                float dy = radius - j - 0.5f;
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                if (d >= radius - 1.5f && d <= radius + 0.5f) {
                    ctx.fill(x + i, y + j, x + i + 1, y + j + 1, color);
                    ctx.fill(x + w - 1 - i, y + j, x + w - i, y + j + 1, color);
                    ctx.fill(x + i, y + h - 1 - j, x + i + 1, y + h - j, color);
                    ctx.fill(x + w - 1 - i, y + h - 1 - j, x + w - i, y + h - j, color);
                }
            }
        }
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
        boolean isBool, isSlider, isText, isDropdown;

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

        Object get() { return getter.get(); }
        void set(Object v) { setter.accept(v); }
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
}