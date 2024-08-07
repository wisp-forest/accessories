package io.wispforest.accessories.pond;

import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.menu.ArmorSlotTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

public interface CosmeticArmorLookupTogglable {
    default void setLookupToggle(boolean value) {
        throw new IllegalStateException("Interface injected method not implemented!");
    }

    default boolean getLookupToggle() {
        throw new IllegalStateException("Interface injected method not implemented!");
    }

    static void getAlternativeStack(LivingEntity livingEntity, EquipmentSlot equipmentSlot, Consumer<ItemStack> consumer) {
        if(!((CosmeticArmorLookupTogglable)livingEntity).getLookupToggle()) return;

        var cosmetic = ArmorSlotTypes.getAlternativeStack(livingEntity, equipmentSlot);

        if(cosmetic == null || cosmetic.isEmpty()) return;

        consumer.accept(cosmetic);
    }
}
