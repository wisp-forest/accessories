package io.wispforest.accessories.api.core;

import io.wispforest.accessories.api.action.ActionResponse;
import io.wispforest.accessories.api.action.ActionResponseBuffer;
import io.wispforest.accessories.api.action.CurseBound;
import io.wispforest.accessories.api.attributes.AccessoryAttributeBuilder;
import io.wispforest.accessories.api.components.AccessoriesDataComponents;
import io.wispforest.accessories.api.components.AccessoryNestContainerContents;
import io.wispforest.accessories.api.events.DropRule;
import io.wispforest.accessories.api.slot.SlotEntryReference;
import io.wispforest.accessories.api.slot.SlotPath;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.api.slot.SlotType;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.network.chat.Component;
import net.minecraft.util.TriState;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import static io.wispforest.accessories.api.core.AccessoryNestUtils.*;

/**
 * An {@link Accessory} that contains and delegates to other accessories in some way
 */
// TODO: POSSIBLY LOOK INTO METHOD OF INDICATING WHEN A SET CALL IS REQUIRED OR FROM ANOTHER MOD TO ALLOW IMMUTABLE NESTS?
public interface AccessoryNest extends Accessory {

    static boolean isNest(ItemStack holderStack) {
        return AccessoryRegistry.getAccessoryOrDefault(holderStack) instanceof AccessoryNest;
    }

    //--

    /**
     * @return all inner accessory stacks
     */
    default List<ItemStack> getInnerStacks(ItemStack holderStack) {
        var data = holderStack.get(AccessoriesDataComponents.NESTED_ACCESSORIES);

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
        if(!AccessoryNest.isNest(holderStack)) return false;
        if(AccessoryNest.isNest(newStack) && !this.allowDeepRecursion()) return false;

        holderStack.update(
                AccessoriesDataComponents.NESTED_ACCESSORIES,
                new AccessoryNestContainerContents(List.of()),
                contents -> contents.setStack(index, newStack));

        return true;
    }

    /**
     * By default, accessory nests can only go one layer deep as it's hard to track the stack modifications any further
     *
     * @return Whether this implementation of the Accessory nest allows for further nesting of other Nests
     */
    default boolean allowDeepRecursion() {
        // TODO: MAKE DATA ADJUSTABLE?
        return false;
    }

    default List<Pair<DropRule, ItemStack>> getDropRules(ItemStack stack, SlotReference reference, DamageSource source) {
        var innerRules = new ArrayList<Pair<DropRule, ItemStack>>();

        var innerStacks = getInnerStacks(stack);

        for (int i = 0; i < innerStacks.size(); i++) {
            var innerStack = innerStacks.get(i);

            var rule = AccessoryRegistry.getAccessoryOrDefault(innerStack).getDropRule(innerStack, SlotPath.cloneWithInnerIndex(reference, i), source);

            innerRules.add(Pair.of(rule, innerStack));
        }

        return innerRules;
    }

    /**
     * Check and handle any inner stack changes that may have occurred from an action performed on the stacks within the nest
     *
     * @param holderStack  HolderStack containing the nest of stacks
     * @param data         StackData linked to the given HolderStack
     * @param livingEntity Potential Living Entity involved with any stack changes
     */
    default void onStackChanges(ItemStack holderStack, AccessoryNestContainerContents data, @Nullable LivingEntity livingEntity){}

    //--

    @Override
    default void onBreak(ItemStack stack, SlotReference reference) {
        consumeEntries(stack, reference, Accessory::onBreak);
    }

    @Override
    default boolean canEquipFromUse(ItemStack stack, SlotReference reference) {
        return handleEntries(stack, reference, new PathedAccessoryFunction<SlotReference, TriState>() {
            @Override
            public TriState handle(Accessory accessory, ItemStack innerStack, SlotReference innerRef) {
                return accessory.canEquipFromUse(innerStack, innerRef) ? TriState.DEFAULT : TriState.FALSE;
            }

            @Override
            public boolean isDefaulted(TriState state) {
                return state != TriState.DEFAULT;
            }
        }).toBoolean(true);
    }

    @Override
    default void onEquipFromUse(ItemStack stack, SlotReference reference) {
        consumeEntries(stack, reference, Accessory::onEquipFromUse);
    }

    @Override
    default void tick(ItemStack stack, SlotReference reference) {
        consumeEntries(stack, reference, Accessory::tick);
    }

    @Override
    default void onEquip(ItemStack stack, SlotReference reference) {
        consumeEntries(stack, reference, Accessory::onEquip);
    }

    @Override
    default void onUnequip(ItemStack stack, SlotReference reference) {
        consumeEntries(stack, reference, Accessory::onUnequip);
    }

    @Override
    default void canEquip(ItemStack stack, SlotReference reference, ActionResponseBuffer buffer) {
        handleEntries(stack, reference, new PathedAccessoryFunction<SlotReference, ActionResponseBuffer>() {
            @Override
            public ActionResponseBuffer handle(Accessory accessory, ItemStack innerStack, SlotReference innerRef) {
                accessory.canEquip(innerStack, innerRef, buffer);

                return buffer;
            }

            @Override
            public boolean isDefaulted(ActionResponseBuffer buffer) {
                return buffer.shouldReturnEarly();
            }
        });

        // TODO: REMOVE canEquip call WHEN DEPRECATION PHASE HAS BEEN LONG ENOUGH
        if (buffer.shouldReturnEarly() || canEquip(stack, reference)) return;

        buffer.respondWith(ActionResponse.of(false, Component.literal("Such an item can not be equipped!")));
    }

