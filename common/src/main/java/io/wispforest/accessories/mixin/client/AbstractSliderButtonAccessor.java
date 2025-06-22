package io.wispforest.accessories.mixin.client;

import net.minecraft.client.gui.components.AbstractSliderButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractSliderButton.class)
public interface AbstractSliderButtonAccessor {
    @Accessor("value")
    void accessories$setValue(double value);
}
