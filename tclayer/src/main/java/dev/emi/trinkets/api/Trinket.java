package dev.emi.trinkets.api;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public interface Trinket {

    /**
     * Called every tick on the client and server side
     *
     * @param stack The stack being ticked
     * @param slot The slot the stack is equipped to
     * @param entity The entity wearing the stack
     */
    default void tick(ItemStack stack, SlotReference slot, LivingEntity entity) {
    }

    /**
     * Called when an entity equips a trinket
     *
     * @param stack The stack being equipped
     * @param slot The slot the stack is equipped to
     * @param entity The entity that equipped the stack
     */
    default void onEquip(ItemStack stack, SlotReference slot, LivingEntity entity) {
    }

    /**
     * Called when an entity equips a trinket
     *
     * @param stack The stack being unequipped
     * @param slot The slot the stack was unequipped from
     * @param entity The entity that unequipped the stack
     */
    default void onUnequip(ItemStack stack, SlotReference slot, LivingEntity entity) {
    }

    /**
     * Determines whether an entity can equip a trinket
     *
     * @param stack The stack being equipped
     * @param slot The slot the stack is being equipped to
     * @param entity The entity that is equipping the stack
     * @return Whether the stack can be equipped
     */
    default boolean canEquip(ItemStack stack, SlotReference slot, LivingEntity entity) {
        return true;
    }

    /**
     * Determines whether an entity can unequip a trinket
     *
     * @param stack The stack being unequipped
     * @param slot The slot the stack is being unequipped from
     * @param entity The entity that is unequipping the stack
     * @return Whether the stack can be unequipped
     */
    default boolean canUnequip(ItemStack stack, SlotReference slot, LivingEntity entity) {
        return !EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE) || (entity instanceof Player player && player.isCreative());
    }

    /**
     * Determines whether a trinket can automatically attempt to equip into the first available
     * slot when used
     *
     * @param stack The stack being equipped
     * @param entity The entity that is using the stack
     * @return Whether the stack can be equipped from use
     */
    default boolean canEquipFromUse(ItemStack stack, LivingEntity entity) {
        return false;
    }

    /**
     * Determines the equip sound of a trinket
     *
     * @param stack The stack for the equip sound
     * @param slot The slot the stack is being equipped to
     * @param entity The entity that is equipping the stack
     * @return The {@link SoundEvent} to play for equipping
     */
    @Nullable
    default Holder<SoundEvent> getEquipSound(ItemStack stack, SlotReference slot, LivingEntity entity) {
        return stack.getItem() instanceof Equipable eq ? eq.getEquipSound() : null;
    }

    /**
     * Returns the Entity Attribute Modifiers for a stack in a slot. Child implementations should
     * remain pure
     * <p>
     * If modifiers do not change based on stack, slot, or entity, caching based on passed UUID
     * should be considered
     *
     * @param location The ResourceLocation to use for creating attributes
     */
    default Multimap<Holder<Attribute>, AttributeModifier> getModifiers(ItemStack stack, SlotReference slot, LivingEntity entity, ResourceLocation location) {
        return Multimaps.newMultimap(Maps.newLinkedHashMap(), ArrayList::new);
    }

    /**
     * Called by Trinkets when a trinket is broken on the client if {@link TrinketsApi#onTrinketBroken}
     * is called by the consumer in  server side
     * <p>
     * The default implementation works the same as breaking vanilla equipment, a sound is played and
     * particles are spawned based on the item
     *
     * @param stack The stack being broken
     * @param slot The slot the stack is being broken in
     * @param entity The entity that is breaking the stack
     */
    default void onBreak(ItemStack stack, SlotReference slot, LivingEntity entity) {
        //((LivingEntityAccessor) entity).callBreakItem(stack);
    }

    default TrinketEnums.DropRule getDropRule(ItemStack stack, SlotReference slot, LivingEntity entity) {
        return TrinketEnums.DropRule.DEFAULT;
    }
}
