package io.wispforest.accessories.pond;

import io.wispforest.accessories.api.AccessoriesStorage;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.UUID;

public interface AccessoriesRenderStateExtension {
    void accessories$setEntity(LivingEntity livingEntity);

    void accessories$setPartialTicks(float value);

    void accessories$storageLookup(Map<String, AccessoriesStorage> map);

    void accessoreis$setEntityUUID(UUID uuid);
}
