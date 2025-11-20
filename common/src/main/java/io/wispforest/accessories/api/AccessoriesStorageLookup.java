package io.wispforest.accessories.api;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.caching.ItemStackBasedPredicate;
import io.wispforest.accessories.api.equip.EquipmentChecking;
import io.wispforest.accessories.api.slot.*;
import io.wispforest.accessories.api.core.AccessoryNestUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public interface AccessoriesStorageLookup {

    /**
     * @return A Map containing all the {@link AccessoriesContainer}s with their {@link SlotType#name()} as the key
     */
    Map<String, ? extends AccessoriesStorage> getContainers();

    @Nullable
    default <T> T getFromContainer(SlotPath slotPath, BiFunction<AccessoriesStorage, Integer, T> function){
        var container = getContainers().get(slotPath.slotName());

        return (container != null) ? function.apply(container, slotPath.index()) : null;
    }

    @Nullable
    default AccessoriesStorage getContainer(SlotPath slotPath){
        return getContainers().get(slotPath.slotName());
    }

    /**
     * @return a given {@link AccessoriesContainer} if found on the given {@link LivingEntity} tied to the Capability or null if not
     */
    @Nullable
    default AccessoriesStorage getContainer(SlotType slotType){
        return getContainers().get(slotType.name());
    }

    @Nullable
    default AccessoriesStorage getContainer(SlotTypeReference reference){
        return getContainers().get(reference.slotName());
    }

    //--

    /**
     * @return If any {@link ItemStack} is equipped based on the given {@link Item} entry
     */
    default boolean isEquipped(Item item){
        return isEquipped(item, EquipmentChecking.ACCESSORIES_ONLY);
    }

    /**
     * @return If any {@link ItemStack} is equipped based on the given {@link Item} entry with
     * the given {@link EquipmentChecking} useful for detecting Cosmetic overrides for rendering.
     */
    default boolean isEquipped(Item item, EquipmentChecking check){
        return isEquipped(ItemStackBasedPredicate.ofItem(item), check);
    }

    /**
     * @return If any {@link ItemStack} is equipped based on the passed predicate
     */
    default boolean isEquipped(Predicate<ItemStack> predicate) {
        return isEquipped(predicate, EquipmentChecking.ACCESSORIES_ONLY);
    }

    /**
     * @return If any {@link ItemStack} is equipped based on the passed {@link Predicate} with
     * the given {@link EquipmentChecking} useful for detecting Cosmetic overrides for rendering.
     */
    default boolean isEquipped(Predicate<ItemStack> predicate, EquipmentChecking check) {
        return isEquipped(ItemStackBasedPredicate.ofPredicate(predicate), check);
    }

    default boolean isEquipped(ItemStackBasedPredicate predicate, EquipmentChecking check) {
        return getFirstEquipped(predicate, check) != null;
    }

    default boolean isAnotherEquipped(ItemStack stack, SlotPath slotPath, Item item) {
        return isAnotherEquipped(stack, slotPath, ItemStackBasedPredicate.ofItem(item));
    }

    default boolean isAnotherEquipped(ItemStack stack, SlotPath slotPath, Predicate<ItemStack> predicate) {
        return isAnotherEquipped(stack, slotPath, ItemStackBasedPredicate.ofPredicate(predicate));
    }

    /**
     * @return If any {@link ItemStack} is equipped based on the passed predicate while deduplicating
     * using the current {@link SlotReference} and the given {@link ItemStack}
     */
    default boolean isAnotherEquipped(ItemStack stack, SlotPath slotPath, ItemStackBasedPredicate predicate) {
        List<? extends SlotPathWithStack> equippedStacks = getEquipped(predicate);

        if (equippedStacks.size() > 2) {
            for (var otherEntryRef : equippedStacks) {
                if (!otherEntryRef.path().equals(slotPath)) return true;
                if (!otherEntryRef.stack().equals(stack)) return true;
            }
        } else if(equippedStacks.size() == 1) {
            var otherEntryRef = equippedStacks.getFirst();

            if (!otherEntryRef.path().equals(slotPath)) return true;

            return !otherEntryRef.stack().equals(stack);
        }

        return false;
    }

    /**
     * @return The first {@link ItemStack} formatted within {@link SlotPathWithStack} that matches the given {@link Item}.
     */
    @Nullable
    default SlotPathWithStack getFirstEquipped(Item item){
        return getFirstEquipped(item, EquipmentChecking.ACCESSORIES_ONLY);
    }

    /**
     * @return The first {@link ItemStack} formatted within {@link SlotPathWithStack} that matches the given {@link Item}
     * with the given {@link EquipmentChecking} useful for detecting Cosmetic overrides for rendering.
     */
    @Nullable
    default SlotPathWithStack getFirstEquipped(Item item, EquipmentChecking check){
        return getFirstEquipped(ItemStackBasedPredicate.ofItem(item), check);
    }

    /**
     * @return The first {@link ItemStack} formatted within {@link SlotPathWithStack} that matches the given {@link Predicate}.
     */
    @Nullable
    default SlotPathWithStack getFirstEquipped(Predicate<ItemStack> predicate) {
        return getFirstEquipped(predicate, EquipmentChecking.ACCESSORIES_ONLY);
    }

    /**
     * @return The first {@link ItemStack} formatted within {@link SlotPathWithStack} that matches the given {@link Predicate}
     * with the given {@link EquipmentChecking} useful for detecting Cosmetic overrides for rendering.
     */
    @Nullable
    default SlotPathWithStack getFirstEquipped(Predicate<ItemStack> predicate, EquipmentChecking check) {
        return getFirstEquipped(ItemStackBasedPredicate.ofPredicate(predicate), check);
    }

    /**
     * @return The first {@link ItemStack} formatted within {@link SlotPathWithStack} that matches the given predicate
     */
    @Nullable
    default SlotPathWithStack getFirstEquipped(ItemStackBasedPredicate predicate, EquipmentChecking check) {
        return AccessoriesStorageLookupUtils.getFirstEquipped(getContainers(), SlotPathWithStack::of, predicate, check);
    }

    /**
     * @return A list of all {@link ItemStack}'s formatted within {@link SlotPathWithStack} matching the given {@link Item}
     */
    default List<? extends SlotPathWithStack> getEquipped(Item item){
        return getEquipped(ItemStackBasedPredicate.ofItem(item));
    }

    /**
     * @return A list of all {@link SlotPathWithStack}'s formatted within {@link SlotPathWithStack} matching the passed predicate
     */
    default List<? extends SlotPathWithStack> getEquipped(Predicate<ItemStack> predicate){
        return getEquipped(ItemStackBasedPredicate.ofPredicate(predicate));
    }

    default List<? extends SlotPathWithStack> getEquipped(ItemStackBasedPredicate predicate) {
        return getAllEquipped().stream().filter(reference -> predicate.test(reference.stack())).toList();
    }

    /**
     * @return A list of all {@link ItemStack}'s formatted within {@link SlotPathWithStack}
     */
    default List<? extends SlotPathWithStack> getAllEquipped() {
        return AccessoriesStorageLookupUtils.getAllEquipped(getContainers(), SlotPathWithStack::of);
    }
}

