package io.wispforest.accessories.mixin;

import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ResourceArgument.class)
public interface ResourceArgumentAccessor<T> {
    @Accessor("registryKey")
    ResourceKey<? extends Registry<T>> accessories$registryKey();
}
