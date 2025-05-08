package io.wispforest.accessories.data.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public interface LookupDataLoader<V> {
    default Map<ResourceLocation, V> getEntries(Level level) {
        return getEntries(level.isClientSide());
    }

    Map<ResourceLocation, V> getEntries(boolean isClientSide);

    @Nullable
    default V getEntry(ResourceLocation id, Level level) {
        return getEntry(id, level.isClientSide());
    }

    @Nullable
    V getEntry(ResourceLocation id, boolean isClientSide);

    default ResourceLocation getId(V t, Level level) {
        return getId(t, level.isClientSide());
    }

    ResourceLocation getId(V t, boolean isClientSide);
}
