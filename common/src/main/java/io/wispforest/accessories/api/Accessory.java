package io.wispforest.accessories.api;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.impl.AccessoriesEventHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.List;
import java.util.UUID;

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
    default Multimap<Attribute, AttributeModifier> getModifiers(ItemStack stack, SlotReference reference, UUID uuid){
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

    default void onEquipFromUse(ItemStack stack, SlotReference reference){
        var sound = getEquipSound(stack, reference);

        reference.entity().playSound(sound.event(), sound.volume(), sound.pitch());
    }

    default SoundEventData getEquipSound(ItemStack stack, SlotReference reference){
        return new SoundEventData(SoundEvents.ARMOR_EQUIP_GENERIC, 1.0f, 1.0f);
    }

    default boolean canEquipFromUse(ItemStack stack, SlotReference reference){
        return true;
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

    default int maxStackSize(ItemStack stack){
        return stack.getMaxStackSize();
    }
}