package io.wispforest.accessories.api;

import io.wispforest.accessories.api.attributes.AccessoryAttributeBuilder;
import io.wispforest.accessories.api.components.AccessoriesDataComponents;
import io.wispforest.accessories.api.components.AccessoryNestContainerContents;
import io.wispforest.accessories.api.slot.SlotEntryReference;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.impl.AccessoryNestUtils;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * An {@link Accessory} that contains and delegates to other accessories in some way
 */
public interface AccessoryNest extends Accessory {

    /**
     * @return all inner accessory stacks
     */
    default List<ItemStack> getInnerStacks(ItemStack holderStack) {
        var data = AccessoriesDataComponents.readOrDefault(AccessoriesDataComponents.NESTED_ACCESSORIES, holderStack);

        return data == null ? List.of() : data.accessories();
    }

    /**
     * Sets a given stack at the specified index for the passed holder stack
     *
     * @param holderStack The given HolderStack
     * @param index       The target index
     * @param newStack    The new stack replacing the given index
     */
    default boolean setInnerStack(ItemStack holderStack, int index, ItemStack newStack) {
        if(!AccessoryNest.isAccessoryNest(holderStack)) return false;
        if(AccessoryNest.isAccessoryNest(newStack) && !this.allowDeepRecursion()) return false;

        AccessoriesDataComponents.update(
                AccessoriesDataComponents.NESTED_ACCESSORIES,
                holderStack,
                contents -> contents.setStack(index, newStack));

        return true;
    }

    /**
     * By default, accessory nests can only go one layer deep as it's hard to track the stack modifications any further
     *
     * @return Whether this implementation of the Accessory nest allows for further nesting of other Nests
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
     * Method used to perform some action on a possible {@link AccessoryNest} and return a result from that action or a default value if none found
     *
     * @param holderStack   Potential stack linked to a AccessoryNest
     * @param slotReference The primary SlotReference used from the given call
     * @param func          Action being done
     * @param defaultValue  Default value if stack is not a AccessoryNest
     */
    static <T> T attemptFunction(ItemStack holderStack, SlotReference slotReference, Function<Map<SlotEntryReference, Accessory>, T> func, T defaultValue){
        var data = AccessoryNestUtils.getData(holderStack);

        if(data == null) return defaultValue;

        var nest = (AccessoryNest) AccessoriesAPI.getAccessory(holderStack);

        var t = func.apply(data.getMap(slotReference));

        if(data.hasChangesOccured(holderStack)) nest.onStackChanges(holderStack, AccessoriesDataComponents.readOrDefault(AccessoriesDataComponents.NESTED_ACCESSORIES, holderStack), slotReference.entity());

        return t;
    }

    /**
     * Method used to perform some action on a possible {@link AccessoryNest} and return a result from that action or a default value if none found
     *
     * @param holderStack   Potential stack linked to a AccessoryNest
     * @param livingEntity Potential Living Entity involved with any stack changes
     * @param func          Action being done
     * @param defaultValue  Default value if stack is not a AccessoryNest
     */
    static <T> T attemptFunction(ItemStack holderStack, @Nullable LivingEntity livingEntity, Function<Map<ItemStack, Accessory>, T> func, T defaultValue){
        var data = AccessoryNestUtils.getData(holderStack);

        if(data == null) return defaultValue;

        var nest = (AccessoryNest) AccessoriesAPI.getAccessory(holderStack);

        var t = func.apply(data.getMap());

        if(data.hasChangesOccured(holderStack)) nest.onStackChanges(holderStack, AccessoriesDataComponents.readOrDefault(AccessoriesDataComponents.NESTED_ACCESSORIES, holderStack), livingEntity);

        return t;
    }

    /**
     * Method used to perform some action on a possible {@link AccessoryNest}
     *
     * @param holderStack   Potential stack linked to a AccessoryNest
     * @param slotReference Potential Living Entity involved with any stack changes
     * @param consumer      Action being done
     */
    static void attemptConsumer(ItemStack holderStack, SlotReference slotReference, Consumer<Map<SlotEntryReference, Accessory>> consumer){
        var data = AccessoryNestUtils.getData(holderStack);

        if(data == null) return;

        var nest = (AccessoryNest) AccessoriesAPI.getAccessory(holderStack);

        consumer.accept(data.getMap(slotReference));

        if(data.hasChangesOccured(holderStack)) nest.onStackChanges(holderStack, AccessoriesDataComponents.readOrDefault(AccessoriesDataComponents.NESTED_ACCESSORIES, holderStack), slotReference.entity());
    }

