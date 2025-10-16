package io.wispforest.accessories.pond;

import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import org.jetbrains.annotations.Nullable;

public interface EnchantedItemInUseExtension {

    EnchantedItemInUse setSlotReference(SlotReference slotReference);

    @Nullable
    SlotReference getSlotReference();
}