class AccessoriesStorageLookupUtils {
    static <E extends SlotPathWithStack> E getFirstEquipped(Map<String, ? extends AccessoriesStorage> containers, BiFunction<SlotPath, ItemStack, E> stackEntryMaker, ItemStackBasedPredicate predicate, EquipmentChecking check) {
        for (var container : containers.values()) {
            int i = 0;

            for (var stack : container.getAccessories()) {
                var path = container.createPath(i);

                if(check == EquipmentChecking.COSMETICALLY_OVERRIDABLE) {
                    var cosmetic = container.getCosmeticAccessories().getItem(i);

                    if(!cosmetic.isEmpty() && Accessories.config().clientOptions.showCosmeticAccessories()) stack = cosmetic;
                }

                var ref = AccessoryNestUtils.recursivelyHandle(stack, path, (innerStack, ref1) -> {
                    return (!innerStack.isEmpty() && predicate.test(innerStack))
                            ? stackEntryMaker.apply(path, innerStack)
                            : null;
                });

                if (ref != null) return ref;

                i++;
            }
        }

        return null;
    }

    static <E extends SlotPathWithStack> List<E> getAllEquipped(Map<String, ? extends AccessoriesStorage> containers, BiFunction<SlotPath, ItemStack, E> stackEntryMaker) {
        var references = new ArrayList<E>();

        for (var container : containers.values()) {
            int i = 0;

            for (var stack : container.getAccessories()) {
                if (!stack.isEmpty()) {
                    var path = container.createPath(i);

                    AccessoryNestUtils.recursivelyConsume(stack, path, (innerStack, path1) -> references.add(stackEntryMaker.apply(path1, innerStack)));
                }

                i++;
            }
        }

        return Collections.unmodifiableList(references);
    }
}
