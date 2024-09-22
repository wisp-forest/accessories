package io.wispforest.accessories.mixin.client.owo;

import io.wispforest.owo.ui.component.DiscreteSliderComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = DiscreteSliderComponent.class)
public interface DiscreteSliderComponentAccessor {

    @Accessor(value = "min", remap = false) void accessories$setMin(double value);
    @Accessor(value = "max", remap = false) void accessories$setMax(double value);

    @Invoker(value = "updateMessage") void accessories$updateMessage();
}
