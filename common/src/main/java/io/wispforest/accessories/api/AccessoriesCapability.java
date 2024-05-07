package io.wispforest.accessories.api;

import com.google.common.collect.Multimap;
import io.wispforest.accessories.api.slot.SlotEntryReference;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.pond.AccessoriesAPIAccess;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public interface AccessoriesCapability {

    /**
     * @return The Capability Bound to the given living entity if such is present
     */
    @Nullable
    static AccessoriesCapability get(@NotNull LivingEntity livingEntity){
        return ((AccessoriesAPIAccess) livingEntity).accessoriesCapability();
    }

    //--

    /**
     * @return The bound entity to the given AccessoriesCapability instance
     */
    LivingEntity entity();

    /**
     * @return The bound data holder to the given LivingEntity
     */
    AccessoriesHolder getHolder();

    //--

    /**
     * Method used to clear all containers bound to the given {@link LivingEntity}
     */
    void reset(boolean loadedFromTag);

    /**
     * @return A Map containing all the {@link AccessoriesContainer}s with their {@link SlotType#name()} as the key
     */
    Map<String, AccessoriesContainer> getContainers();

    /**
     * @return a given {@link AccessoriesContainer} if found on the given {@link LivingEntity} tied to the Capability or null if not
     */
    @Nullable
    default AccessoriesContainer getContainer(SlotType slotType){
        return getContainers().get(slotType.name());
    }

    void updateContainers();

    //--

    /**
     * Attempts to equip a given item stack within any valid accessory container without swapping
     *
     * @param stack The desired stack to equip
     * @return The stack replaced within the slot or null if unable to equip
     */
    @Nullable
    default Pair<SlotReference, List<ItemStack>> equipAccessory(ItemStack stack){
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
    Pair<SlotReference, List<ItemStack>> equipAccessory(ItemStack stack, boolean allowSwapping, TriFunction<Accessory, ItemStack, SlotReference, Boolean> additionalCheck);

    //--

    /**
     * @return If any {@link ItemStack} is equipped based on the given {@link Item} entry
     */
    default boolean isEquipped(Item item){
        return isEquipped(item, EquipmentChecking.ACCESSORIES_ONLY);
    }

    default boolean isEquipped(Item item, EquipmentChecking check){
        return isEquipped(stack -> stack.getItem() == item, check);
    }

    /**
     * @return If any {@link ItemStack} is equipped based on the passed predicate
     */
    default boolean isEquipped(Predicate<ItemStack> predicate) {
        return isEquipped(predicate, EquipmentChecking.ACCESSORIES_ONLY);
    }

    default boolean isEquipped(Predicate<ItemStack> predicate, EquipmentChecking check) {
        return getFirstEquipped(predicate, check) != null;
    }

    /**
     * @return The first {@link ItemStack} formatted within {@link SlotEntryReference} that matches the given {@link Item}
     */
    @Nullable
    default SlotEntryReference getFirstEquipped(Item item){
        return getFirstEquipped(item, EquipmentChecking.ACCESSORIES_ONLY);
    }

    @Nullable
    default SlotEntryReference getFirstEquipped(Item item, EquipmentChecking check){
        return getFirstEquipped(stack -> stack.getItem() == item, check);
    }

    /**
     * @return The first {@link ItemStack} formatted within {@link SlotEntryReference} that matches the given predicate
     */
    default SlotEntryReference getFirstEquipped(Predicate<ItemStack> predicate) {
        return getFirstEquipped(predicate, EquipmentChecking.ACCESSORIES_ONLY);
    }

    SlotEntryReference getFirstEquipped(Predicate<ItemStack> predicate, EquipmentChecking check);

    /**
     * @return A list of all {@link ItemStack}'s formatted within {@link SlotEntryReference} matching the given {@link Item}
     */
    default List<SlotEntryReference> getEquipped(Item item){
        return getEquipped(stack -> stack.getItem() == item);
    }

    /**
     * @return A list of all {@link SlotEntryReference}'s formatted within {@link SlotEntryReference} matching the passed predicate
     */
    List<SlotEntryReference> getEquipped(Predicate<ItemStack> predicate);

    /**
     * @return A list of all {@link ItemStack}'s formatted within {@link SlotEntryReference}
     */
    List<SlotEntryReference> getAllEquipped();

    //--

    /**
     * Add map containing slot attributes to the given capability based on the keys used referencing specific slots
     * with being lost on reload
     * @param modifiers Slot Attribute Modifiers
     */
    void addTransientSlotModifiers(Multimap<String, AttributeModifier> modifiers);

    /**
     * Add slot attribute attributes to the given capability based on the keys used referencing specific slots
     * with being persistent on a reload
     * @param modifiers Slot Attribute Modifiers
     */
    void addPersistentSlotModifiers(Multimap<String, AttributeModifier> modifiers);

    /**
     * Add slot attribute attributes to the given capability based on the keys
     * @param modifiers Slot Attribute Modifiers
     */
    void removeSlotModifiers(Multimap<String, AttributeModifier> modifiers);

    /**
     * Get all modifiers from the given containers bound to the capability
     */
    Multimap<String, AttributeModifier> getSlotModifiers();

    /**
     * Remove all modifiers from the given containers bound to the capability
     */
    void clearSlotModifiers();

    /**
     * Remove all cached modifiers from the given containers bound to the capability
     */
    void clearCachedSlotModifiers();
}
