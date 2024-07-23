package io.wispforest.accessories.api;

import io.wispforest.accessories.api.attributes.AccessoryAttributeBuilder;
import io.wispforest.accessories.api.components.AccessoriesDataComponents;
import io.wispforest.accessories.api.components.AccessoryItemAttributeModifiers;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.impl.AccessoriesEventHandler;
import io.wispforest.accessories.mixin.LivingEntityAccessor;
import io.wispforest.accessories.networking.client.AccessoryBreak;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Main interface for implementing accessory functionality within the main API.
 */
public interface Accessory {

    /**
     * Called on every tick of the wearing {@link LivingEntity} on both client and server.
     *
     * @param stack the stack being ticked
     * @param reference the slot the accessory is in
     */
    default void tick(ItemStack stack, SlotReference reference){}

    /**
     * Called when the accessory is equipped
     *
     * @param stack the stack being equipped
     * @param reference the slot the accessory is in
     */
    default void onEquip(ItemStack stack, SlotReference reference){}

    /**
     * Called when the accessory is unequipped
     *
     * @param stack the stack being unequipped
     * @param reference the slot the accessory is in
     */
    default void onUnequip(ItemStack stack, SlotReference reference){}

    /**
     * @param stack the stack to be equipped
     * @param reference the slot the accessory is in
     * @return whether the given stack can be equipped
     */
    default boolean canEquip(ItemStack stack, SlotReference reference){
        return true;
    }

    /**
     * @param stack the stack to be unequipped
     * @param reference the slot the accessory is in
     * @return whether the given stack can be unequipped
     */
    default boolean canUnequip(ItemStack stack, SlotReference reference){
        if(EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)) {
            return reference.entity() instanceof Player player && player.isCreative();
        }

        return true;
    }

    /**
     * Helper method used to fill the passed {@link AccessoryAttributeBuilder} for every call from
     * {@link AccessoriesAPI#getAttributeModifiers}
     *
     * @param stack     The Stack attempting to be unequipped
     * @param reference The reference to the targeted {@link LivingEntity}, slot and index
     * @param builder   The builder to which attributes are to be added
     */
    default void getDynamicModifiers(ItemStack stack, SlotReference reference, AccessoryAttributeBuilder builder){
        getModifiers(stack, reference, builder);
    }

    /**
     * Helper method used to fill the passed {@link AccessoryItemAttributeModifiers.Builder} when called to modify
     * the given default {@link AccessoriesDataComponents#ATTRIBUTES} right before Registry freeze occurs. This is
     * useful for attributes that are not meant to be changed and remain mostly static.
     *
     * @param item      The item to which the given attributes will be defaulted for
     * @param builder   The builder to which attributes are to be added
     */
    default void getStaticModifiers(Item item, AccessoryItemAttributeModifiers.Builder builder){

    }

    /**
     * Returns the following drop rule for the given Item
     *
     * @param stack     The Stack being prepared for dropping
     * @param reference The reference to the targeted {@link LivingEntity}, slot and index
     * @param source    The specific {@link DamageSource} that lead to the drop rule evaluation
     */
    default DropRule getDropRule(ItemStack stack, SlotReference reference, DamageSource source){
        return DropRule.DEFAULT;
    }

    //--

    /**
     * Method called when equipping the given accessory from hotbar by right-clicking
     *
     * @param stack The Stack being prepared for dropping
     * @param reference The reference to the targeted {@link LivingEntity}, slot and index
     */
    default void onEquipFromUse(ItemStack stack, SlotReference reference){
        var sound = getEquipSound(stack, reference);

        if(sound == null) return;

        reference.entity().playSound(sound.event().value(), sound.volume(), sound.pitch());
    }

    /**
     * Returns the equipping sound from use for a given stack
     *
     * @param stack The Stack being prepared for dropping
     * @param reference The reference to the targeted {@link LivingEntity}, slot and index
     */
    @Nullable
    default SoundEventData getEquipSound(ItemStack stack, SlotReference reference){
        return new SoundEventData(SoundEvents.ARMOR_EQUIP_GENERIC, 1.0f, 1.0f);
    }

    /**
     * Returns whether the given stack can be equipped from use
     *
     * @param stack The Stack being prepared for dropping
     * @param reference The reference to the targeted {@link LivingEntity}, slot and index
     */
    default boolean canEquipFromUse(ItemStack stack, SlotReference reference){
        return true;
    }

    /**
     * Method used to render client based particles when {@link AccessoriesAPI#breakStack(SlotReference)} is
     * called on the server and the {@link AccessoryBreak} packet is received
     *
     * @param stack The Stack being prepared for dropping
     * @param reference The reference to the targeted {@link LivingEntity}, slot and index
     */
    default void onBreak(ItemStack stack, SlotReference reference) {
        ((LivingEntityAccessor) reference.entity()).accessors$breakItem(stack);
    }

    /**
     * @return Return the max stack amount allowed when equipping a given stack into an accessories inventory
     */
    default int maxStackSize(ItemStack stack){
        return stack.getMaxStackSize();
    }

    //--

    /**
     * Method used to add tooltip info for attribute like data based on a given slot type
     *
     * @param stack The Stack being referenced
     * @param type The SlotType being referenced
     * @param tooltips Final list containing the tooltip info
     * @param tooltipContext Current tooltip context
     * @param tooltipType Current tooltipFlag
     */
    default void getAttributesTooltip(ItemStack stack, SlotType type, List<Component> tooltips, Item.TooltipContext tooltipContext, TooltipFlag tooltipType){
        getAttributesTooltip(stack, type, tooltips);
    }

    /**
     * Method used to add any additional tooltip information to a given {@link Accessory} tooltip after
     * {@link AccessoriesEventHandler#addEntityBasedTooltipData} if called and at the
     * end of the {@link AccessoriesEventHandler#getTooltipData} call.
     *
     * <p>
     *     Do note that <b>if the given entity</b> is found to not be null, the list passed
     *     contains all tooltip info allowing for positioning before or after the tooltip info
     * </p>
     *
     * @param stack The Stack being referenced
     * @param tooltips Final list containing the tooltip info
     * @param tooltipContext Current tooltip context
     * @param tooltipType Current tooltipFlag
     */
    default void getExtraTooltip(ItemStack stack, List<Component> tooltips, Item.TooltipContext tooltipContext, TooltipFlag tooltipType){
        getExtraTooltip(stack, tooltips);
    }

    //--

    /**
     * @deprecated Use {@link #getDynamicModifiers} instead
     */
    @ApiStatus.ScheduledForRemoval(inVersion = "1.22")
    @Deprecated(forRemoval = true)
    default void getModifiers(ItemStack stack, SlotReference reference, AccessoryAttributeBuilder builder){}

    /**
     * @deprecated Use {@link #getAttributesTooltip(ItemStack, SlotType, List, Item.TooltipContext, TooltipFlag)}
     */
    @Deprecated(forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "1.22")
    default void getAttributesTooltip(ItemStack stack, SlotType type, List<Component> tooltips){}

    /**
     * @deprecated Use {@link #getExtraTooltip(ItemStack, List, Item.TooltipContext, TooltipFlag)}
     */
    @Deprecated(forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "1.22")
    default void getExtraTooltip(ItemStack stack, List<Component> tooltips){}
}