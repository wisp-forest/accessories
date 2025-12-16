package io.wispforest.accessories.data.api;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public interface LookupDataLoader<V> {
    default Map<Identifier, V> getEntries(Level level) {
        return getEntries(level.isClientSide());
    }

    Map<Identifier, V> getEntries(boolean isClientSide);

    @Nullable
    default V getEntry(Identifier id, Level level) {
        return getEntry(id, level.isClientSide());
    }

    @Nullable
    V getEntry(Identifier id, boolean isClientSide);

    default Identifier getId(V t, Level level) {
        return getId(t, level.isClientSide());
    }

    Identifier getId(V t, boolean isClientSide);
}