    @Override
    default void canUnequip(ItemStack stack, SlotReference reference, ActionResponseBuffer buffer) {
        if (CurseBound.checkIfCursed(stack, reference.entity(), buffer)) return;

        handleEntries(stack, reference, new PathedAccessoryFunction<SlotReference, ActionResponseBuffer>() {
            @Override
            public ActionResponseBuffer handle(Accessory accessory, ItemStack innerStack, SlotReference innerRef) {
                accessory.canUnequip(innerStack, innerRef, buffer);

                return buffer;
            }

            @Override
            public boolean isDefaulted(ActionResponseBuffer buffer) {
                return buffer.shouldReturnEarly();
            }
        });

        // TODO: REMOVE canUnequip call WHEN DEPRECATION PHASE HAS BEEN LONG ENOUGH
        if (buffer.shouldReturnEarly() || canUnequip(stack, reference)) return;

        buffer.respondWith(ActionResponse.of(false, Component.literal("Such an item can not be unequipped!")));
    }

    @Override
    default void getDynamicModifiers(ItemStack stack, SlotReference reference, AccessoryAttributeBuilder builder) {
        consumeEntries(stack, reference,
            (accessory, innerStack, innerRef) -> accessory.getDynamicModifiers(innerStack, innerRef, new AccessoryAttributeBuilder(innerRef, builder))
        );
    }

    @Override
    default void getAttributesTooltip(ItemStack stack, SlotType type, List<Component> tooltips, Item.TooltipContext tooltipContext, TooltipFlag tooltipType) {
        consumeEntries(stack,
            (accessory, innerStack) -> accessory.getAttributesTooltip(innerStack, type, tooltips, tooltipContext, tooltipType)
        );
    }

    @Override
    default void getExtraTooltip(ItemStack stack, List<Component> tooltips, Item.TooltipContext tooltipContext, TooltipFlag tooltipType) {
        consumeEntries(stack,
            (accessory, innerStack) -> accessory.getExtraTooltip(innerStack, tooltips, tooltipContext, tooltipType)
        );
    }

    //--

    @Deprecated(forRemoval = true)
    static boolean checkIfChangesOccurred(ItemStack holderStack, @Nullable LivingEntity livingEntity, AccessoryNestContainerContents data) {
        return AccessoryNestUtils.checkIfChangesOccurred(holderStack, livingEntity, data);
    }

    /**
     * Method used to perform some action on a possible {@link AccessoryNest} and return a result from that action or a default value if none found
     *
     * @param holderStack   Potential stack linked to a AccessoryNest
     * @param slotReference The primary SlotReference used from the given call
     * @param func          Action being done
     * @param defaultValue  Default value if stack is not a AccessoryNest
     */
    @Deprecated(forRemoval = true)
    static <T> T attemptFunction(ItemStack holderStack, SlotReference slotReference, Function<Map<SlotEntryReference, Accessory>, T> func, T defaultValue){
        var data = AccessoryNestUtils.getData(holderStack);

        if(data == null) return defaultValue;

        var t = func.apply(data.getMap(slotReference));

        AccessoryNestUtils.checkIfChangesOccurred(holderStack, null, data);

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
    @Deprecated(forRemoval = true)
    static <T> T attemptFunction(ItemStack holderStack, @Nullable LivingEntity livingEntity, Function<Map<ItemStack, Accessory>, T> func, T defaultValue){
        var data = AccessoryNestUtils.getData(holderStack);

        if(data == null) return defaultValue;

        var t = func.apply(data.getMap());

        AccessoryNestUtils.checkIfChangesOccurred(holderStack, livingEntity, data);

        return t;
    }

    /**
     * Method used to perform some action on a possible {@link AccessoryNest}
     *
     * @param holderStack   Potential stack linked to a AccessoryNest
     * @param slotReference Potential Living Entity involved with any stack changes
     * @param consumer      Action being done
     */
    @Deprecated(forRemoval = true)
    static void attemptConsumer(ItemStack holderStack, SlotReference slotReference, Consumer<Map<SlotEntryReference, Accessory>> consumer){
        var data = AccessoryNestUtils.getData(holderStack);

        if(data == null) return;

        consumer.accept(data.getMap(slotReference));

        AccessoryNestUtils.checkIfChangesOccurred(holderStack, slotReference.entity(), data);
    }

    /**
     * Method used to perform some action on a possible {@link AccessoryNest}
     *
     * @param holderStack  Potential stack linked to a AccessoryNest
     * @param livingEntity Potential Living Entity involved with any stack changes
     * @param consumer     Action being done
     */
    @Deprecated(forRemoval = true)
    static void attemptConsumer(ItemStack holderStack, @Nullable LivingEntity livingEntity, Consumer<Map<ItemStack, Accessory>> consumer) {
        var data = AccessoryNestUtils.getData(holderStack);

        if (data == null) return;

        consumer.accept(data.getMap());

        AccessoryNestUtils.checkIfChangesOccurred(holderStack, livingEntity, data);
    }

    //--

    @Deprecated(forRemoval = true)
    static boolean isAccessoryNest(ItemStack holderStack) {
        return isNest(holderStack);
    }

    @Override
    @Deprecated(forRemoval = true)
    default boolean canEquipFromUse(ItemStack stack) {
        return handleEntries(stack, new AccessoryFunction<TriState>() {
            @Override
            public TriState handle(Accessory accessory, ItemStack innerStack) {
                return accessory.canEquipFromUse(innerStack) ? TriState.DEFAULT : TriState.FALSE;
            }

            @Override
            public boolean isDefaulted(TriState state) {
                return state != TriState.DEFAULT;
            }
        }).toBoolean(true);
    }

    @Override
    default boolean canEquip(ItemStack stack, SlotReference reference) {
        return true;
    }

    @Override
    default boolean canUnequip(ItemStack stack, SlotReference reference) {
        if(EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)) {
            return reference.entity() instanceof Player player && player.isCreative();
        }

        return true;
    }
}
