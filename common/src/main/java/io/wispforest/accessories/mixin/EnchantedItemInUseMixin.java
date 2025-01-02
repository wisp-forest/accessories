package io.wispforest.accessories.mixin;

import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.pond.EnchantedItemInUseExtension;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(EnchantedItemInUse.class)
public abstract class EnchantedItemInUseMixin implements EnchantedItemInUseExtension {

    private final MutableObject<@Nullable SlotReference> slotReferenceHolder = new MutableObject<>(null);

    @Override
    public EnchantedItemInUse setSlotReference(SlotReference slotReference) {
        this.slotReferenceHolder.setValue(slotReference);

        return (EnchantedItemInUse)(Object) this;
    }

    @Override
    public @Nullable SlotReference getSlotReference() {
        return this.slotReferenceHolder.getValue();
    }
}
