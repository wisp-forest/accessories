package io.wispforest.accessories.api.slot;

import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.data.SlotTypeLoader;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public record SlotReference(String slotName, LivingEntity entity, int slot) {

    @Nullable
    public SlotType type(){
        return SlotTypeLoader.getSlotType(entity.level(), this.slotName);
    }

    public AccessoriesCapability capability() {
        return this.entity.accessoriesCapability();
    }

    public AccessoriesContainer slotContainer() {
        return this.capability().getContainers().get(slotName);
    }
}
