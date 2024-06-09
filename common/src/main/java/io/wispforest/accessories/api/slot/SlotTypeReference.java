package io.wispforest.accessories.api.slot;

import io.wispforest.accessories.data.SlotTypeLoader;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Helper Class used to hold a reference for a SlotType to use later if needed
 * with the required context of a level to grab correct side information
 */
public record SlotTypeReference(String slotName) {

    @Nullable
    public SlotType get(boolean isClientSide) {
        if(this.slotName == null) return null;

        return SlotTypeLoader.INSTANCE.getSlotTypes(isClientSide).getOrDefault(this.slotName, null);
    }

    @Nullable
    public SlotType get(Level level) {
        return this.get(level.isClientSide());
    }
}
