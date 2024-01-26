package io.wispforest.accessories.api;

import io.wispforest.accessories.AccessoriesAccess;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

public record SlotReference(String slotName, LivingEntity entity, int slot) {

    public Optional<SlotType> type(){
        return AccessoriesAPI.getSlotType(entity.level(), this.slotName);
    }
}
