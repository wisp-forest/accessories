package dev.emi.trinkets;

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public interface TrinketSlot {
    static boolean canInsert(ItemStack stack, SlotReference slotRef, LivingEntity entity) {
        boolean res = TrinketsApi.evaluatePredicateSet(slotRef.inventory().getSlotType().getValidatorPredicates(), stack, slotRef, entity);

        if (res) return TrinketsApi.getTrinket(stack.getItem()).canEquip(stack, slotRef, entity);

        return false;
    }
}
