package io.wispforest.accessories.mixin;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.network.ServerPlayerConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

@Mixin(ChunkMap.TrackedEntity.class)
public interface EntityTrackerAccessor {
    @Accessor("seenBy")
    Set<ServerPlayerConnection> accessories$getSeenBy();
}
