package io.wispforest.accessories.api;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.attributes.AccessoryAttributeBuilder;
import io.wispforest.accessories.api.attributes.AccessoryAttributeUtils;
import io.wispforest.accessories.api.core.Accessory;
import io.wispforest.accessories.api.core.AccessoryRegistry;
import io.wispforest.accessories.api.slot.SlotBasedPredicate;
import io.wispforest.accessories.api.slot.SlotPredicateRegistry;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.api.slot.validator.SlotValidatorRegistry;
import io.wispforest.accessories.impl.AccessoryAttributeLogic;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

/**
 * Class containing the bulk of API calls for either registering {@link Accessory} instances, new {@link SlotBasedPredicate}s and more.
 */
@Deprecated(forRemoval = true)
public class AccessoriesAPI {

    /**
     * Registers an accessory implementation for a given item.
     */
    @Deprecated(forRemoval = true)
    public static void registerAccessory(Item item, Accessory accessory) {
        AccessoryRegistry.register(item, accessory);
    }

    /**
     * @return the accessory bound to this stack or {@code null} if there is none
     */
    @Deprecated(forRemoval = true)
    @Nullable
    public static Accessory getAccessory(ItemStack stack){
        return AccessoryRegistry.getAccessoryOrDefault(stack);
    }

    /**
     * @return the accessory bound to this item or {@code null} if there is none
     */
    @Deprecated(forRemoval = true)
    @Nullable
    public static Accessory getAccessory(Item item) {
        return AccessoryRegistry.getAccessory(item);
    }

    /**
     * @return the accessory bound to this stack or {@link #defaultAccessory()} if there is none
     */
    @Deprecated(forRemoval = true)
    public static Accessory getOrDefaultAccessory(ItemStack stack){
        return AccessoryRegistry.getAccessoryOrDefault(stack);
    }

    /**
     * @return the accessory bound to this item or {@link #defaultAccessory()} if there is none
     */
    @Deprecated(forRemoval = true)
    public static Accessory getOrDefaultAccessory(Item item){
        return AccessoryRegistry.getAccessoryOrDefault(item);
    }

    /**
     * @return the default accessory implementation
     */
    @Deprecated(forRemoval = true)
    public static Accessory defaultAccessory(){
        return AccessoryRegistry.defaultAccessory();
    }

    @Deprecated(forRemoval = true)
    public static boolean isDefaultAccessory(Accessory accessory) {
        return AccessoryRegistry.isDefaultAccessory(accessory);
    }

    /**
     * @return If a given {@link ItemStack} is found either to have an {@link Accessory} besides the
     * default or if the given stack has valid slots which it can be equipped
     */
    @Deprecated(forRemoval = true)
    public static boolean isValidAccessory(ItemStack stack, Level level){
        return SlotValidatorRegistry.isValidAccessory(stack, level);
    }

    @Deprecated(forRemoval = true)
    public static boolean isValidAccessory(ItemStack stack, Level level, @Nullable LivingEntity entity){
        return SlotValidatorRegistry.isValidAccessory(stack, level, entity);
    }

    //--

    @Deprecated(forRemoval = true)
    public static AccessoryAttributeBuilder getAttributeModifiers(ItemStack stack, SlotReference slotReference){
        return AccessoryAttributeLogic.getAttributeModifiers(stack, slotReference, false);
    }

    @Deprecated(forRemoval = true)
    public static AccessoryAttributeBuilder getAttributeModifiers(ItemStack stack, SlotReference slotReference, boolean useTooltipCheck){
        return AccessoryAttributeLogic.getAttributeModifiers(stack, slotReference.entity(), slotReference.slotName(), slotReference.index(), useTooltipCheck);
    }

    @Deprecated(forRemoval = true)
    public static AccessoryAttributeBuilder getAttributeModifiers(ItemStack stack, String slotName, int slot){
        return AccessoryAttributeLogic.getAttributeModifiers(stack, null, slotName, slot);
    }

    @Deprecated(forRemoval = true)
    public static AccessoryAttributeBuilder getAttributeModifiers(ItemStack stack, @Nullable LivingEntity entity, String slotName, int slot){
        return AccessoryAttributeLogic.getAttributeModifiers(stack, entity, slotName, slot, false);
    }

    /**
     * Attempts to get any at all AttributeModifier's found on the stack either though NBT or the Accessory bound
     * to the {@link ItemStack}'s item
     */
    @Deprecated(forRemoval = true)
    public static AccessoryAttributeBuilder getAttributeModifiers(ItemStack stack, @Nullable LivingEntity entity, String slotName, int slot, boolean hideTooltipIfDisabled){
        return AccessoryAttributeLogic.getAttributeModifiers(stack, entity, slotName, slot, hideTooltipIfDisabled);
    }

