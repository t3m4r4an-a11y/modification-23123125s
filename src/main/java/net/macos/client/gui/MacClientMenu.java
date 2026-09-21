package net.macos.client.gui;

import net.macos.client.MacClient;
import net.macos.client.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class MacClientMenu extends Screen implements BlurableScreen {

    private static final int PANEL_W = 480;
    private static final int PANEL_H = 300;
    private static final int HEADER_H = 28;
    private static final int TABS_BAR_H = 40;
    private static final int TAB_W = 100;
    private static final int TAB_GAP = 6;
    private static final int TABS_BOTTOM_MARGIN = 30;
    private static final int OPT_ROW_H = 26;
    private static final int OPT_LIST_TOP = HEADER_H + 30;

    private int panelX, panelY;
    private int selectedCategory = 0;
    private double scrollOffset = 0;
    private double maxScroll = 0;

    private boolean draggingPanel = false;
    private WidgetEntry draggingWidget = null;
    private int dragOffX = 0, dragOffY = 0;

    private Option draggingSlider = null;
    private int sliderX = 0, sliderW = 0;

    private Option openDropdown = null;
    private int dropdownScroll = 0;              
    private static final int DROPDOWN_MAX_VISIBLE = 7;  
    private final List<Category> categories = new ArrayList<>();
    private final List<WidgetEntry> widgetEntries = new ArrayList<>();

    public MacClientMenu() {
        super(Text.literal("MacClient"));
    }

    @Override
    protected void init() {
        super.init();
        MacClient.hudEditorOpen = true;
        panelX = (width - PANEL_W) / 2;
        panelY = (height - PANEL_H) / 2 - 20;
        scrollOffset = 0;

        categories.clear();
        widgetEntries.clear();

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
        // === HitFX ===
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

        // === CritFX ===
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
        // === Smooth Swing ===
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
                default -> 0; // DIAGONAL
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
        // === Aspect Ratio ===
        visuals.add(Option.bool("Aspect Ratio", "Растянуть картинку по горизонтали.",
            () -> ConfigManager.INSTANCE.enableAspectRatio,
            v -> ConfigManager.INSTANCE.enableAspectRatio = v));
        visuals.add(Option.sliderFloat("Aspect Multiplier", "0.75=4:3, 1.0=Vanilla, 1.33=21:9.",
            () -> ConfigManager.INSTANCE.aspectRatio,
            v -> ConfigManager.INSTANCE.aspectRatio = v, 0.5f, 2.0f, 0.05f));            
        categories.add(new Category("Visuals", visuals));
        // ===== VIEWMODEL =====
        List<Option> viewmodel = new ArrayList<>();

        // --- Main Hand ---
        viewmodel.add(Option.bool("Main Hand VM", "Настройки правой руки (главной).",
            () -> ConfigManager.INSTANCE.enableViewModel,
            v -> ConfigManager.INSTANCE.enableViewModel = v));
        viewmodel.add(Option.sliderFloat("Main Offset X", "Смещение главной руки по X.",
            () -> ConfigManager.INSTANCE.vmOffsetX,
            v -> ConfigManager.INSTANCE.vmOffsetX = v, -2f, 2f, 0.01f));
        viewmodel.add(Option.sliderFloat("Main Offset Y", "Смещение главной руки по Y.",
            () -> ConfigManager.INSTANCE.vmOffsetY,
            v -> ConfigManager.INSTANCE.vmOffsetY = v, -2f, 2f, 0.01f));
        viewmodel.add(Option.sliderFloat("Main Offset Z", "Смещение главной руки по Z (вперёд/назад).",
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

        // --- Off Hand ---
        viewmodel.add(Option.bool("Off Hand VM", "Настройки левой руки (второй).",
            () -> ConfigManager.INSTANCE.enableOffHandViewModel,
            v -> ConfigManager.INSTANCE.enableOffHandViewModel = v));
        viewmodel.add(Option.sliderFloat("Off Offset X", "Смещение второй руки по X (расстояние между рук).",
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

        // --- Features ---
        viewmodel.add(Option.bool("Free Look (Alt)", "Осмотр камерой при зажатом Alt.",
            () -> ConfigManager.INSTANCE.enableFreeLook,
            v -> ConfigManager.INSTANCE.enableFreeLook = v));

        categories.add(new Category("ViewModel", viewmodel));
        // ===== MISC =====
        List<Option> misc = new ArrayList<>();

        // --- Zoom ---
        misc.add(Option.bool("Zoom", "Приближение (C).",
            () -> ConfigManager.INSTANCE.enableZoom,
            v -> ConfigManager.INSTANCE.enableZoom = v));
        misc.add(Option.slider("Zoom FOV", "FOV при зуме.",
            () -> ConfigManager.INSTANCE.zoomFov,
            v -> ConfigManager.INSTANCE.zoomFov = v, 5, 50, 1));
        misc.add(Option.slider("Zoom Speed", "Скорость зума.",
            () -> ConfigManager.INSTANCE.zoomSpeed,
            v -> ConfigManager.INSTANCE.zoomSpeed = v, 5, 100, 5));

        // --- Hit Sound ---
        misc.add(Option.bool("Hit Sound", "Звук при ударе.",
            () -> ConfigManager.INSTANCE.enableHitSound,
            v -> ConfigManager.INSTANCE.enableHitSound = v));
        misc.add(Option.dropdown("Hit Sound Preset", "Пресет звука удара.",
            () -> indexOfHit(ConfigManager.INSTANCE.hitSoundPreset),
            v -> ConfigManager.INSTANCE.hitSoundPreset = HIT_PRESETS[v],
            HIT_LABELS));

        // --- Kill Sound ---
        misc.add(Option.bool("Kill Sound", "Звук при убийстве.",
            () -> ConfigManager.INSTANCE.enableKillSound,
            v -> ConfigManager.INSTANCE.enableKillSound = v));
        misc.add(Option.dropdown("Kill Sound Preset", "Пресет звука убийства.",
            () -> indexOfHit(ConfigManager.INSTANCE.killSoundPreset),
            v -> ConfigManager.INSTANCE.killSoundPreset = HIT_PRESETS[v],
            HIT_LABELS));

        // --- Volume / Pitch ---
        misc.add(Option.sliderFloat("Sound Volume", "Общая громкость звуков.",
            () -> ConfigManager.INSTANCE.hitSoundVolume,
            v -> ConfigManager.INSTANCE.hitSoundVolume = v, 0f, 2f, 0.05f));
        misc.add(Option.sliderFloat("Sound Pitch", "Общий питч звуков.",
            () -> ConfigManager.INSTANCE.hitSoundPitch,
            v -> ConfigManager.INSTANCE.hitSoundPitch = v, 0.5f, 2f, 0.05f));

        // --- Misc features ---
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

        // ===== Widgets =====


        updateMaxScroll();
    }

    @Override
    public void removed() {
        super.removed();
        MacClient.hudEditorOpen = false;
    }

    private void updateMaxScroll() {
        Category cat = categories.get(selectedCategory);
        int visibleHeight = PANEL_H - OPT_LIST_TOP - 8;
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
    @Override
    public java.util.List<int[]> getBlurRegions() {
        java.util.List<int[]> list = new java.util.ArrayList<>();

        // 1. Панель меню (с паддингом 8px вокруг)
        list.add(new int[]{
            panelX - 8,
            panelY - 8,
            PANEL_W + 16,
            PANEL_H + 16
        });

        // 2. Таббар (dock) снизу
        list.add(new int[]{
            getTabsBarX() - 8,
            getTabsBarY() - 4,
            getTabsBarTotalWidth() + 16,
            TABS_BAR_H + 8
        });

        return list;
    }
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int accent;
        try {
            accent = 0xFF000000 | Integer.parseInt(ConfigManager.INSTANCE.accentColor.replace("#", ""), 16);
        } catch (Exception e) {
            accent = 0xFF00D4FF;
        }

        if (ConfigManager.INSTANCE.hudEditorShowGrid) {
            int grid = Math.max(4, ConfigManager.INSTANCE.hudEditorGrid);
            int gridColor = 0x18FFFFFF;
            for (int x = 0; x < width; x += grid) context.fill(x, 0, x + 1, height, gridColor);
            for (int y = 0; y < height; y += grid) context.fill(0, y, width, y + 1, gridColor);
        }

        for (WidgetEntry w : widgetEntries) {
            int wx = w.getX();
            int wy = w.getY();
            int ww = w.getWidth();
            int wh = w.getHeight();
            boolean hov = mouseX >= wx && mouseX <= wx + ww && mouseY >= wy && mouseY <= wy + wh;

            int boxColor = (hov || draggingWidget == w) ? 0xFF00D4FF : 0x50FFFFFF;
            context.fill(wx, wy, wx + ww, wy + 1, boxColor);
            context.fill(wx, wy + wh - 1, wx + ww, wy + wh, boxColor);
            context.fill(wx, wy, wx + 1, wy + wh, boxColor);
            context.fill(wx + ww - 1, wy, wx + ww, wy + wh, boxColor);

            if (hov || draggingWidget == w) {
                String label = w.name;
                int tw = textRenderer.getWidth(label);
                context.fill(wx, wy - 12, wx + tw + 6, wy - 1, 0xE000D4FF);
                context.drawTextWithShadow(textRenderer, label, wx + 3, wy - 10, 0xFF000000);
            }
        }

        int px = panelX;
        int py = panelY;

        context.fill(px, py, px + PANEL_W, py + PANEL_H, 0xF01A1A2E);
        context.fill(px, py, px + PANEL_W, py + 1, accent);
        context.fill(px, py + PANEL_H - 1, px + PANEL_W, py + PANEL_H, accent);
        context.fill(px, py, px + 1, py + PANEL_H, accent);
        context.fill(px + PANEL_W - 1, py, px + PANEL_W, py + PANEL_H, accent);

        context.drawTextWithShadow(textRenderer, "MacClient Settings", px + 12, py + 10, 0xFFFFFFFF);

        int closeX = px + PANEL_W - 24;
        int closeY = py + 8;
        boolean closeHover = mouseX >= closeX - 2 && mouseX <= closeX + 14
                          && mouseY >= closeY - 2 && mouseY <= closeY + 14;
        context.fill(closeX, closeY, closeX + 12, closeY + 12, closeHover ? 0xFFFF4455 : 0x40FFFFFF);
        context.drawTextWithShadow(textRenderer, "x", closeX + 3, closeY + 2, 0xFFFFFFFF);

        int resetX = px + PANEL_W - 24 - 60;
        int resetY = py + 8;
        boolean resetHover = mouseX >= resetX && mouseX <= resetX + 52
                          && mouseY >= resetY && mouseY <= resetY + 12;
        context.fill(resetX, resetY, resetX + 52, resetY + 12, resetHover ? 0xFFAA3333 : 0x40FFFFFF);
        context.drawTextWithShadow(textRenderer, "Reset", resetX + 10, resetY + 2, 0xFFFFFFFF);

        context.fill(px + 8, py + HEADER_H - 1, px + PANEL_W - 8, py + HEADER_H, 0x40FFFFFF);

        Category current = categories.get(selectedCategory);
        context.drawTextWithShadow(textRenderer, current.name, px + 12, py + HEADER_H + 6, 0xFF00D4FF);
        context.fill(px + 12, py + HEADER_H + 20, px + PANEL_W - 12, py + HEADER_H + 21, 0x30FFFFFF);

        int optX = px + 20;
        int optY = py + OPT_LIST_TOP;
        int listBottom = py + PANEL_H - 8;

        context.enableScissor(px + 8, py + OPT_LIST_TOP - 4, px + PANEL_W - 8, listBottom);

        Option hoveredOption = null;

        for (int i = 0; i < current.options.size(); i++) {
            Option opt = current.options.get(i);
            int oy = optY + i * OPT_ROW_H - (int) scrollOffset;

            if (oy + OPT_ROW_H < py + OPT_LIST_TOP - 4) continue;
            if (oy > listBottom) break;

            boolean optHover = mouseX >= px + 8 && mouseX <= px + PANEL_W - 8
                            && mouseY >= oy - 2 && mouseY <= oy + 20
                            && mouseY >= py + OPT_LIST_TOP - 4 && mouseY <= listBottom;
            if (optHover) {
                context.fill(px + 8, oy - 2, px + PANEL_W - 8, oy + 20, 0x15FFFFFF);
                hoveredOption = opt;
            }

            context.drawTextWithShadow(textRenderer, opt.name, optX, oy + 4, 0xFFFFFFFF);

            if (opt.isBool) {
                boolean value = (boolean) opt.get();
                int cbX = px + PANEL_W - 34;
                int cbY = oy + 2;
                context.fill(cbX, cbY, cbX + 14, cbY + 14, value ? accent : 0x40FFFFFF);
                if (value) context.fill(cbX + 3, cbY + 3, cbX + 11, cbY + 11, 0xFFFFFFFF);
            } else if (opt.isSlider) {
                float value;
                float min, max;
                String display;

                if (opt.isFloat) {
                    value = ((Number) opt.get()).floatValue();
                    min = opt.floatMin;
                    max = opt.floatMax;
                    display = String.format("%.2f", value);
                } else {
                    value = ((Number) opt.get()).floatValue();
                    min = opt.min;
                    max = opt.max;
                    display = String.valueOf((int) value);
                }

                int slX = optX + 130;
                int slY = oy + 4;
                int slW = px + PANEL_W - slX - 54;
                int slH = 10;

                context.fill(slX, slY, slX + slW, slY + slH, 0x40FFFFFF);
                float pct = (value - min) / (max - min);
                context.fill(slX, slY, slX + (int)(slW * pct), slY + slH, accent);
                int knobX = slX + (int)(slW * pct) - 2;
                context.fill(knobX, slY - 2, knobX + 4, slY + slH + 2, 0xFFFFFFFF);
                context.drawTextWithShadow(textRenderer, display, slX + slW + 6, slY + 1, 0xFFFFFFFF);
            } else if (opt.isText) {
                String value = (String) opt.get();
                int txX = optX + 130;
                int txY = oy + 2;
                int txW = px + PANEL_W - txX - 20;
                context.fill(txX, txY, txX + txW, txY + 16, 0x30FFFFFF);
                try {
                    int col = 0xFF000000 | Integer.parseInt(value.replace("#", ""), 16);
                    context.fill(txX + 2, txY + 2, txX + 14, txY + 14, col);
                    context.drawTextWithShadow(textRenderer, value, txX + 20, txY + 5, 0xFFFFFFFF);
                } catch (Exception e) {
                    context.drawTextWithShadow(textRenderer, value, txX + 4, txY + 5, 0xFFFFFFFF);
                }
            } else if (opt.isDropdown) {
                int value = (int) opt.get();
                int ddX = optX + 130;
                int ddY = oy + 2;
                int ddW = px + PANEL_W - ddX - 20;
                int ddH = 16;
                context.fill(ddX, ddY, ddX + ddW, ddY + ddH, 0x30FFFFFF);
                context.drawTextWithShadow(textRenderer, opt.choices[value], ddX + 6, ddY + 4, 0xFFFFFFFF);
                context.drawTextWithShadow(textRenderer, "v", ddX + ddW - 10, ddY + 4, 0xC0FFFFFF);
            }
        }

        context.disableScissor();

        if (maxScroll > 0) {
            int sbX = px + PANEL_W - 4;
            int sbY = py + OPT_LIST_TOP;
            int sbH = listBottom - sbY;
            int barH = Math.max(20, (int)(sbH * (sbH / (float)(sbH + maxScroll))));
            int barY = sbY + (int)((sbH - barH) * (scrollOffset / maxScroll));
            context.fill(sbX, sbY, sbX + 2, sbY + sbH, 0x30FFFFFF);
            context.fill(sbX, barY, sbX + 2, barY + barH, accent);
        }
        if (openDropdown != null) {
            int idx = current.options.indexOf(openDropdown);
            if (idx >= 0) {
                int oy = optY + idx * OPT_ROW_H - (int) scrollOffset;
                int ddX = optX + 130;
                int ddY = oy + 2 + 16;
                int ddW = px + PANEL_W - ddX - 20;
                int itemH = 16;

                int visible = Math.min(DROPDOWN_MAX_VISIBLE, openDropdown.choices.length);
                int listH = visible * itemH;

                // Ограничиваем скролл
                int maxScroll = Math.max(0, openDropdown.choices.length - DROPDOWN_MAX_VISIBLE);
                dropdownScroll = Math.max(0, Math.min(maxScroll, dropdownScroll));

                // Фон dropdown
                context.fill(ddX, ddY, ddX + ddW, ddY + listH, 0xF02A2A3E);
                context.fill(ddX, ddY, ddX + ddW, ddY + 1, accent);

                // Обводка
                context.fill(ddX, ddY, ddX + 1, ddY + listH, 0x40FFFFFF);
                context.fill(ddX + ddW - 1, ddY, ddX + ddW, ddY + listH, 0x40FFFFFF);
                context.fill(ddX, ddY + listH - 1, ddX + ddW, ddY + listH, 0x40FFFFFF);

                // Scissor — обрезаем всё что не влезает
                context.enableScissor(ddX, ddY, ddX + ddW, ddY + listH);

                for (int i = 0; i < openDropdown.choices.length; i++) {
                    int iy = ddY + (i - dropdownScroll) * itemH;

                    // Пропускаем то что вне видимости
                    if (iy + itemH < ddY || iy > ddY + listH) continue;

                    boolean hov = mouseX >= ddX && mouseX <= ddX + ddW
                               && mouseY >= iy && mouseY <= iy + itemH
                               && mouseY >= ddY && mouseY <= ddY + listH;
                    if (hov) context.fill(ddX, iy, ddX + ddW, iy + itemH, 0x40FFFFFF);
                    context.drawTextWithShadow(textRenderer, openDropdown.choices[i], ddX + 6, iy + 4, 0xFFFFFFFF);
                }

                context.disableScissor();

                // Скроллбар справа если нужен
                if (maxScroll > 0) {
                    int sbX = ddX + ddW - 3;
                    int barH = Math.max(8, (int)(listH * (DROPDOWN_MAX_VISIBLE / (float) openDropdown.choices.length)));
                    int barY = ddY + (int)((listH - barH) * (dropdownScroll / (float) maxScroll));
                    context.fill(sbX, ddY, sbX + 2, ddY + listH, 0x30FFFFFF);
                    context.fill(sbX, barY, sbX + 2, barY + barH, accent);
                }
            }
        }

        renderTabsBar(context, mouseX, mouseY, accent);

        if (hoveredOption != null && hoveredOption.description != null && openDropdown == null) {
            int tw = textRenderer.getWidth(hoveredOption.description) + 8;
            int th = 14;
            int tx = Math.min(mouseX + 10, width - tw - 4);
            int ty = mouseY + 10;
            context.fill(tx - 1, ty - 1, tx + tw + 1, ty + th + 1, 0xC0000000);
            context.fill(tx, ty, tx + tw, ty + th, 0xFF1A1A2E);
            context.drawTextWithShadow(textRenderer, hoveredOption.description, tx + 4, ty + 3, 0xFFFFFFFF);
        }
                // === HUD-виджеты поверх меню (для редактирования) ===
        if (MacClient.hudEditorOpen) {
            boolean mouseDown = org.lwjgl.glfw.GLFW.glfwGetMouseButton(
                MinecraftClient.getInstance().getWindow().getHandle(),
                org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT
            ) == org.lwjgl.glfw.GLFW.GLFW_PRESS;

            net.macos.client.hud.WidgetManager.INSTANCE.renderAll(
                context, mouseX, mouseY, delta, mouseDown
            );
        }
    }

    private void renderTabsBar(DrawContext context, int mouseX, int mouseY, int accent) {
        int tbX = getTabsBarX();
        int tbY = getTabsBarY();
        int tbW = getTabsBarTotalWidth();
        int tbH = TABS_BAR_H;

        context.fill(tbX - 8, tbY - 4, tbX + tbW + 8, tbY + tbH + 4, 0xE01A1A2E);
        context.fill(tbX - 8, tbY - 4, tbX + tbW + 8, tbY - 3, accent);
        context.fill(tbX - 8, tbY + tbH + 3, tbX + tbW + 8, tbY + tbH + 4, accent);
        context.fill(tbX - 8, tbY - 4, tbX - 7, tbY + tbH + 4, accent);
        context.fill(tbX + tbW + 7, tbY - 4, tbX + tbW + 8, tbY + tbH + 4, accent);

        for (int i = 0; i < categories.size(); i++) {
            Category cat = categories.get(i);
            int tx = tbX + i * (TAB_W + TAB_GAP);
            int ty = tbY;
            int tw = TAB_W;
            int th = tbH;

            boolean selected = (i == selectedCategory);
            boolean hovered = mouseX >= tx && mouseX <= tx + tw
                           && mouseY >= ty && mouseY <= ty + th;

            int bg;
            if (selected) bg = accent;
            else if (hovered) bg = 0x40FFFFFF;
            else bg = 0x20FFFFFF;
            context.fill(tx, ty, tx + tw, ty + th, bg);

            int tc = selected ? 0xFF000000 : 0xFFFFFFFF;
            int textW = textRenderer.getWidth(cat.name);
            context.drawTextWithShadow(textRenderer, cat.name,
                tx + (tw - textW) / 2, ty + (th - 8) / 2, tc);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);

        int px = panelX;
        int py = panelY;

        if (openDropdown != null) {
            Category current = categories.get(selectedCategory);
            int idx = current.options.indexOf(openDropdown);
            if (idx >= 0) {
                int optY = py + OPT_LIST_TOP;
                int oy = optY + idx * OPT_ROW_H - (int) scrollOffset;
                int ddX = px + 20 + 130;
                int ddY = oy + 2 + 16;
                int ddW = px + PANEL_W - ddX - 20;
                int itemH = 16;
                int listH = Math.min(DROPDOWN_MAX_VISIBLE, openDropdown.choices.length) * itemH;

                // Клик внутри области списка
                if (mx >= ddX && mx <= ddX + ddW && my >= ddY && my <= ddY + listH) {
                    for (int i = 0; i < openDropdown.choices.length; i++) {
                        int iy = ddY + (i - dropdownScroll) * itemH;
                        if (iy + itemH < ddY || iy > ddY + listH) continue;
                        if (mx >= ddX && mx <= ddX + ddW && my >= iy && my <= iy + itemH) {
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

        int closeX = px + PANEL_W - 24;
        int closeY = py + 8;
        if (mx >= closeX && mx <= closeX + 12 && my >= closeY && my <= closeY + 12) {
            this.close();
            return true;
        }

        int resetX = px + PANEL_W - 24 - 60;
        int resetY = py + 8;
        if (mx >= resetX && mx <= resetX + 52 && my >= resetY && my <= resetY + 12) {
            for (WidgetEntry w : widgetEntries) w.resetPos();
            ConfigManager.save();
            return true;
        }

        if (mx >= px && mx <= px + PANEL_W - 90 && my >= py && my <= py + HEADER_H) {
            draggingPanel = true;
            dragOffX = (int) mx - px;
            dragOffY = (int) my - py;
            return true;
        }

        int tbX = getTabsBarX();
        int tbY = getTabsBarY();
        for (int i = 0; i < categories.size(); i++) {
            int tx = tbX + i * (TAB_W + TAB_GAP);
            int ty = tbY;
            if (mx >= tx && mx <= tx + TAB_W && my >= ty && my <= ty + TABS_BAR_H) {
                selectedCategory = i;
                scrollOffset = 0;
                updateMaxScroll();
                return true;
            }
        }

        int optX = px + 20;
        int optY = py + OPT_LIST_TOP;
        int listBottom = py + PANEL_H - 8;

        if (my >= py + OPT_LIST_TOP - 4 && my <= listBottom) {
            Category current = categories.get(selectedCategory);
            for (int i = 0; i < current.options.size(); i++) {
                Option opt = current.options.get(i);
                int oy = optY + i * OPT_ROW_H - (int) scrollOffset;

                if (opt.isBool) {
                    int cbX = px + PANEL_W - 34;
                    int cbY = oy + 2;
                    if (mx >= cbX && mx <= cbX + 14 && my >= cbY && my <= cbY + 14) {
                        opt.set(!(boolean) opt.get());
                        ConfigManager.save();
                        return true;
                    }
                } else if (opt.isSlider) {
                    int slX = optX + 130;
                    int slY = oy + 4;
                    int slW = px + PANEL_W - slX - 54;
                    int slH = 10;
                    if (mx >= slX && mx <= slX + slW && my >= slY - 4 && my <= slY + slH + 4) {
                        draggingSlider = opt;
                        sliderX = slX;
                        sliderW = slW;
                        applySlider(mx);
                        return true;
                    }
                } else if (opt.isText) {
                    int txX = optX + 130;
                    int txY = oy + 2;
                    int txW = px + PANEL_W - txX - 20;
                    if (mx >= txX && mx <= txX + txW && my >= txY && my <= txY + 16) {
                        try {
                            String hex = (String) opt.get();
                            int initial = 0xFF000000 | Integer.parseInt(hex.replace("#", ""), 16);
                            MinecraftClient.getInstance().setScreen(new ColorPickerScreen(this, initial));
                        } catch (Exception e) {
                            MinecraftClient.getInstance().setScreen(new ColorPickerScreen(this, 0xFF00D4FF));
                        }
                        return true;
                    }
                } else if (opt.isDropdown) {
                    int ddX = optX + 130;
                    int ddY = oy + 2;
                    int ddW = px + PANEL_W - ddX - 20;
                    if (mx >= ddX && mx <= ddX + ddW && my >= ddY && my <= ddY + 16) {
                        openDropdown = opt;
                        dropdownScroll = 0;
                        return true;
                    }
                }
            }
        }

        boolean insidePanel = mx >= px && mx <= px + PANEL_W
                           && my >= py && my <= py + PANEL_H;
        boolean insideTabsBar = mx >= tbX - 8 && mx <= tbX + getTabsBarTotalWidth() + 8
                             && my >= tbY - 4 && my <= tbY + TABS_BAR_H + 4;
        if (!insidePanel && !insideTabsBar) {
            for (WidgetEntry w : widgetEntries) {
                int wx = w.getX();
                int wy = w.getY();
                int ww = w.getWidth();
                int wh = w.getHeight();
                if (mx >= wx && mx <= wx + ww && my >= wy && my <= wy + wh) {
                    draggingWidget = w;
                    dragOffX = (int) mx - wx;
                    dragOffY = (int) my - wy;
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

        // Если открыт dropdown — сначала проверяем его
        if (openDropdown != null) {
            Category current = categories.get(selectedCategory);
            int idx = current.options.indexOf(openDropdown);
            if (idx >= 0) {
                int optY = py + OPT_LIST_TOP;
                int oy = optY + idx * OPT_ROW_H - (int) scrollOffset;
                int ddX = px + 20 + 130;
                int ddY = oy + 2 + 16;
                int ddW = px + PANEL_W - ddX - 20;
                int listH = Math.min(DROPDOWN_MAX_VISIBLE, openDropdown.choices.length) * 16;

                if (mx >= ddX && mx <= ddX + ddW && my >= ddY && my <= ddY + listH) {
                    int maxScroll = Math.max(0, openDropdown.choices.length - DROPDOWN_MAX_VISIBLE);
                    dropdownScroll -= (int) delta;
                    dropdownScroll = Math.max(0, Math.min(maxScroll, dropdownScroll));
                    return true;
                }
            }
        }

        if (mx >= px && mx <= px + PANEL_W && my >= py + OPT_LIST_TOP && my <= py + PANEL_H) {
            scrollOffset -= delta * 20;
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
        if (draggingWidget != null) {
            int nx = (int) mx - dragOffX;
            int ny = (int) my - dragOffY;
            if (ConfigManager.INSTANCE.hudEditorSnap) {
                int grid = Math.max(2, ConfigManager.INSTANCE.hudEditorGrid);
                nx = Math.round((float) nx / grid) * grid;
                ny = Math.round((float) ny / grid) * grid;
            }
            draggingWidget.setPos(nx, ny);
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
        if (draggingWidget != null) { draggingWidget = null; ConfigManager.save(); return true; }
        if (draggingSlider != null) { draggingSlider = null; ConfigManager.save(); return true; }
        return super.mouseReleased(mx, my, button);
    }

    private void applySlider(double mx) {
        Option opt = draggingSlider;
        if (opt == null) return;
        float pct = (float)((mx - sliderX) / sliderW);
        pct = Math.max(0f, Math.min(1f, pct));

        if (opt.isFloat) {
            float newVal = opt.floatMin + pct * (opt.floatMax - opt.floatMin);
            if (opt.floatStep > 0) {
                newVal = Math.round(newVal / opt.floatStep) * opt.floatStep;
            }
            newVal = Math.max(opt.floatMin, Math.min(opt.floatMax, newVal));
            opt.set(newVal);
        } else {
            int newVal = opt.min + (int)(pct * (opt.max - opt.min));
            if (opt.step > 1) newVal = Math.round((float)newVal / opt.step) * opt.step;
            newVal = Math.max(opt.min, Math.min(opt.max, newVal));
            opt.set(newVal);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private static class Category {
        String name;
        List<Option> options;
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

        static Option bool(String name, String desc,
                           java.util.function.Supplier<Boolean> getter,
                           java.util.function.Consumer<Boolean> setter) {
            Option o = new Option();
            o.name = name; o.description = desc;
            o.getter = () -> getter.get();
            o.setter = v -> setter.accept((Boolean) v);
            o.isBool = true;
            return o;
        }

        static Option slider(String name, String desc,
                             java.util.function.Supplier<Integer> getter,
                             java.util.function.Consumer<Integer> setter,
                             int min, int max, int step) {
            Option o = new Option();
            o.name = name; o.description = desc;
            o.getter = () -> getter.get();
            o.setter = v -> setter.accept((Integer) v);
            o.isSlider = true;
            o.min = min; o.max = max; o.step = step;
            return o;
        }

        static Option sliderFloat(String name, String desc,
                                  java.util.function.Supplier<Float> getter,
                                  java.util.function.Consumer<Float> setter,
                                  float min, float max, float step) {
            Option o = new Option();
            o.name = name; o.description = desc;
            o.getter = () -> getter.get();
            o.setter = v -> setter.accept((Float) v);
            o.isSlider = true;
            o.isFloat = true;
            o.floatMin = min; o.floatMax = max; o.floatStep = step;
            return o;
        }

        static Option text(String name, String desc,
                           java.util.function.Supplier<String> getter,
                           java.util.function.Consumer<String> setter) {
            Option o = new Option();
            o.name = name; o.description = desc;
            o.getter = () -> getter.get();
            o.setter = v -> setter.accept((String) v);
            o.isText = true;
            return o;
        }

        static Option dropdown(String name, String desc,
                               java.util.function.Supplier<Integer> getter,
                               java.util.function.Consumer<Integer> setter,
                               String[] choices) {
            Option o = new Option();
            o.name = name; o.description = desc;
            o.getter = () -> getter.get();
            o.setter = v -> setter.accept((Integer) v);
            o.isDropdown = true;
            o.choices = choices;
            return o;
        }

        Object get() { return getter.get(); }
        void set(Object v) { setter.accept(v); }
          }

    // ============================================================
    // HIT / KILL SOUND PRESETS
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