    /**
     * Method used to perform some action on a possible {@link AccessoryNest}
     *
     * @param holderStack  Potential stack linked to a AccessoryNest
     * @param livingEntity Potential Living Entity involved with any stack changes
     * @param consumer     Action being done
     */
    static void attemptConsumer(ItemStack holderStack, @Nullable LivingEntity livingEntity, Consumer<Map<ItemStack, Accessory>> consumer) {
        var data = AccessoryNestUtils.getData(holderStack);

        if (data == null) return;

        var nest = (AccessoryNest) AccessoriesAPI.getAccessory(holderStack);

        consumer.accept(data.getMap());

        if(data.hasChangesOccured(holderStack)) nest.onStackChanges(holderStack, AccessoriesDataComponents.readOrDefault(AccessoriesDataComponents.NESTED_ACCESSORIES, holderStack), livingEntity);
    }

    //--

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
    default void onStackChanges(ItemStack holderStack, AccessoryNestContainerContents data,  @Nullable LivingEntity livingEntity){}

    //--

    @Override
    default void tick(ItemStack stack, SlotReference reference) {
        attemptConsumer(stack, reference, map -> map.forEach((entryRef, accessory) -> accessory.tick(entryRef.stack(), entryRef.reference())));
    }

    @Override
    default void onEquip(ItemStack stack, SlotReference reference) {
        attemptConsumer(stack, reference, map -> map.forEach((entryRef, accessory) -> accessory.onEquip(entryRef.stack(), entryRef.reference())));
    }

    @Override
    default void onUnequip(ItemStack stack, SlotReference reference) {
        attemptConsumer(stack, reference, map -> map.forEach((entryRef, accessory) -> accessory.onUnequip(entryRef.stack(), entryRef.reference())));
    }

    @Override
    default boolean canEquip(ItemStack stack, SlotReference reference) {
        return attemptFunction(stack, reference, map -> {
            MutableBoolean canEquip = new MutableBoolean(true);

            map.forEach((entryRef, accessory) -> canEquip.setValue(canEquip.booleanValue() && accessory.canEquip(entryRef.stack(), entryRef.reference())));

            return canEquip.getValue();
        }, false);
    }

    @Override
    default boolean canUnequip(ItemStack stack, SlotReference reference) {
        return attemptFunction(stack, reference, map -> {
            MutableBoolean canUnequip = new MutableBoolean(true);

            map.forEach((entryRef, accessory) -> canUnequip.setValue(canUnequip.booleanValue() && accessory.canUnequip(entryRef.stack(), entryRef.reference())));

            return canUnequip.getValue();
        }, true);
    }

    @Override
    default void getDynamicModifiers(ItemStack stack, SlotReference reference, AccessoryAttributeBuilder builder) {
        attemptConsumer(stack, reference, innerMap -> innerMap.forEach((entryRef, accessory) -> {
            var innerBuilder = new AccessoryAttributeBuilder(entryRef.reference());

            accessory.getDynamicModifiers(entryRef.stack(), entryRef.reference(), innerBuilder);

            builder.addFrom(innerBuilder);
        }));
    }

    @Override
    default void getAttributesTooltip(ItemStack stack, SlotType type, List<Component> tooltips, TooltipFlag tooltipType) {
        attemptConsumer(stack, (LivingEntity) null, map -> map.forEach((stack1, accessory) -> accessory.getAttributesTooltip(stack1, type, tooltips, tooltipType)));
    }

    @Override
    default void getExtraTooltip(ItemStack stack, List<Component> tooltips,TooltipFlag tooltipType) {
        attemptConsumer(stack, (LivingEntity) null, map -> map.forEach((stack1, accessory) -> accessory.getExtraTooltip(stack1, tooltips, tooltipType)));
    }
}
