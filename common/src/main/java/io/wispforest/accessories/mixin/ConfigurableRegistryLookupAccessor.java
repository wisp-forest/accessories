package io.wispforest.accessories.mixin;

import net.minecraft.core.RegistryAccess;
import net.minecraft.server.ReloadableServerResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ReloadableServerResources.ConfigurableRegistryLookup.class)
public interface ConfigurableRegistryLookupAccessor {
    @Accessor("registryAccess")
    RegistryAccess getRegistryAccess();
}
