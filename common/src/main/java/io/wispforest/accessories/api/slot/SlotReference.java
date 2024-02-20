package io.wispforest.accessories.api.slot;

import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.data.SlotTypeLoader;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

public record SlotReference(String slotName, LivingEntity entity, int slot) {

    public Optional<SlotType> type(){
        return SlotTypeLoader.getSlotType(entity.level(), this.slotName);
    }
}
