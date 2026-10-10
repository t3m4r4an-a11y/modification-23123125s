package net.macos.client.gui.menu;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Represents an individual configurable setting inside a module or category.
 */
public class MenuOption {

    public final String name;
    public final String description;
    public final Supplier<Object> getter;
    public final Consumer<Object> setter;

    public boolean isBool;
    public boolean isSlider;
    public boolean isFloat;
    public boolean isText;
    public boolean isDropdown;

    public int min, max, step;
    public float floatMin, floatMax, floatStep;
    public String[] choices;

    private MenuOption(String name, String description, Supplier<Object> getter, Consumer<Object> setter) {
        this.name = name;
        this.description = description;
        this.getter = getter;
        this.setter = setter;
    }

    public static MenuOption bool(String name, String description,
                                  Supplier<Boolean> getter, Consumer<Boolean> setter) {
        MenuOption o = new MenuOption(name, description, getter::get, v -> setter.accept((Boolean) v));
        o.isBool = true;
        return o;
    }

    public static MenuOption slider(String name, String description,
                                    Supplier<Integer> getter, Consumer<Integer> setter,
                                    int min, int max, int step) {
        MenuOption o = new MenuOption(name, description, getter::get, v -> setter.accept((Integer) v));
        o.isSlider = true;
        o.min = min;
        o.max = max;
        o.step = step;
        return o;
    }

    public static MenuOption sliderFloat(String name, String description,
                                         Supplier<Float> getter, Consumer<Float> setter,
                                         float min, float max, float step) {
        MenuOption o = new MenuOption(name, description, getter::get, v -> setter.accept((Float) v));
        o.isSlider = true;
        o.isFloat = true;
        o.floatMin = min;
        o.floatMax = max;
        o.floatStep = step;
        return o;
    }

    public static MenuOption color(String name, String description,
                                   Supplier<String> getter, Consumer<String> setter) {
        MenuOption o = new MenuOption(name, description, getter::get, v -> setter.accept((String) v));
        o.isText = true;
        return o;
    }

    public static MenuOption dropdown(String name, String description,
                                      Supplier<Integer> getter, Consumer<Integer> setter,
                                      String[] choices) {
        MenuOption o = new MenuOption(name, description, getter::get, v -> setter.accept((Integer) v));
        o.isDropdown = true;
        o.choices = choices;
        return o;
    }

    public Object get() {
        return getter.get();
    }

    public void set(Object val) {
        setter.accept(val);
    }
}
