package net.macos.client.mixin;

import net.macos.client.utils.ISimpleOption;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SimpleOption.class)
public abstract class SimpleOptionMixin<T> implements ISimpleOption<T> {

    @Shadow
    @Mutable
    private T value;

    @Override
    public void forceSetValue(T newValue) {
        this.value = newValue;
    }
}