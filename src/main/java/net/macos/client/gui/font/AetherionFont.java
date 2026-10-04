package net.macos.client.gui.font;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.text.OrderedText;

public final class AetherionFont {

    private static final Identifier FONT_ID = new Identifier("macclient", "aetherion");
    private static final Style STYLE = Style.EMPTY.withFont(FONT_ID);

    private AetherionFont() {}

    public static Text of(String text) {
        return Text.literal(text).setStyle(STYLE);
    }

    public static Text of(Text text) {
        return text.copy().setStyle(STYLE);
    }

    public static void draw(DrawContext ctx, String text, int x, int y, int color) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        ctx.drawText(tr, of(text), x, y, color, true);
    }

    public static void draw(DrawContext ctx, Text text, int x, int y, int color) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        ctx.drawText(tr, of(text), x, y, color, true);
    }

    public static void draw(DrawContext ctx, OrderedText text, int x, int y, int color) {
        StringBuilder sb = new StringBuilder();
        text.accept((index, style, codePoint) -> {
            sb.appendCodePoint(codePoint);
            return true;
        });
        draw(ctx, sb.toString(), x, y, color);
    }

    public static void drawNoShadow(DrawContext ctx, String text, int x, int y, int color) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        ctx.drawText(tr, of(text), x, y, color, false);
    }

    public static int width(String text) {
        return MinecraftClient.getInstance().textRenderer.getWidth(of(text));
    }

    public static int width(Text text) {
        return MinecraftClient.getInstance().textRenderer.getWidth(of(text));
    }

    public static int width(OrderedText text) {
        StringBuilder sb = new StringBuilder();
        text.accept((index, style, codePoint) -> {
            sb.appendCodePoint(codePoint);
            return true;
        });
        return width(sb.toString());
    }
}