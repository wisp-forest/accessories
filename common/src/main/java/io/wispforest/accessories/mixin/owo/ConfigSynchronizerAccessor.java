package io.wispforest.accessories.mixin.owo;

import io.wispforest.owo.config.ConfigSynchronizer;
import io.wispforest.owo.config.ConfigWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.HashMap;
import java.util.Map;

@Mixin(ConfigSynchronizer.class)
public interface ConfigSynchronizerAccessor {
    @Accessor(value = "KNOWN_CONFIGS")
    static Map<String, ConfigWrapper<?>> KNOWN_CONFIGS() {
        throw new IllegalStateException("UHHHHHHHHHHHHHHHHHH");
    }
}
