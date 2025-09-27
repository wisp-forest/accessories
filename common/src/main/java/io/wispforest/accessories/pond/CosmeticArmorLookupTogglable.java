package io.wispforest.accessories.pond;

import io.wispforest.accessories.menu.ArmorSlotTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;
import java.util.function.Supplier;

public interface CosmeticArmorLookupTogglable {

    // TODO: HOPE THAT WITHIN THE FUTURE THIS WRAPPER METHODS WILL NOT BE NEEDED
    static <T> T runWithLookupToggle(Entity entity, Supplier<T> runnable) {
        var bl = entity instanceof CosmeticArmorLookupTogglable;

        if (bl) ((CosmeticArmorLookupTogglable) entity).setLookupToggle(true);

        var t = runnable.get();

        if (bl) ((CosmeticArmorLookupTogglable) entity).setLookupToggle(false);

        return t;
    }

    static void runWithLookupToggle(Entity entity, Runnable runnable) {
        var bl = entity instanceof CosmeticArmorLookupTogglable;

        if (bl) ((CosmeticArmorLookupTogglable) entity).setLookupToggle(true);

        runnable.run();

        if (bl) ((CosmeticArmorLookupTogglable) entity).setLookupToggle(false);
    }

    default void setLookupToggle(boolean value) {
        throw new IllegalStateException("Interface injected method not implemented!");
    }

    default boolean getLookupToggle() {
        throw new IllegalStateException("Interface injected method not implemented!");
    }

    static void getAlternativeStack(LivingEntity livingEntity, EquipmentSlot equipmentSlot, Consumer<ItemStack> consumer) {
        if(!((CosmeticArmorLookupTogglable)livingEntity).getLookupToggle()) return;

        var cosmetic = ArmorSlotTypes.getAlternativeStack(livingEntity, equipmentSlot);

        if(cosmetic == null) return;

        consumer.accept(cosmetic);
    }
}
