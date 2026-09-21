package net.macos.client.hud;


import net.macos.client.hud.impl.ArmorHudWidget;
import net.macos.client.hud.impl.ComboCounterWidget;
import net.macos.client.hud.impl.KeystrokesWidget;
import net.macos.client.hud.impl.PotionHudWidget;
import net.macos.client.hud.impl.TargetHudWidget;
import net.macos.client.hud.impl.WatermarkWidget;

import java.util.List;
import java.util.function.Supplier;

public final class WidgetRegistry {

    private WidgetRegistry() {}

    public static final List<Supplier<HudWidget>> FACTORIES = List.of(
        WatermarkWidget::new,
        TargetHudWidget::new,
        ComboCounterWidget::new,
        KeystrokesWidget::new,
        ArmorHudWidget::new,
        PotionHudWidget::new
    );

    public static void registerAll() {
        for (Supplier<HudWidget> f : FACTORIES) {
            WidgetManager.INSTANCE.register(f.get());
        }
    }
}