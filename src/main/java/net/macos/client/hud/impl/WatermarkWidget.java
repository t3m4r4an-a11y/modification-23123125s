package net.macos.client.hud.impl;

import net.macos.client.config.ConfigManager;
import net.macos.client.hud.glass.GlassWidget;
import net.macos.client.hud.glass.PanelStyle;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.macos.client.MacClient;
import net.minecraft.text.Text;
import net.minecraft.text.Style;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class WatermarkWidget extends GlassWidget {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm");

    // ============================================================
    // ICON FONT
    // ============================================================

    private static final Identifier ICON_FONT =
            new Identifier("macclient", "mac_icons");

    // ============================================================
    // ICONS
    // ============================================================

    // Logo / GUI (SF Symbols)
    private static final String ICON_LOGO = icon(0x10035F);

    // Performance
    private static final String ICON_FPS = icon(0x10037E);
    private static final String ICON_WIFI = icon(0x100647);

    // Time
    private static final String ICON_CLOCK = icon(0x10042C);

    // Weather
    private static final String ICON_SUN = icon(0x1001AE);
    private static final String ICON_MOON = icon(0x1001BA);
    private static final String ICON_RAIN = icon(0x1001C7);
    private static final String ICON_HEAVY_RAIN = icon(0x1001C9);
    private static final String ICON_THUNDER = icon(0x1001D3);
    private static final String ICON_SNOW = icon(0x1001E5);

    // ============================================================
    // STATE
    // ============================================================

    private long lastUpdate = 0;
    private String cachedTime = "00:00";

    public WatermarkWidget() {
        super("watermark");

        this.appearSpeed = 3f;
    }

    // ============================================================
    // PANEL STYLE
    // ============================================================

    @Override
    protected void configureStyle(PanelStyle s) {
        s.radius = 10;
        s.bgColor = 0x60101820;
        s.borderColor = 0x28FFFFFF;
        s.topAccent = false;
        s.specular = true;
        s.gradientTop = 0x18FFFFFF;
        s.gradientBot = 0x10000000;
        s.glowLayers = 0;
    }

    // ============================================================
    // MEASURE
    // ============================================================

    @Override
    protected void measure(float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.player == null) {
            this.w = 0;
            this.h = 0;
            return;
        }

        updateTime();

        int fps = mc.getCurrentFps();
        int ping = getPing(mc);

        String nick = mc.getSession().getUsername();

        String fpsText = fps + " FPS";
        String pingText = ping + " ms";

        String weatherIcon = getWeatherIcon(mc);

        // --------------------------------------------------------
        // Базовые размеры
        // --------------------------------------------------------

        int totalWidth = 8;

        // Акцентная полоса
        totalWidth += 3;

        // Logo
        totalWidth += 18;
        totalWidth += 6;

        // Head
        totalWidth += 18;
        totalWidth += 4;

        // Nick
        totalWidth += mc.textRenderer.getWidth(nick);

        // --------------------------------------------------------
        // FPS
        // --------------------------------------------------------

        totalWidth += 8 + 2 + 8;
        totalWidth += iconWidth(mc, ICON_FPS);
        totalWidth += 4;
        totalWidth += mc.textRenderer.getWidth(fpsText);

        // --------------------------------------------------------
        // Ping
        // --------------------------------------------------------

        totalWidth += 8 + 2 + 8;
        totalWidth += iconWidth(mc, ICON_WIFI);
        totalWidth += 4;
        totalWidth += mc.textRenderer.getWidth(pingText);

        // --------------------------------------------------------
        // Time
        // --------------------------------------------------------

        totalWidth += 8 + 2 + 8;
        totalWidth += iconWidth(mc, ICON_CLOCK);
        totalWidth += 4;
        totalWidth += mc.textRenderer.getWidth(cachedTime);

        // --------------------------------------------------------
        // Weather
        // --------------------------------------------------------

        totalWidth += 8 + 2 + 8;
        totalWidth += iconWidth(mc, weatherIcon);

        // Right padding
        totalWidth += 8;

        this.w = totalWidth;
        this.h = 22;
    }

    // ============================================================
    // RENDER
    // ============================================================

    @Override
    protected void renderInner(
            DrawContext ctx,
            int mouseX,
            int mouseY,
            float delta
    ) {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.player == null) {
            return;

        }
        debugIconFont(mc);
        int alpha = (int) (appearProgress * 255);

        int textColor =
                (alpha << 24) | 0xFFFFFF;

        int sepColor =
                (alpha << 24) | 0x60FFFFFF;

        int accentRGB = parseAccentColor();

        // ========================================================
        // ACCENT BAR
        // ========================================================

        ctx.fill(
                x + 4,
                y + 4,
                x + 6,
                y + h - 4,
                (alpha << 24) | accentRGB
        );

        int curX = x + 10;

        // ========================================================
        // LOGO
        // ========================================================

        drawIcon(
                ctx,
                ICON_LOGO,
                curX + 1,
                y + 5,
                (alpha << 24) | accentRGB
        );

        curX += 18 + 6;

        // ========================================================
        // PLAYER HEAD
        // ========================================================

        try {
            Identifier skin = mc.player.getSkinTexture();

            ctx.getMatrices().push();

            ctx.getMatrices().translate(
                    curX,
                    y + 4,
                    0
            );

            float scale = 14f / 8f;

            ctx.getMatrices().scale(
                    scale,
                    scale,
                    1f
            );

            ctx.setShaderColor(
                    1f,
                    1f,
                    1f,
                    alpha / 255f
            );

            ctx.drawTexture(
                    skin,
                    0,
                    0,
                    8,
                    8,
                    8,
                    8,
                    64,
                    64
            );

            ctx.setShaderColor(
                    1f,
                    1f,
                    1f,
                    1f
            );

            ctx.getMatrices().pop();

        } catch (Exception ignored) {
        }

        curX += 18 + 4;

        // ========================================================
        // TEXT Y
        // ========================================================

        int textY =
                y + (h - 8) / 2;

        // ========================================================
        // NICK
        // ========================================================

        String nick =
                mc.getSession().getUsername();

        ctx.drawTextWithShadow(
                mc.textRenderer,
                nick,
                curX,
                textY,
                textColor
        );

        curX +=
                mc.textRenderer.getWidth(nick);

        // ========================================================
        // SEPARATOR
        // ========================================================

        curX =
                drawSeparator(
                        ctx,
                        curX,
                        y,
                        sepColor
                );

        // ========================================================
        // FPS
        // ========================================================

        int fps =
                mc.getCurrentFps();

        int fpsColor =
                fpsColorFor(
                        fps,
                        alpha
                );

        drawIcon(
                ctx,
                ICON_FPS,
                curX,
                y + 5,
                fpsColor
        );

        curX +=
                iconWidth(mc, ICON_FPS) + 4;

        String fpsText =
                fps + " FPS";

        ctx.drawTextWithShadow(
                mc.textRenderer,
                fpsText,
                curX,
                textY,
                fpsColor
        );

        curX +=
                mc.textRenderer.getWidth(fpsText);

        // ========================================================
        // SEPARATOR
        // ========================================================

        curX =
                drawSeparator(
                        ctx,
                        curX,
                        y,
                        sepColor
                );

        // ========================================================
        // PING
        // ========================================================

        int ping =
                getPing(mc);

        int pingColor =
                pingColorFor(
                        ping,
                        alpha
                );

        drawIcon(
                ctx,
                ICON_WIFI,
                curX,
                y + 5,
                pingColor
        );

        curX +=
                iconWidth(mc, ICON_WIFI) + 4;

        String pingText =
                ping + " ms";

        ctx.drawTextWithShadow(
                mc.textRenderer,
                pingText,
                curX,
                textY,
                pingColor
        );

        curX +=
                mc.textRenderer.getWidth(pingText);

        // ========================================================
        // SEPARATOR
        // ========================================================

        curX =
                drawSeparator(
                        ctx,
                        curX,
                        y,
                        sepColor
                );

        // ========================================================
        // TIME
        // ========================================================

        drawIcon(
                ctx,
                ICON_CLOCK,
                curX,
                y + 5,
                textColor
        );

        curX +=
                iconWidth(mc, ICON_CLOCK) + 4;

        ctx.drawTextWithShadow(
                mc.textRenderer,
                cachedTime,
                curX,
                textY,
                textColor
        );

        curX +=
                mc.textRenderer.getWidth(cachedTime);

        // ========================================================
        // SEPARATOR
        // ========================================================

        curX =
                drawSeparator(
                        ctx,
                        curX,
                        y,
                        sepColor
                );

        // ========================================================
        // WEATHER
        // ========================================================

        String weatherIcon =
                getWeatherIcon(mc);

        int weatherColor =
                getWeatherColor(
                        mc,
                        alpha
                );

        drawIcon(
                ctx,
                weatherIcon,
                curX,
                y + 5,
                weatherColor
        );
    }

    // ============================================================
    // ICON RENDERING
    // ============================================================

    private static void drawIcon(
            DrawContext ctx,
            String icon,
            int x,
            int y,
            int color
    ) {
        MinecraftClient mc =
                MinecraftClient.getInstance();

        Text text =
                Text.literal(icon)
                        .setStyle(
                                Style.EMPTY.withFont(
                                        ICON_FONT
                                )
                        );

        ctx.drawTextWithShadow(
                mc.textRenderer,
                text,
                x,
                y,
                color
        );
    }

    private static int iconWidth(
            MinecraftClient mc,
            String icon
    ) {
        Text text =
                Text.literal(icon)
                        .setStyle(
                                Style.EMPTY.withFont(
                                        ICON_FONT
                                )
                        );

        return mc.textRenderer.getWidth(text);
    }

    // ============================================================
    // WEATHER
    // ============================================================

    private static String getWeatherIcon(
            MinecraftClient mc
    ) {
        if (mc.world == null || mc.player == null) {
            return ICON_SUN;
        }

        // Гроза
        if (mc.world.isThundering()) {
            return ICON_THUNDER;
        }

        // Дождь
        if (mc.world.isRaining()) {
            return ICON_RAIN;
        }

        // День / ночь
        if (mc.world.isNight()) {
            return ICON_MOON;
        }

        return ICON_SUN;
    }

    private static int getWeatherColor(
            MinecraftClient mc,
            int alpha
    ) {
        if (mc.world == null) {
            return (alpha << 24) | 0xFFFFFF;
        }

        if (mc.world.isThundering()) {
            return (alpha << 24) | 0xB8C7FF;
        }

        if (mc.world.isRaining()) {
            return (alpha << 24) | 0x7EBBFF;
        }

        if (mc.world.isNight()) {
            return (alpha << 24) | 0xC7CFFF;
        }

        return (alpha << 24) | 0xFFE27A;
    }

    // ============================================================
    // PING
    // ============================================================

    private static int getPing(
            MinecraftClient mc
    ) {
        if (mc.getNetworkHandler() == null) {
            return 0;
        }

        var entry =
                mc.getNetworkHandler()
                        .getPlayerListEntry(
                                mc.player.getUuid()
                        );

        if (entry == null) {
            return 0;
        }

        return entry.getLatency();
    }

    // ============================================================
    // SEPARATOR
    // ============================================================

    private int drawSeparator(
            DrawContext ctx,
            int curX,
            int y,
            int color
    ) {
        int gap = 8;

        curX += gap;

        ctx.fill(
                curX,
                y + h / 2 - 1,
                curX + 2,
                y + h / 2 + 1,
                color
        );

        curX += 2 + gap;

        return curX;
    }
    // ============================================================
    // TIME
    // ============================================================

    private void updateTime() {
        long now =
                System.currentTimeMillis();

        if (now - lastUpdate > 1000) {
            cachedTime =
                    LocalTime.now()
                            .format(TIME_FORMAT);

            lastUpdate = now;
        }
    }

    // ============================================================
    // COLORS
    // ============================================================

    private static int parseAccentColor() {
        try {
            String hex =
                    ConfigManager.INSTANCE.accentColor
                            .replace("#", "");

            return Integer.parseInt(
                    hex,
                    16
            );

        } catch (Exception e) {
            return 0x00D4FF;
        }
    }

    private static int fpsColorFor(
            int fps,
            int alpha
    ) {
        int rgb;

        if (fps >= 100) {
            rgb = 0x44FF66;
        } else if (fps >= 60) {
            rgb = 0x88FF44;
        } else if (fps >= 30) {
            rgb = 0xFFDD33;
        } else {
            rgb = 0xFF3344;
        }

        return (alpha << 24) | rgb;
    }

    private static int pingColorFor(
            int ping,
            int alpha
    ) {
        int rgb;

        if (ping < 60) {
            rgb = 0x44FF66;
        } else if (ping < 120) {
            rgb = 0xFFDD33;
        } else {
            rgb = 0xFF3344;
        }

        return (alpha << 24) | rgb;
    }

    // ============================================================
    // CODEPOINT → STRING
    // ============================================================

    private static String icon(
            int codePoint
    ) {
        return new String(
                Character.toChars(codePoint)
        );
    }
    private static boolean fontDebugDone = false;

    private static void debugIconFont(MinecraftClient mc) {
        if (fontDebugDone) return;
        fontDebugDone = true;

        Identifier fontId = new Identifier("macclient", "mac_icons");

        int[] codes = {
                0x10035F, // gear
                0x10037E, // FPS
                0x100647, // Wi-Fi
                0x10042C, // clock
                0x1001AE  // sun
        };

        MacClient.LOGGER.info("========== MAC ICON FONT DEBUG ==========");
        MacClient.LOGGER.info("Font ID: {}", fontId);

        for (int cp : codes) {
            String icon = new String(Character.toChars(cp));

            Text text = Text.literal(icon)
                    .setStyle(Style.EMPTY.withFont(fontId));

            int width = mc.textRenderer.getWidth(text);

            MacClient.LOGGER.info(
                    "U+{} | chars={} | width={}",
                    Integer.toHexString(cp).toUpperCase(),
                    icon.length(),
                    width
            );
        }

        boolean resourceExists = mc.getResourceManager()
                .getResource(new Identifier("macclient", "font/mac_icons.json"))
                .isPresent();

        MacClient.LOGGER.info("Font JSON exists: {}", resourceExists);
        MacClient.LOGGER.info("=========================================");
    }
}