package net.macos.client.chat;

import net.macos.client.config.ConfigManager;
import net.macos.client.gui.font.AetherionFont;
import net.macos.client.gui.icon.IconRenderer;
import net.macos.client.gui.icon.MacIcons;
import net.macos.client.hud.glass.GlassRenderer;
import net.macos.client.render.GlassBackdrop;
import net.macos.client.render.SquircleRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.screen.ChatScreen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 2026 Luxury Liquid Glass Chat Renderer.
 * Features GPU SDF Squircles, blur backdrop, rich header when open, message hover pills,
 * and smooth fade-out when closed.
 */
public class ChatRenderer {

    // Layout metrics
    private static final int PADDING_X    = 12;
    private static final int PADDING_Y    = 8;
    private static final int LINE_HEIGHT  = 13;
    private static final int RADIUS       = 9;
    private static final int LEFT_MARGIN  = 8;
    private static final int HEADER_H     = 22;

    private static final int CLOSED_MAX_LINES     = 10;
    private static final int OPEN_MAX_LINES       = 20;
    private static final int CLOSED_BOTTOM_MARGIN = 58;
    private static final int OPEN_BOTTOM_MARGIN   = 46;

    // Palette
    private static final int C_PANEL_BG     = 0xD00A0E18;
    private static final int C_BORDER       = 0x22FFFFFF;
    private static final int C_SPECULAR     = 0x1EFFFFFF;

    public static void renderFromHud(DrawContext ctx, List<ChatHudLine.Visible> messages,
                                     int scrolledLines, int lineHeight, int currentTick) {
        renderFromHud(ctx, messages, scrolledLines, lineHeight, currentTick, -1, -1);
    }

    public static void renderFromHud(DrawContext ctx, List<ChatHudLine.Visible> messages,
                                     int scrolledLines, int lineHeight, int currentTick,
                                     int mouseX, int mouseY) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;
        if (messages.isEmpty()) return;

        boolean chatOpen = mc.currentScreen instanceof ChatScreen;

        // Collect message pool (most recent first in vanilla order)
        List<ChatHudLine.Visible> pool = new ArrayList<>();
        for (ChatHudLine.Visible line : messages) {
            int age = currentTick - line.addedTime();
            if (!chatOpen && age > 200) continue;
            pool.add(line);
        }
        if (pool.isEmpty()) return;

        // Visible window slicing
        int maxLines = chatOpen ? OPEN_MAX_LINES : CLOSED_MAX_LINES;
        int start = chatOpen
                ? Math.max(0, Math.min(scrolledLines, pool.size() - 1))
                : 0;
        int end = Math.min(pool.size(), start + maxLines);

        List<ChatHudLine.Visible> visible = new ArrayList<>(pool.subList(start, end));
        Collections.reverse(visible); // Oldest at top, newest at bottom

        // Measure widest text line
        int longestText = 0;
        for (ChatHudLine.Visible line : visible) {
            int w = AetherionFont.width(line.content());
            if (w > longestText) longestText = w;
        }

        // Parse accent color
        int accent = 0x00D4FF;
        try {
            accent = Integer.parseInt(ConfigManager.INSTANCE.accentColor.replace("#", ""), 16);
        } catch (Exception ignored) {}

        int screenH = ctx.getScaledWindowHeight();
        int screenW = ctx.getScaledWindowWidth();
        int bottomMargin = chatOpen ? OPEN_BOTTOM_MARGIN : CLOSED_BOTTOM_MARGIN;

        int boxW = Math.max(chatOpen ? 220 : 140, longestText + PADDING_X * 2 + 10);
        int boxH = visible.size() * LINE_HEIGHT + PADDING_Y * 2 + (chatOpen ? HEADER_H : 0);

        int boxX = LEFT_MARGIN;
        int boxY = screenH - bottomMargin - boxH;

