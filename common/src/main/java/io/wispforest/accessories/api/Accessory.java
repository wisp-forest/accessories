package io.wispforest.accessories.api;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

public interface Accessory {

    default void tick(ItemStack stack, SlotReference reference){}

    default void onEquip(ItemStack stack, SlotReference reference){}

    default void onUnequip(ItemStack stack, SlotReference reference){}

    default boolean canEquip(ItemStack stack, SlotReference reference){
        // TODO: IMPLEMENT???
        return true;
    }

    default boolean canUnequip(ItemStack stack, SlotReference reference){
        // TODO: CHECK FOR CURSE OR SOMETHING?
        return true;
    }

    // TODO: Find places for which such should and should not be called
    default void onBreak(ItemStack stack, SlotReference reference){}

    default Multimap<Attribute, AttributeModifier> getModifiers(ItemStack stack, SlotReference reference, UUID uuid){
        return HashMultimap.create();
    }

    default SlotType.DropRule getDropRule(ItemStack stack, SlotReference reference){
        return SlotType.DropRule.DEFAULT;
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
