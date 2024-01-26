package io.wispforest.accessories.api;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
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
        return !EnchantmentHelper.hasBindingCurse(stack);
    }

    // TODO: Find places for which such should and should not be called
    default void onBreak(ItemStack stack, SlotReference reference){}

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
    default DropRule getDropRule(ItemStack stack, SlotReference reference){
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
        // TODO: Should this be defaulting to false?
        return false;
    }

    default List<Component> getExtraTooltip(ItemStack stack, List<Component> tooltips){
        return tooltips;
    }

    default List<Component> getAttributesTooltip(ItemStack stack, List<Component> tooltips){
        return tooltips;
    }

    //--

    //TODO: Figure out if such should be implemented or not. Is required for curios layer but unknown if the mod should handle such???

    default int getFortuneLevel() { return 0; }

    default int getLootingLevel() { return 0; }

    default boolean makesPiglinsNeutral() { return false; }

    default boolean canWalkOnPowderedSnow() { return false; }

    default boolean isEnderMask() { return false; }

    //--





}
