package io.wispforest.accessories.api;

import com.google.common.collect.Multimap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

public interface AccessoriesCapability extends InstanceCodecable {

    LivingEntity getEntity();

    void clear();

    Map<String, AccessoriesContainer> getContainers();

    //--

    void addTransientSlotModifiers(Multimap<String, AttributeModifier> modifiers);

    void addPersistentSlotModifiers(Multimap<String, AttributeModifier> modifiers);

    void removeSlotModifiers(Multimap<String, AttributeModifier> modifiers);

    Multimap<String, AttributeModifier> getSlotModifiers();

    void clearSlotModifiers();

    void clearCachedSlotModifiers();

    //--

    /**
     * Attempts to equip a given item stack within any valid accessory container without swapping
     *
     * @param stack The desired stack to equip
     * @return The stack replaced within the slot or null if unable to equip
     */
    @Nullable
    default SlotEntryReference equipAccessory(ItemStack stack){
        return equipAccessory(stack, false, (accessory, stack1, reference) -> true);
    }

    /**
     * Attempts to equip a given item stack within any valid accessory container
     *
     * @param stack The desired stack to equip
     * @param allowSwapping Toggles check to allow for swapping stack if no empty spot is found
     * @param additionalCheck Additional Check function used to see if such can be equipped
     * @return The stack replaced within the slot or null if unable to equip
     */
    @Nullable
    SlotEntryReference equipAccessory(ItemStack stack, boolean allowSwapping, TriFunction<Accessory, ItemStack, SlotReference, Boolean> additionalCheck);

    //--

    /**
     * @return If any {@link ItemStack} is equipped based on the passed predicate
     */
    boolean isEquipped(Predicate<ItemStack> predicate);

    /**
     * @return If any {@link ItemStack} is equipped based on the given {@link Item} entry
     */
    default boolean isEquipped(Item item){
        return isEquipped(stack -> stack.getItem() == item);
    }

    /**
     * @return A list of all {@link SlotEntryReference}'s formatted within {@link SlotEntryReference} matching the passed predicate
     */
    List<SlotEntryReference> getEquipped(Predicate<ItemStack> predicate);

    /**
     * @return A list of all {@link ItemStack}'s formatted within {@link SlotEntryReference} matching the given {@link Item} entry
     */
    default List<SlotEntryReference> getEquipped(Item item){
        return getEquipped(stack -> stack.getItem() == item);
    }

    /**
     * @return A list of all {@link ItemStack}'s formatted within {@link SlotEntryReference}
     */
    List<SlotEntryReference> getAllEquipped();

}
