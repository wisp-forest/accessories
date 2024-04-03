package io.wispforest.accessories.mixin;

import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesHolder;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.impl.AccessoriesCapabilityImpl;
import io.wispforest.accessories.pond.AccessoriesAPIAccess;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.Optional;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements AccessoriesAPIAccess {

    @Override
    @Nullable
    public AccessoriesCapability accessoriesCapability() {
        var slots = EntitySlotLoader.getEntitySlots((LivingEntity) (Object) this);

        if(slots.isEmpty()) return null;

        return new AccessoriesCapabilityImpl((LivingEntity) (Object) this);
    }

    @Override
    @Nullable
    public AccessoriesHolder accessoriesHolder() {
        var capability = accessoriesCapability();

        return capability != null ? capability.getHolder() : null;
    }
}