    @Deprecated(forRemoval = true)
    public static void addAttribute(ItemStack stack, String slotName, Holder<Attribute> attribute, ResourceLocation location, double amount, AttributeModifier.Operation operation, boolean isStackable) {
        AccessoryAttributeUtils.addAttribute(stack, slotName, attribute, location, amount, operation, isStackable);
    }

    //--

    /**
     * @return {@link UUID} based on the provided {@link SlotType#name} and entry index
     */
    @Deprecated(forRemoval = true)
    public static ResourceLocation createSlotLocation(SlotType slotType, int index) {
        return createSlotLocation(slotType.name(), index);
    }

    /**
     * @return {@link UUID} based on the provided slot name and entry index
     */
    @Deprecated(forRemoval = true)
    public static ResourceLocation createSlotLocation(String slotName, int index) {
        return Accessories.of(slotName.replace(":", "_") + "/" + index);
    }

    //--

    /**
     * Used to check if the given {@link ItemStack} is valid for the given LivingEntity and SlotReference
     * based on {@link io.wispforest.accessories.api.slot.validator.SlotValidator}s bound to the Slot and the {@link Accessory} bound to the stack if present
     */
    @Deprecated(forRemoval = true)
    public static boolean canInsertIntoSlot(ItemStack stack, SlotReference reference){
        return SlotValidatorRegistry.canInsertIntoSlot(stack, reference);
    }

    /**
     * Method used to check weather or not the given stack can be equipped within the slot referenced
     *
     * @param stack
     * @param reference
     * @return if the stack can be equipped or not
     */
    @Deprecated(forRemoval = true)
    public static boolean canEquip(ItemStack stack, SlotReference reference){
        return AccessoryRegistry.canEquip(stack, reference);
    }

    /**
     * Method used to check weather or not the given stack can be unequipped within the slot referenced
     *
     * @param stack
     * @param reference
     * @return if the stack can be unequipped or not
     */
    @Deprecated(forRemoval = true)
    public static boolean canUnequip(ItemStack stack, SlotReference reference){
        return AccessoryRegistry.canUnequip(stack, reference);
    }

    /**
     * @return All valid {@link SlotType}s for the given {@link ItemStack} based on the {@link LivingEntity}
     * available {@link SlotType}s
     */
    @Deprecated(forRemoval = true)
    public static Collection<SlotType> getValidSlotTypes(LivingEntity entity, ItemStack stack){
        return SlotValidatorRegistry.getValidSlotTypes(entity, stack);
    }

    @Deprecated(forRemoval = true)
    public static Collection<SlotType> getStackSlotTypes(Level level, ItemStack stack){
        return SlotValidatorRegistry.getStackSlotTypes(level, null, stack);
    }

    @Deprecated(forRemoval = true)
    public static Collection<SlotType> getStackSlotTypes(LivingEntity entity, ItemStack stack) {
        return SlotValidatorRegistry.getStackSlotTypes(entity.level(), entity, stack);
    }

    @Deprecated(forRemoval = true)
    public static Collection<SlotType> getStackSlotTypes(Level level, @Nullable LivingEntity entity, ItemStack stack) {
        return SlotValidatorRegistry.getStackSlotTypes(level, entity, stack);
    }

    @Deprecated(forRemoval = true)
    public static Collection<SlotType> getUsedSlotsFor(Player player) {
        return AccessoriesCapability.getUsedSlotsFor(player);
    }

    @Deprecated(forRemoval = true)
    public static Collection<SlotType> getUsedSlotsFor(LivingEntity entity, Container container) {
        return AccessoriesCapability.getUsedSlotsFor(entity, container);
    }

    @Deprecated(forRemoval = true)
    public static void breakStack(SlotReference reference){
        reference.breakStack();
    }

    //--

    @Deprecated(forRemoval = true)
    @Nullable
    public static SlotBasedPredicate getPredicate(ResourceLocation location) {
        return SlotPredicateRegistry.getPredicate(location);
    }

    @Deprecated(forRemoval = true)
    public static void registerPredicate(ResourceLocation location, SlotBasedPredicate predicate) {
        SlotPredicateRegistry.register(location, predicate);
    }

    @Deprecated(forRemoval = true)
    public static boolean getPredicateResults(Set<ResourceLocation> predicateIds, Level level, SlotType slotType, int index, ItemStack stack){
        return SlotValidatorRegistry.getPredicateResults(predicateIds, level, null, slotType, index, stack);
    }

    @Deprecated(forRemoval = true)
    public static boolean getPredicateResults(Set<ResourceLocation> predicateIds, Level level, @Nullable LivingEntity entity, SlotType slotType, int index, ItemStack stack){
        return SlotValidatorRegistry.getPredicateResults(predicateIds, level, entity, slotType, index, stack);
    }
}