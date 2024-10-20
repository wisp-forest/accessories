package io.wispforest.accessories.mixin.owo;

import io.wispforest.endec.impl.ReflectiveEndecBuilder;
import io.wispforest.owo.config.ConfigWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ConfigWrapper.class, remap = false)
public interface ConfigWrapperAccessor {
    @Accessor(value = "builder", remap = false) ReflectiveEndecBuilder accessories$builder();
}
