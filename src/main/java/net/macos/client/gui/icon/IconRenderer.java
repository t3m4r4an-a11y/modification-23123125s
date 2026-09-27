package net.macos.client.gui.icon;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

public final class IconRenderer {

    private IconRenderer() {}

    public static Text text(String icon) {
        return Text.literal(icon)
                .setStyle(Style.EMPTY.withFont(MacIcons.FONT));
    }

    public static int width(String icon) {
        return MinecraftClient.getInstance()
                .textRenderer
                .getWidth(text(icon));
    }

    public static void draw(
            DrawContext ctx,
            String icon,
            int x,
            int y,
            int color
    ) {
        ctx.drawTextWithShadow(
                MinecraftClient.getInstance().textRenderer,
                text(icon),
                x,
                y,
                color
        );
    }
}