package io.wispforest.accessories.api;

import com.google.common.collect.Multimap;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.impl.AccessoryNestUtils;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Implementation of {@link Accessory} declaring such as an Accessory that holds nested {@link Accessory} in some manor
 */
public interface AccessoryNest extends Accessory {

    String ACCESSORY_NEST_ITEMS_KEY = "AccessoryNestItems";

    /**
     * @Return Gets all inner {@link ItemStack}'s with the passed holderStack
     */
    default List<ItemStack> getInnerStacks(ItemStack holderStack) {
        var tag = holderStack.getTag();

        if(tag == null) return List.of();

        var listTag = tag.getList(ACCESSORY_NEST_ITEMS_KEY, 10);

        var list = NonNullList.withSize(listTag.size(), ItemStack.EMPTY);

        for (Tag tag1 : listTag) {
            if(tag1 instanceof CompoundTag compoundTag) list.add(ItemStack.of(compoundTag));
        }

        return list;
    }

    /**
     * Method used to modify the inner stacks of given AccessoryNest Stack
     *
     * @param holderStack The given HolderStack
     * @param index The target index
     * @param newStack The new stack replacing the given index
     */
    default boolean setInnerStack(ItemStack holderStack, int index, ItemStack newStack) {
        if(AccessoryNest.isAccessoryNest(holderStack) && !this.allowDeepRecursion()) return false;

        var listTag = holderStack.getTag().getList(ACCESSORY_NEST_ITEMS_KEY, 10);

        ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, newStack).result()
                .ifPresent(tag1 -> listTag.set(index, tag1));

        return true;
    }

    /**
     * By default, accessory nests can only go one layer deep as it's hard to track the stack modifications any further
     *
     * @return Whether such implementation of the Accessory nest allows for further nesting of other Nests
     */
    default boolean allowDeepRecursion() {
        return false;
    }

    default List<Pair<DropRule, ItemStack>> getDropRules(ItemStack stack, SlotReference reference, DamageSource source) {
        var innerRules = new ArrayList<Pair<DropRule, ItemStack>>();

        var innerStacks = getInnerStacks(stack);

        for (int i = 0; i < innerStacks.size(); i++) {
            var innerStack = innerStacks.get(i);

            var rule = AccessoriesAPI.getOrDefaultAccessory(innerStack).getDropRule(innerStack, reference, source);

            innerRules.add(Pair.of(rule, innerStack));
        }

        return innerRules;
    }

    //--

    /**
     * Method used to perform some action on a possible {@link AccessoryNest} and return a result from such action or a default value if none found
     *
     * @param holderStack  Potential stack linked to a AccessoryNest
     * @param livingEntity Potential Living Entity involved with any stack changes
     * @param func         Action being done
     * @param defaultValue Default value if stack is not a AccessoryNest
     */
    static <T> T attemptFunction(ItemStack holderStack, @Nullable LivingEntity livingEntity, Function<Map<ItemStack, Accessory>, T> func, T defaultValue){
        var data = AccessoryNestUtils.getData(holderStack);

        if(data == null) return defaultValue;

        var t = func.apply(data.getMap());

        data.getNest().checkAndHandleStackChanges(holderStack, data, livingEntity);

        return t;
    }

    /**
     * Method used to perform some action on a possible {@link AccessoryNest}
     *
     * @param holderStack  Potential stack linked to a AccessoryNest
     * @param livingEntity Potential Living Entity involved with any stack changes
     * @param consumer     Action being done
     */
    static void attemptConsumer(ItemStack holderStack, @Nullable LivingEntity livingEntity, Consumer<Map<ItemStack, Accessory>> consumer){
        var data = AccessoryNestUtils.getData(holderStack);

        if(data == null) return;

        consumer.accept(data.getMap());

        data.getNest().checkAndHandleStackChanges(holderStack, data, livingEntity);
    }

    static boolean isAccessoryNest(ItemStack holderStack) {
        return AccessoriesAPI.getAccessory(holderStack) instanceof AccessoryNest;
    }

    /**
     * Check and handle any inner stack changes that may have occurred from an action performed on the stacks within the nest
     *
     * @param holderStack  HolderStack containing the nest of stacks
     * @param data         StackData linked to the given HolderStack
     * @param livingEntity Potential Living Entity involved with any stack changes
     */
    default void checkAndHandleStackChanges(ItemStack holderStack, AccessoryNestUtils.StackData data, @Nullable LivingEntity livingEntity){
        var prevStacks = data.getDefensiveCopies();
        var currentStacks = data.getStacks();

        for (int i = 0; i < prevStacks.size(); i++) {
            var currentStack = currentStacks.get(i);

            if(ItemStack.matches(prevStacks.get(i), currentStack)) continue;

            this.setInnerStack(holderStack, i, currentStack);
        }
    }

    //--

    @Override
    default void tick(ItemStack stack, SlotReference reference) {
        attemptConsumer(stack, reference.entity(), map -> map.forEach((stack1, accessory) -> accessory.tick(stack1, reference)));
    }

    @Override
    default void onEquip(ItemStack stack, SlotReference reference) {
        attemptConsumer(stack, reference.entity(), map -> map.forEach((stack1, accessory) -> accessory.onEquip(stack1, reference)));
    }

    @Override
    default void onUnequip(ItemStack stack, SlotReference reference) {
        attemptConsumer(stack, reference.entity(), map -> map.forEach((stack1, accessory) -> accessory.onUnequip(stack1, reference)));
    }

    @Override
    default boolean canEquip(ItemStack stack, SlotReference reference) {
        return attemptFunction(stack, reference.entity(), map -> {
            MutableBoolean canEquip = new MutableBoolean(true);

            map.forEach((stack1, accessory) -> canEquip.setValue(canEquip.booleanValue() && accessory.canEquip(stack1, reference)));

            return canEquip.getValue();
        }, false);
    }

    @Override
    default boolean canUnequip(ItemStack stack, SlotReference reference) {
        return attemptFunction(stack, reference.entity(), map -> {
            MutableBoolean canUnequip = new MutableBoolean(true);

            map.forEach((stack1, accessory) -> canUnequip.setValue(canUnequip.booleanValue() && accessory.canUnequip(stack1, reference)));

            return canUnequip.getValue();
        }, false);
    }

    @Override
    default Multimap<Attribute, AttributeModifier> getModifiers(ItemStack stack, SlotReference reference, UUID uuid) {
        var map = Accessory.super.getModifiers(stack, reference, uuid);

        // TODO: May need to deal with potential collisions when using the specific passed UUID
        attemptConsumer(stack, reference.entity(), innerMap -> innerMap.forEach((stack1, accessory) -> map.putAll(accessory.getModifiers(stack, reference, uuid))));

        return map;
    }

    @Override
    default void getAttributesTooltip(ItemStack stack, SlotType type, List<Component> tooltips) {
        attemptConsumer(stack, null, map -> map.forEach((stack1, accessory) -> accessory.getAttributesTooltip(stack1, type, tooltips)));
    }

    @Override
    default void getExtraTooltip(ItemStack stack, List<Component> tooltips) {
        attemptConsumer(stack, null, map -> map.forEach((stack1, accessory) -> accessory.getExtraTooltip(stack1, tooltips)));
    }
}
