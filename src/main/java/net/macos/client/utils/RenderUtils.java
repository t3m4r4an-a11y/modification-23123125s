package net.macos.client.utils;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import java.awt.Color;

public class RenderUtils {

    public static Color hexToColor(String hex) {
        return Color.decode(hex);
    }

    // Рисование скруглённого прямоугольника (macOS стиль)
    public static void drawRoundedRect(DrawContext context, int x, int y, int width, int height, int radius, Color color) {
        context.fill(x + radius, y, x + width - radius, y + height, color.getRGB());
        context.fill(x, y + radius, x + width, y + height - radius, color.getRGB());
        context.fill(x, y + radius, x + radius, y + height - radius, color.getRGB());
        context.fill(x + width - radius, y + radius, x + width, y + height - radius, color.getRGB());
    }

    // Рисование дугового прогресса (для брони)
    public static void drawArc(DrawContext context, int centerX, int centerY, int radius, float startAngle, float endAngle, Color color, int thickness) {
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(centerX, centerY, 0);

        VertexConsumer vertexConsumer = context.getVertexConsumers().getBuffer(RenderLayer.getLines());
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        int r = color.getRed(), g = color.getGreen(), b = color.getBlue(), a = color.getAlpha();
        float step = 5f;
        for (float angle = startAngle; angle < endAngle; angle += step) {
            float rad1 = (float) Math.toRadians(angle);
            float rad2 = (float) Math.toRadians(angle + step);

            float x1 = (float) Math.cos(rad1) * radius;
            float y1 = (float) Math.sin(rad1) * radius;
            float x2 = (float) Math.cos(rad2) * radius;
            float y2 = (float) Math.sin(rad2) * radius;

            vertexConsumer.vertex(matrix, x1, y1, 0).color(r, g, b, a).normal(0, 0, 1).next();
            vertexConsumer.vertex(matrix, x2, y2, 0).color(r, g, b, a).normal(0, 0, 1).next();
        }
        context.draw();
        matrices.pop();
    }
}