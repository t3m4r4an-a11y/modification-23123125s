package net.macos.client.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

import java.util.HashMap;
import java.util.Map;

public class Animation {

    // Отдельный smoothF для каждой руки — не утекает
    private static final Map<Arm, Float> smoothMap = new HashMap<>();

    public static void applySwing(MatrixStack m, Arm arm, float swingProgress, String mode) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        Hand preferred = mc.player.preferredHand;
        boolean thisArmActive = (preferred == Hand.MAIN_HAND)
                == (arm == mc.player.getMainArm());

        float targetF;
        if (mc.player.handSwinging && thisArmActive) {
            float realSwing = mc.player.getHandSwingProgress(mc.getTickDelta());
            if (mc.interactionManager != null && mc.interactionManager.isBreakingBlock()) {
                realSwing = Math.min(realSwing * 3f, 1f);
            }
            targetF = MathHelper.sin(realSwing * (float) Math.PI);
        } else {
            targetF = 0f;
        }

        float smooth = smoothMap.getOrDefault(arm, 0f);
        smooth += (targetF - smooth) * 0.35f;
        if (Math.abs(smooth - targetF) < 0.005f) smooth = targetF;
        smoothMap.put(arm, smooth);

        float f = smooth;

        switch (mode) {
            case "DIAGONAL" -> diagonal(m, arm, f);
            case "HORIZONTAL" -> horizontal(m, arm, f);
            case "BACKHAND" -> backhand(m, arm, f);
            case "THRUST" -> thrust(m, arm, f);
            case "CHOP" -> chop(m, arm, f);
            case "JAB" -> jab(m, arm, f);
            default -> vanilla(m, arm, swingProgress);
        }
    }

    // ============================================================
    // DIAGONAL — зеркалим для левой руки
    // ============================================================
    private static void diagonal(MatrixStack m, Arm arm, float f) {
        int side = arm == Arm.RIGHT ? 1 : -1;
        m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90f));
        m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * (45f - 45f * f)));
        m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * 90f));
    }

    // ============================================================
    // HORIZONTAL — зеркалим для левой
    // ============================================================
    private static void horizontal(MatrixStack m, Arm arm, float f) {
        int side = arm == Arm.RIGHT ? 1 : -1;
        m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-7f * f));
        m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * (-46f + 56f * f)));
        m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * 90f));
    }

    // ============================================================
    // BACKHAND — замах назад
    // ============================================================
    private static void backhand(MatrixStack m, Arm arm, float f) {
        int side = arm == Arm.RIGHT ? 1 : -1;
        m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * -80f * f));
        m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-30f * f));
        m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * 40f * f));
    }

    // ============================================================
    // THRUST — укол вперёд
    // ============================================================
    private static void thrust(MatrixStack m, Arm arm, float f) {
        m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-75f * f));
        m.translate(0f, -0.05f * f, -0.4f * f);
    }

    // ============================================================
    // CHOP — рубящий сверху вниз
    // ============================================================
    private static void chop(MatrixStack m, Arm arm, float f) {
        int side = arm == Arm.RIGHT ? 1 : -1;
        m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(40f));
        m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-100f * f));
        m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * 15f * f));
        m.translate(0f, -0.1f * f, -0.08f * f);
    }

    // ============================================================
    // JAB — быстрый прямой удар
    // ============================================================
    private static void jab(MatrixStack m, Arm arm, float f) {
        m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-45f * f));
        m.translate(0f, -0.03f * f, -0.25f * f);
    }

    public static void vanilla(MatrixStack m, Arm arm, float p) {
        int i = arm == Arm.RIGHT ? 1 : -1;
        float f = MathHelper.sin(p * p * (float) Math.PI);
        m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * (45.0F + f * -20.0F)));
        float g = MathHelper.sin(MathHelper.sqrt(p) * (float) Math.PI);
        m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * g * -20.0F));
        m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * -80.0F));
        m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * -45.0F));
    }
}