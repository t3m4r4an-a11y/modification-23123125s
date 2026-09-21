package net.macos.client;

import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.macos.client.render.BlurRenderer;
import net.macos.client.gui.MacClientMenu;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.macos.client.config.ConfigManager;
import net.macos.client.gui.MacClientMenuKey;
import net.macos.client.gui.toast.ToastManager;
import net.macos.client.gui.toast.ToastType;
import net.macos.client.hud.WidgetManager;
import net.macos.client.hud.WidgetRegistry;
import net.macos.client.module.hud.AttackCooldown;
import net.macos.client.module.hud.CustomCrosshair;
import net.macos.client.module.hud.HitIndicator;
import net.macos.client.module.hud.TargetIndicator;
import net.macos.client.particle.HitFX;
import net.macos.client.particle.KillEffect;
import net.macos.client.utils.AntiAFK;
import net.macos.client.utils.AutoSprint;
import net.macos.client.utils.FreeLook;
import net.macos.client.utils.Fullbright;
import net.macos.client.utils.KillTracker;
import net.macos.client.utils.SoundRegistry;
import net.macos.client.utils.Zoom;
import net.macos.client.waypoint.WaypointManager;
import net.macos.client.waypoint.WaypointRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class MacClient implements ClientModInitializer {
    public static final String MOD_ID = "macclient";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Старые виджеты — те, что ещё не мигрировали на HudWidget
    public static AttackCooldown attackCooldown;

    // Watermark и TargetHud теперь в WidgetManager.INSTANCE.get("watermark") / ("targetHud")

    public static boolean hudEditorOpen = false;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing MacClient with macOS Glassmorphism...");
        ConfigManager.INSTANCE.load();

        WaypointManager.load();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("macwp")
                .then(ClientCommandManager.literal("add")
                    .then(ClientCommandManager.argument("name", StringArgumentType.word())
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "name");
                            var p = ctx.getSource().getPlayer();
                            String dim = p.getWorld().getRegistryKey().getValue().toString();
                            WaypointManager.add(name, p.getX(), p.getY(), p.getZ(), dim, 0x00D4FF);
                            ToastManager.show("Waypoint", "Добавлен: " + name, ToastType.SUCCESS);
                            return 1;
                        })))
                .then(ClientCommandManager.literal("remove")
                    .then(ClientCommandManager.argument("name", StringArgumentType.word())
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "name");
                            boolean ok = WaypointManager.remove(name);
                            if (ok) ToastManager.show("Waypoint", "Удалён: " + name, ToastType.SUCCESS);
                            else ToastManager.show("Waypoint", "Не найден: " + name, ToastType.ERROR);
                            return ok ? 1 : 0;
                        })))
                .then(ClientCommandManager.literal("list")
                    .executes(ctx -> {
                        var list = WaypointManager.getAll();
                        if (list.isEmpty()) {
                            ctx.getSource().sendFeedback(Text.literal("§eНет waypoints"));
                            return 1;
                        }
                        var p = ctx.getSource().getPlayer();
                        for (var w : list) {
                            double dist = w.distanceTo(p.getX(), p.getY(), p.getZ());
                            String dim = w.dimension.substring(w.dimension.lastIndexOf(':') + 1);
                            ctx.getSource().sendFeedback(Text.literal(
                                "§b" + w.name +
                                " §7[" + dim + "] " +
                                "§f(" + (int)w.x + ", " + (int)w.y + ", " + (int)w.z + ") " +
                                "§a" + (int)dist + "m"
                            ));
                        }
                        return 1;
                    })));
        });

        SoundRegistry.init();
        MacClientMenuKey.init();
        Zoom.init();
        FreeLook.init();

        // Регистрация новых виджетов в WidgetManager
        WidgetRegistry.registerAll();

        // Старые виджеты (пока не мигрировали)
        attackCooldown = new AttackCooldown();


        WorldRenderEvents.LAST.register(context -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc == null) return;

            // 1. Обновляем проекции 3D-объектов
            MatrixStack matrices = context.matrixStack();
            Camera camera = context.camera();
            float tickDelta = context.tickDelta();
            if (matrices != null && camera != null) {
                WaypointRenderer.updateProjections(matrices, camera, tickDelta);
                TargetIndicator.updateProjections(matrices, camera, tickDelta);
                KillEffect.updateProjections(matrices, camera, tickDelta);
                HitFX.updateProjections(matrices, camera, tickDelta);
            }

            // 2. Блюр поверх мира (руки и HUD будут резкими — они рендерятся позже)
            if (!ConfigManager.INSTANCE.enableGlassBlur) return;

            if (mc.currentScreen != null) {
                BlurRenderer.applyFullscreen(ConfigManager.INSTANCE.blurRadius);
            } else {
                BlurRenderer.captureBlurredBackground();
            }
        });

        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            int mouseX = (int) mc.mouse.getX();
            int mouseY = (int) mc.mouse.getY();

            // === Новые виджеты (Watermark, TargetHud, ...) ===
            if (mc.currentScreen == null) {
                boolean mouseDown = GLFW.glfwGetMouseButton(
                    mc.getWindow().getHandle(),
                   GLFW.GLFW_MOUSE_BUTTON_LEFT
                ) == GLFW.GLFW_PRESS;

            WidgetManager.INSTANCE.renderAll(drawContext, mouseX, mouseY, tickDelta, mouseDown);
                }
        
            // === Старые виджеты (пока не мигрировали) ===
            if (ConfigManager.INSTANCE.enableAttackCooldown) {
                attackCooldown.render(drawContext, mouseX, mouseY, tickDelta);
            };

            // === Всё остальное ===
            ToastManager.render(drawContext, tickDelta);
            CustomCrosshair.render(drawContext, tickDelta);
            WaypointRenderer.render2D(drawContext);
            HitIndicator.render(drawContext, tickDelta);
            TargetIndicator.render(drawContext, tickDelta);
            HitFX.render(drawContext, tickDelta);
            KillEffect.render(drawContext, tickDelta);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (MacClientMenuKey.openKey.wasPressed()) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.currentScreen == null) {
                    mc.setScreen(new MacClientMenu());
                } else if (mc.currentScreen instanceof MacClientMenu) {
                    mc.currentScreen.close();
                }
            }
            KillTracker.tick();
            Fullbright.tick(client);
            FreeLook.tick(client);
            Zoom.tick(client);
            AutoSprint.tick(client);
            AntiAFK.tick(client);
        });
    }
}