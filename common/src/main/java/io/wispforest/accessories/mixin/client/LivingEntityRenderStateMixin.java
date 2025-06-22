package io.wispforest.accessories.mixin.client;

import io.wispforest.accessories.api.AccessoriesStorage;
import io.wispforest.accessories.api.AccessoriesStorageLookup;
import io.wispforest.accessories.pond.AccessoriesRenderStateAPI;
import io.wispforest.accessories.pond.AccessoriesRenderStateExtension;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Mixin(LivingEntityRenderState.class)
public abstract class LivingEntityRenderStateMixin implements AccessoriesRenderStateAPI, AccessoriesRenderStateExtension {

    @Unique
    @Nullable
    private LivingEntity livingEntity = null;

    @Unique
    private UUID entityUUID = UUID.randomUUID();

    @Unique
    @Nullable
    private AccessoriesStorageLookup storageLookup = null;

    @Override
    public Optional<LivingEntity> getEntityForState() {
        return Optional.ofNullable(this.livingEntity);
    }

    @Override
    public void accessories$setEntity(LivingEntity livingEntity) {
        this.livingEntity = livingEntity;

    }

    @Override
    public void accessories$storageLookup(Map<String, AccessoriesStorage> map) {
        this.storageLookup = map.isEmpty() ? null : () -> map;
    }

    @Override
    public void accessoreis$setEntityUUID(UUID uuid) {
        this.entityUUID = uuid;
    }

    @Override
    @Nullable
    public AccessoriesStorageLookup getStorageLookup() {
        return storageLookup;
    }

    @Override
    public UUID getEntityUUIDForState() {
        return this.entityUUID;
    }
}
