package io.wispforest.accessories.api;

import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * An action callback to attempt to equip some stack to a given {@link AccessoriesContainer}
 * returned from {@link AccessoriesCapability#canEquipAccessory(ItemStack, boolean)} allowing
 * of the ability to equip later once knowing equipability is possible.
 */
public interface EquipAction {

    /**
     * Method used to either equip the given stack or swap the given stack at
     * the location within the action
     *
     * @param stack The given stack to be equipped
     * @return The possible swapped stack if allowing swapping
     */
    Optional<ItemStack> equipStack(ItemStack stack);
}
