package io.wispforest.accessories.api;

import com.google.common.collect.Multimap;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * Implementation of {@link Accessory} declaring such as a Accessory that holds nested {@link Accessory} in some manor
 */
public interface AccessoryNest extends Accessory {

    /**
     * @Return Gets all inner {@link ItemStack}'s with the passed holderStack
     */
    List<ItemStack> getInnerStacks(ItemStack holderStack);

    /**
     * Method used to modify the inner stacks of given AccessoryNest Stack
     *
     * @param holderStack The given HolderStack
     * @param index The target index
     * @param newStack The new stack replacing the given index
     */
    void modifyInnerStack(ItemStack holderStack, int index, ItemStack newStack);

    default List<Pair<DropRule, ItemStack>> getDropRules(ItemStack stack, SlotReference reference, DamageSource source) {
        var innerRules = new ArrayList<Pair<DropRule, ItemStack>>();

        var innerStacks = getInnerStacks(stack);

        for (int i = 0; i < innerStacks.size(); i++) {
            var innerStack = innerStacks.get(i);

            var innerAccessory = AccessoriesAPI.getOrDefaultAccessory(innerStack);

            var rule = innerAccessory.getDropRule(innerStack, reference, source);

            innerRules.add(Pair.of(rule, innerStack));
        }

        return innerRules;
    }

    /**
     * Attempt to process a given stack as if it may be a {@AccessoryNest}
     * @param stack
     * @return A Map of all stacks and there Accessory if found or else the default variant
     */
    static Map<ItemStack, Accessory> tryAndGet(ItemStack stack) {
        if(!(AccessoriesAPI.getAccessory(stack).orElse(null) instanceof AccessoryNest holdable)) return Map.of();

        var map = new HashMap<ItemStack, Accessory>();

        for (ItemStack innerStack : holdable.getInnerStacks(stack)) {
            map.put(innerStack, AccessoriesAPI.getOrDefaultAccessory(stack));
        }

        return map;
    }

    @Override
    default void tick(ItemStack stack, SlotReference reference) {
        var innerStacks = getInnerStacks(stack);

        for (ItemStack innerStack : innerStacks) {
            var innerAccessory = AccessoriesAPI.getOrDefaultAccessory(innerStack);

            innerAccessory.tick(innerStack, reference);
        }
    }

    @Override
    default void onEquip(ItemStack stack, SlotReference reference) {
        var innerStacks = getInnerStacks(stack);

        for (ItemStack innerStack : innerStacks) {
            var innerAccessory = AccessoriesAPI.getOrDefaultAccessory(innerStack);

            innerAccessory.onEquip(innerStack, reference);
        }
    }

    @Override
    default void onUnequip(ItemStack stack, SlotReference reference) {
        var innerStacks = getInnerStacks(stack);

        for (ItemStack innerStack : innerStacks) {
            var innerAccessory = AccessoriesAPI.getOrDefaultAccessory(innerStack);

            innerAccessory.canUnequip(innerStack, reference);
        }
    }

    @Override
    default boolean canEquip(ItemStack stack, SlotReference reference) {
        boolean canEquip = true;

        var innerStacks = getInnerStacks(stack);

        for (ItemStack innerStack : innerStacks) {
            var innerAccessory = AccessoriesAPI.getOrDefaultAccessory(innerStack);

            canEquip = canEquip && innerAccessory.canEquip(innerStack, reference);
        }

        return canEquip;
    }

    @Override
    default boolean canUnequip(ItemStack stack, SlotReference reference) {
        boolean canUnequip = true;

        var innerStacks = getInnerStacks(stack);

        for (ItemStack innerStack : innerStacks) {
            var innerAccessory = AccessoriesAPI.getOrDefaultAccessory(innerStack);

            canUnequip = canUnequip && innerAccessory.canEquip(innerStack, reference);
        }

        return canUnequip;
    }

    @Override
    default Multimap<Attribute, AttributeModifier> getModifiers(ItemStack stack, SlotReference reference, UUID uuid) {
        var map = Accessory.super.getModifiers(stack, reference, uuid);

        var innerStacks = getInnerStacks(stack);

        for (ItemStack innerStack : innerStacks) {
            var innerAccessory = AccessoriesAPI.getOrDefaultAccessory(innerStack);

            map.putAll(innerAccessory.getModifiers(innerStack, reference, uuid));
        }

        return map;
    }

    @Override
    default void getAttributesTooltip(ItemStack stack, SlotType type, List<Component> tooltips) {
        var innerStacks = getInnerStacks(stack);

        for (ItemStack innerStack : innerStacks) {
            var innerAccessory = AccessoriesAPI.getOrDefaultAccessory(innerStack);

            innerAccessory.getAttributesTooltip(innerStack, type, tooltips);
        }
    }

    @Override
    default void getExtraTooltip(ItemStack stack, List<Component> tooltips) {
        var innerStacks = getInnerStacks(stack);

        for (ItemStack innerStack : innerStacks) {
            var innerAccessory = AccessoriesAPI.getOrDefaultAccessory(innerStack);

            innerAccessory.getExtraTooltip(innerStack, tooltips);
        }
    }
}
