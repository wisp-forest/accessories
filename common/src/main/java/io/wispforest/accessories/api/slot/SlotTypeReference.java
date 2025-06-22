package io.wispforest.accessories.api.slot;

import io.wispforest.accessories.data.SlotTypeLoader;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Helper Class used to hold a reference for a SlotType to use later if needed
 * with the required context of a level to grab correct side information
 */
public interface SlotTypeReference extends Supplier<String> {

    @Override
    default String get() {
        return slotName();
    }

    String slotName();

    @Nullable
    default SlotType get(boolean isClientSide) {
        if(this.slotName() == null) return null;

        return SlotTypeLoader.INSTANCE.getSlotType(isClientSide, this.slotName());
    }

    @Nullable
    default SlotType get(Level level) {
        return this.get(level.isClientSide());
    }
}
