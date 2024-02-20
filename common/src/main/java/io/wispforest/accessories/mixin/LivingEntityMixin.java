package io.wispforest.accessories.mixin;

import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesHolder;
import io.wispforest.accessories.impl.AccessoriesCapabilityImpl;
import io.wispforest.accessories.pond.AccessoriesAPIAccess;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.Optional;

@Mixin(LivingEntity.class)
public class LivingEntityMixin implements AccessoriesAPIAccess {

    @Unique
    private AccessoriesCapability capability = null;

    @Override
    public Optional<AccessoriesCapability> accessoriesCapability() {
        var slots = EntitySlotLoader.getEntitySlots((LivingEntity) (Object) this);

        if(slots.isEmpty()) return Optional.empty();

        this.capability = new AccessoriesCapabilityImpl((LivingEntity) (Object) this);

        return Optional.of(this.capability);
    }

    @Override
    public Optional<AccessoriesHolder> accessoriesHolder() {
        return accessoriesCapability().map(AccessoriesCapability::getHolder);
    }
}
