package io.wispforest.accessories.pond;

import io.wispforest.accessories.api.AccessoriesStorage;
import io.wispforest.accessories.api.AccessoriesStorageLookup;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface AccessoriesRenderStateAPI {
    default Optional<LivingEntity> getEntityForState() {
        throw new IllegalStateException("Injected Interface method not implemented!");
    }

    @Nullable
    default AccessoriesStorageLookup getStorageLookup() {
        throw new IllegalStateException("Injected Interface method not implemented!");
    }

    default UUID getEntityUUIDForState() {
        throw new IllegalStateException("Injected Interface method not implemented!");
    }
}
