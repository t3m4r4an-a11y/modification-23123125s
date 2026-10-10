package net.macos.client.gui.menu;

import net.macos.client.gui.anim.AnimationState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Represents a functional module card displayed in the client menu.
 */
public class MenuModule {

    public final String name;
    public final String description;
    public final String icon;
    public final Supplier<Boolean> enabledSupplier;
    public final Consumer<Boolean> enabledSetter;
    public final List<MenuOption> subOptions;
    public final PreviewType previewType;

    public final AnimationState hover = new AnimationState(0f, 16f);
    public final AnimationState toggle = new AnimationState(0f, 18f);

    public MenuModule(String name, String description, String icon,
                      Supplier<Boolean> enabledSupplier, Consumer<Boolean> enabledSetter,
                      List<MenuOption> subOptions, PreviewType previewType) {
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.enabledSupplier = enabledSupplier;
        this.enabledSetter = enabledSetter;
        this.subOptions = subOptions != null ? subOptions : Collections.emptyList();
        this.previewType = previewType != null ? previewType : PreviewType.NONE;
    }

    public static MenuModule ofToggle(String name, String description, String icon,
                                      Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return new MenuModule(name, description, icon, getter, setter, new ArrayList<>(), PreviewType.NONE);
    }

    public static MenuModule ofComplex(String name, String description, String icon,
                                       Supplier<Boolean> getter, Consumer<Boolean> setter,
                                       List<MenuOption> subOptions, PreviewType previewType) {
        return new MenuModule(name, description, icon, getter, setter, subOptions, previewType);
    }

    public static MenuModule ofSettings(String name, String description, String icon,
                                        List<MenuOption> subOptions) {
        return new MenuModule(name, description, icon, null, null, subOptions, PreviewType.NONE);
    }

    public boolean isToggleable() {
        return enabledSupplier != null && enabledSetter != null;
    }

    public boolean isEnabled() {
        return isToggleable() && Boolean.TRUE.equals(enabledSupplier.get());
    }

    public void toggle() {
        if (isToggleable()) {
            enabledSetter.accept(!isEnabled());
        }
    }

    public boolean hasSubSettings() {
        return !subOptions.isEmpty() || previewType != PreviewType.NONE;
    }
}
