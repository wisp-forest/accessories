package io.wispforest.tclayer.mixin;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

@Mixin(net.fabricmc.fabric.impl.resource.loader.ResourceManagerHelperImpl.class)
public interface ResourceManagerHelperImplAccessor {
    @Accessor Set<IdentifiableResourceReloadListener> getAddedListeners();
}