        if (chatOpen) {
            // === OPEN CHAT: FULL LUXURY GLASS PANEL ===
            GlassRenderer.dropShadow(ctx, boxX, boxY, boxW, boxH, 12, 10, 0x75);

            if (ConfigManager.INSTANCE.enableChatBlur && ConfigManager.INSTANCE.enableGlassBlur) {
                GlassBackdrop.draw(ctx, boxX + 2, boxY + 2, boxW - 4, boxH - 4);
            }

            // Dark obsidian acrylic fill
            SquircleRenderer.fill(ctx, boxX, boxY, boxW, boxH, RADIUS, C_PANEL_BG);
            SquircleRenderer.border(ctx, boxX, boxY, boxW, boxH, RADIUS, 1.0f, C_BORDER);
            GlassRenderer.specular(ctx, boxX, boxY, boxW, boxH, RADIUS, C_SPECULAR);

            // --- Header Bar ---
            int headerY = boxY + 4;
            // Title
            AetherionFont.draw(ctx, "Chat", boxX + 10, headerY + 2, (0xF0 << 24) | accent);

            // Active channel capsule "[ ALL ]"
            int chanW = 34;
            int chanH = 13;
            int chanX = boxX + 44;
            int chanY = headerY + 1;
            SquircleRenderer.pill(ctx, chanX, chanY, chanW, chanH, 0x22FFFFFF);
            SquircleRenderer.border(ctx, chanX, chanY, chanW, chanH, chanH / 2.0f, 1.0f, 0x30FFFFFF);
            AetherionFont.draw(ctx, "ALL", chanX + 8, chanY + 3, 0xD0FFFFFF);

            // Message count badge
            String countText = pool.size() + " msgs";
            int countW = AetherionFont.width(countText);
            int countX = boxX + boxW - countW - 14;
            AetherionFont.draw(ctx, countText, countX, headerY + 3, 0x80A0A5B5);

            // Divider line under header
            ctx.fill(boxX + 6, boxY + HEADER_H, boxX + boxW - 6, boxY + HEADER_H + 1, 0x18FFFFFF);

            // Scroll indicator if scrolled
            if (scrolledLines > 0) {
                String scrollHint = "▲ " + scrolledLines;
                int shW = AetherionFont.width(scrollHint);
                SquircleRenderer.pill(ctx, boxX + boxW - shW - 20, boxY + HEADER_H + 3, shW + 12, 11, 0x40000000);
                AetherionFont.draw(ctx, scrollHint, boxX + boxW - shW - 14, boxY + HEADER_H + 4, (0xD0 << 24) | accent);
            }
        } else {
            // === CLOSED CHAT: COMPACT FLOATING MESSAGE CAPSULE ===
            // Soft translucent glass backing behind visible lines
            GlassRenderer.dropShadow(ctx, boxX, boxY, boxW, boxH, 8, 6, 0x45);
            SquircleRenderer.fill(ctx, boxX, boxY, boxW, boxH, RADIUS, 0x90080B12);
            SquircleRenderer.border(ctx, boxX, boxY, boxW, boxH, RADIUS, 1.0f, 0x16FFFFFF);
            GlassRenderer.specular(ctx, boxX, boxY, boxW, boxH, RADIUS, 0x12FFFFFF);
        }

        // Base coordinates for message lines
        int textX = boxX + PADDING_X + 2;
        int textY = boxY + PADDING_Y + (chatOpen ? HEADER_H + 2 : 0);

        // --- PASS 1: Render All Background Overlays & Indicator Pips ---
        for (int i = 0; i < visible.size(); i++) {
            ChatHudLine.Visible line = visible.get(i);
            int age = Math.max(0, currentTick - line.addedTime());

            float alpha = 1.0f;
            if (!chatOpen && age > 150) {
                alpha = Math.max(0f, (200 - age) / 50f);
            }
            int a = (int) (alpha * 255);
            if (a < 5) continue;

            int lineY = textY + i * LINE_HEIGHT;
            boolean isNewest = (i == visible.size() - 1);

            // Message hover highlight pill when chat is open
            if (chatOpen && mouseX >= boxX + 4 && mouseX <= boxX + boxW - 4 &&
                    mouseY >= lineY - 1 && mouseY < lineY + LINE_HEIGHT - 1) {
                SquircleRenderer.fill(ctx, boxX + 6, lineY - 1, boxW - 12, LINE_HEIGHT, 4, 0x24FFFFFF);
            }

            // Left neon vertical pip for the latest incoming message
            if (isNewest) {
                int pipCol = ((int) (a * 0.9f) << 24) | accent;
                SquircleRenderer.pill(ctx, boxX + 4, lineY + 1, 2, LINE_HEIGHT - 4, pipCol);
            }
        }

        // Flush any SDF geometry before entering text batch
        ctx.draw();

        // --- PASS 2: Render All Message Texts in One Clean Batch ---
        for (int i = 0; i < visible.size(); i++) {
            ChatHudLine.Visible line = visible.get(i);
            int age = Math.max(0, currentTick - line.addedTime());

            float alpha = 1.0f;
            if (!chatOpen && age > 150) {
                alpha = Math.max(0f, (200 - age) / 50f);
            }

            boolean isNewest = (i == visible.size() - 1);
            if (isNewest && !chatOpen) {
                alpha = Math.min(1.0f, alpha * 1.15f);
            }

            int a = (int) (alpha * 255);
            if (a < 5) continue;

            int lineY = textY + i * LINE_HEIGHT;
            int baseColor = isNewest ? 0xFFFFFF : 0xE8ECF2;
            int color = (a << 24) | baseColor;

            AetherionFont.drawWithShadow(ctx, line.content(), textX, lineY, color);
        }

        ctx.draw();
    }
}