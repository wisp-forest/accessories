package io.wispforest.accessories.api;

import com.google.common.collect.Multimap;
import io.wispforest.accessories.api.caching.ItemStackBasedPredicate;
import io.wispforest.accessories.api.slot.SlotEntryReference;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.api.slot.SlotTypeReference;
import io.wispforest.accessories.impl.AccessoriesHolderImpl;
import io.wispforest.accessories.pond.AccessoriesAPIAccess;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public interface AccessoriesCapability {

    /**
     * @return The {@link AccessoriesCapability} Bound to the given living entity if present
     */
    @Nullable
    static AccessoriesCapability get(@NotNull LivingEntity livingEntity){
        return ((AccessoriesAPIAccess) livingEntity).accessoriesCapability();
    }

    static Optional<AccessoriesCapability> getOptionally(@NotNull LivingEntity livingEntity){
        return Optional.ofNullable(get(livingEntity));
    }

    //--

    /**
     * @return The entity bound to the given {@link AccessoriesCapability} instance
     */
    LivingEntity entity();

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

    @Nullable
    default AccessoriesContainer getContainer(SlotTypeReference reference){
        return getContainers().get(reference.slotName());
    }

    void updateContainers();

    //--

    /**
     * Attempts to equip a given stack within any available {@link AccessoriesContainer} returning a
     * reference to where it was equipped. The given passed stack <b>will</b> be adjusted passed on
     * the amount of room that can be found within the found container.
     *
     * @param stack The given stack attempting to be equipped
     */
    @Nullable
    default SlotReference attemptToEquipAccessory(ItemStack stack) {
        var result = attemptToEquipAccessory(stack, false);

        return result != null ? result.first() : null;
    }

    /**
     * Attempts to equip a given stack within any available {@link AccessoriesContainer} returning a
     * reference to where it was equipped and an {@link Optional} of the previous stack if swapped for
     * the passed stack. The given passed stack <b>will</b> be adjusted passed on the amount of room that
     * can be found within the found container.
     *
     * @param stack The given stack attempting to be equipped
     */
    @Nullable
    default Pair<SlotReference, Optional<ItemStack>> attemptToEquipAccessory(ItemStack stack, boolean allowSwapping) {
        var result = canEquipAccessory(stack, allowSwapping, (slotStack, slotReference) -> true);

        return result != null ? Pair.of(result.first(), result.second().equipStack(stack)) : null;
    }

    default Pair<SlotReference, EquipAction> canEquipAccessory(ItemStack stack, boolean allowSwapping) {
        return canEquipAccessory(stack, allowSwapping, (slotStack, slotReference) -> true);
    }

    /**
     * Attempts to equip a given stack within any available {@link AccessoriesContainer} returning a
     * reference to where it can be equipped and a function to attempt equipping of the item which
     * may return an {@link Optional} of the previous stack if allowing for swapping.
     * <p>
     * Info: The passed stack will not be mutated in any way! Such only occurs on call of the possible
     * returned function.
     *
     * @param stack The given stack attempting to be equipped
     */
    @Nullable
    Pair<SlotReference, EquipAction> canEquipAccessory(ItemStack stack, boolean allowSwapping, EquipCheck extraCheck);

    //--

    /**
     * @return If any {@link ItemStack} is equipped based on the given {@link Item} entry
     */
    default boolean isEquipped(Item item){
        return isEquipped(item, EquipmentChecking.ACCESSORIES_ONLY);
    }

    default boolean isEquipped(Item item, EquipmentChecking check){
        return isEquipped(ItemStackBasedPredicate.ofItem(item), check);
    }

    /**
     * @return If any {@link ItemStack} is equipped based on the passed predicate
     */
    default boolean isEquipped(Predicate<ItemStack> predicate) {
        return isEquipped(predicate, EquipmentChecking.ACCESSORIES_ONLY);
    }

    default boolean isEquipped(Predicate<ItemStack> predicate, EquipmentChecking check) {
        return isEquipped(ItemStackBasedPredicate.ofPredicate(predicate), check);
    }

    default boolean isEquipped(ItemStackBasedPredicate predicate, EquipmentChecking check) {
        var cache = AccessoriesHolderImpl.getHolder(this).getLookupCache();

        if (cache != null) return cache.firstEquipped(predicate, check) != null;

        return getFirstEquipped(predicate, check) != null;
    }

    default boolean isAnotherEquipped(ItemStack stack, SlotReference slotReference, Item item) {
        return isAnotherEquipped(stack, slotReference, ItemStackBasedPredicate.ofItem(item));
    }

    default boolean isAnotherEquipped(ItemStack stack, SlotReference slotReference, Predicate<ItemStack> predicate) {
        return isAnotherEquipped(stack, slotReference, ItemStackBasedPredicate.ofPredicate(predicate));
    }

    /**
     * @return If any {@link ItemStack} is equipped based on the passed predicate while deduplicating
     * using the current {@link SlotReference} and the given {@link ItemStack}
     */
    default boolean isAnotherEquipped(ItemStack stack, SlotReference slotReference, ItemStackBasedPredicate predicate) {
        var cache = AccessoriesHolderImpl.getHolder(this).getLookupCache();

        List<SlotEntryReference> equippedStacks = (cache != null) ? cache.getEquipped(predicate) : getEquipped(predicate);

        if (equippedStacks.size() > 2) {
            for (var otherEntryRef : equippedStacks) {
                if (!otherEntryRef.reference().equals(slotReference)) return true;
                if (!otherEntryRef.stack().equals(stack)) return true;
            }
        } else if(equippedStacks.size() == 1) {
            var otherEntryRef = equippedStacks.getFirst();

            if (!otherEntryRef.reference().equals(slotReference)) return true;

            return !otherEntryRef.stack().equals(stack);
        }

        return false;
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
        return getFirstEquipped(ItemStackBasedPredicate.ofItem(item), check);
    }

    @Nullable
    default SlotEntryReference getFirstEquipped(Predicate<ItemStack> predicate) {
        return getFirstEquipped(predicate, EquipmentChecking.ACCESSORIES_ONLY);
    }

    @Nullable
    default SlotEntryReference getFirstEquipped(Predicate<ItemStack> predicate, EquipmentChecking check) {
        return getFirstEquipped(ItemStackBasedPredicate.ofPredicate(predicate), check);
    }

    /**
     * @return The first {@link ItemStack} formatted within {@link SlotEntryReference} that matches the given predicate
     */
    @Nullable
    SlotEntryReference getFirstEquipped(ItemStackBasedPredicate predicate, EquipmentChecking check);

    /**
     * @return A list of all {@link ItemStack}'s formatted within {@link SlotEntryReference} matching the given {@link Item}
     */
    default List<SlotEntryReference> getEquipped(Item item){
        return getEquipped(ItemStackBasedPredicate.ofItem(item));
    }

    /**
     * @return A list of all {@link SlotEntryReference}'s formatted within {@link SlotEntryReference} matching the passed predicate
     */
    default List<SlotEntryReference> getEquipped(Predicate<ItemStack> predicate){
        return getEquipped(ItemStackBasedPredicate.ofPredicate(predicate));
    }

    default List<SlotEntryReference> getEquipped(ItemStackBasedPredicate predicate) {
        var cache = AccessoriesHolderImpl.getHolder(this).getLookupCache();

        if (cache != null) return cache.getEquipped(predicate);

        return getAllEquipped().stream().filter(reference -> predicate.test(reference.stack())).toList();
    }
    /**
     * @return A list of all {@link ItemStack}'s formatted within {@link SlotEntryReference}
     */
    default List<SlotEntryReference> getAllEquipped() {
        return getAllEquipped(true);
    }

    List<SlotEntryReference> getAllEquipped(boolean recursiveStackLookup);

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

    //--

    /**
     * Used to attempt to equip a given stack within any available {@link AccessoriesContainer} returning a
     * reference and list within a pair. The given list may contain the overflow that could not fit based
     * on the containers max stack size.
     * <p>
     * <b>WARNING: THE GIVEN STACK PASSED WILL NOT BE MUTATED AT ALL!</b>
     *
     * @param stack          The given stack attempting to be equipped
     */
    @Deprecated
    @Nullable
    default Pair<SlotReference, List<ItemStack>> equipAccessory(ItemStack stack){
        return equipAccessory(stack, false);
    }

    /**
     * Used to attempt to equip a given stack within any available {@link AccessoriesContainer} returning a
     * reference and list within a pair. The given list may contain the overflow that could not fit based
     * on the containers max stack size and the old stack found if swapping was allowed.
     * <p>
     * <b>WARNING: THE GIVEN STACK PASSED WILL NOT BE MUTATED AT ALL!</b>
     *
     * @param stack          The given stack attempting to be equipped
     * @param allowSwapping  If the given call can attempt to swap accessories
     */
    @Deprecated
    default Pair<SlotReference, List<ItemStack>> equipAccessory(ItemStack stack, boolean allowSwapping) {
        var stackCopy = stack.copy();

        var result = attemptToEquipAccessory(stackCopy, allowSwapping);

        if(result == null) return null;

        var returnStacks = new ArrayList<ItemStack>();

        if(!stackCopy.isEmpty()) returnStacks.add(stackCopy);

        result.second().ifPresent(returnStacks::add);

        return Pair.of(result.first(), returnStacks);
    }

    @Nullable
    @Deprecated
    default Pair<SlotReference, List<ItemStack>> equipAccessory(ItemStack stack, boolean allowSwapping, TriFunction<Accessory, ItemStack, SlotReference, Boolean> additionalCheck) {
        return equipAccessory(stack, allowSwapping);
    }

    /**
     * @deprecated Use {@link #isAnotherEquipped(ItemStack, SlotReference, Item)}
     */
    @Deprecated(forRemoval = true)
    default boolean isAnotherEquipped(SlotReference slotReference, Item item) {
        return isAnotherEquipped(slotReference.getStack() /* <- DO NOT DO THIS! */, slotReference, item);
    }

    /**
     * @deprecated Use {@link #isAnotherEquipped(ItemStack, SlotReference, Predicate)}
     */
    @Deprecated(forRemoval = true)
    default boolean isAnotherEquipped(SlotReference slotReference, Predicate<ItemStack> predicate) {
        return isAnotherEquipped(slotReference.getStack() /* <- DO NOT DO THIS! */, slotReference, predicate);
    }
}
