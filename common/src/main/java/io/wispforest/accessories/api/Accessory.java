package io.wispforest.accessories.api;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.impl.AccessoriesEventHandler;
import io.wispforest.accessories.mixin.LivingEntityAccessor;
import io.wispforest.accessories.networking.client.AccessoryBreak;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Main interface for implementing an accessory
 */
public interface Accessory {

    /**
     * Called every tick on every tick of the linked {@link LivingEntity} on both the Client and Server
     *
     * @param stack The Stack being ticked
     * @param reference The reference to the targeted {@link LivingEntity}, slot and index
     */
    default void tick(ItemStack stack, SlotReference reference){}

    /**
     * Called on Equip of the given Stack
     *
     * @param stack The Stack being equipped
     * @param reference The reference to the targeted {@link LivingEntity}, slot and index
     */
    default void onEquip(ItemStack stack, SlotReference reference){}

    /**
     * Called on Unequipped of the given Stack
     *
     * @param stack The Stack being unequipped
     * @param reference The reference to the targeted {@link LivingEntity}, slot and index
     */
    default void onUnequip(ItemStack stack, SlotReference reference){}

    /**
     * Returns whether the following stack can be equipped
     *
     * @param stack The Stack attempting to be equipped
     * @param reference The reference to the targeted {@link LivingEntity}, slot and index
     */
    default boolean canEquip(ItemStack stack, SlotReference reference){
        return true;
    }

    /**
     * Returns whether the following stack can be unequipped
     *
     * @param stack The Stack attempting to be unequipped
     * @param reference The reference to the targeted {@link LivingEntity}, slot and index
     */
    default boolean canUnequip(ItemStack stack, SlotReference reference){
        if(EnchantmentHelper.hasBindingCurse(stack)) {
            return reference.entity() instanceof Player player && player.isCreative();
        }

        return true;
    }

    /**
     * Returns the Attribute Modifiers for the following stack within the given reference
     *
     * @param stack The Stack attempting to be unequipped
     * @param reference The reference to the targeted {@link LivingEntity}, slot and index
     * @param uuid The UUID used for creating Modifiers
     */
    default Multimap<Holder<Attribute>, AttributeModifier> getModifiers(ItemStack stack, SlotReference reference, UUID uuid){
        return HashMultimap.create();
    }

    /**
     * Returns the following drop rule for the given Item
     *
     * @param stack The Stack being prepared for dropping
     * @param reference The reference to the targeted {@link LivingEntity}, slot and index
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
     * called on the server and such {@link AccessoryBreak} packet is received
     *
     * @param stack The Stack being prepared for dropping
     * @param reference The reference to the targeted {@link LivingEntity}, slot and index
     */
    default void onBreak(ItemStack stack, SlotReference reference) {
        ((LivingEntityAccessor) reference.entity()).breakItem(stack);
    }

    /**
     * Method used to add tooltip info for attribute like data based on a given slot type
     *
     * @param stack The Stack being referenced
     * @param type The SlotType being referenced
     * @param tooltips Final list containing the tooltip info
     */
    default void getAttributesTooltip(ItemStack stack, SlotType type, List<Component> tooltips){}

    /**
     * Method used to add any additional tooltip information to a given {@link Accessory} tooltip
     * within {@link AccessoriesEventHandler#addTooltipInfo(LivingEntity, ItemStack, List)} at the
     * end of the method call.
     *
     * <p>
     *     Do note that means that the list passed contains all tooltip info allowing for
     *     positioning before or after the tooltip info
     * </p>
     *
     * @param stack The Stack being referenced
     * @param tooltips Final list containing the tooltip info
     */
    default void getExtraTooltip(ItemStack stack, List<Component> tooltips){}

    /**
     * @return Return the max stack amount allowed when equipping a given stack into an accessories inventory
     */
    default int maxStackSize(ItemStack stack){
        return stack.getMaxStackSize();
    }
}