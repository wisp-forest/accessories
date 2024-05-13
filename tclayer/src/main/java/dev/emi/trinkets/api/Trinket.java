package dev.emi.trinkets.api;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

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
        return !EnchantmentHelper.hasBindingCurse(stack) || (entity instanceof Player player && player.isCreative());
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
    default SoundEvent getEquipSound(ItemStack stack, SlotReference slot, LivingEntity entity) {
        return stack.getItem() instanceof Equipable eq ? eq.getEquipSound() : null;
    }

    /**
     * Returns the Entity Attribute Modifiers for a stack in a slot. Child implementations should
     * remain pure
     * <p>
     * If modifiers do not change based on stack, slot, or entity, caching based on passed UUID
     * should be considered
     *
     * @param uuid The UUID to use for creating attributes
     */
    default Multimap<Attribute, AttributeModifier> getModifiers(ItemStack stack,
                                                                SlotReference slot, LivingEntity entity, UUID uuid) {

        Multimap<Attribute, AttributeModifier> map = Multimaps.newMultimap(Maps.newLinkedHashMap(), ArrayList::new);

        if (stack.hasTag() && stack.getTag().contains("TrinketAttributeModifiers", 9)) {
            ListTag list = stack.getTag().getList("TrinketAttributeModifiers", 10);

            for (int i = 0; i < list.size(); i++) {
                CompoundTag tag = list.getCompound(i);

                if (!tag.contains("Slot", NbtType.STRING) || tag.getString("Slot")
                        .equals(slot.inventory().getSlotType().getName())) {
                    Optional<Attribute> optional = BuiltInRegistries.ATTRIBUTE
                            .getOptional(ResourceLocation.tryParse(tag.getString("AttributeName")));

                    if (optional.isPresent()) {
                        AttributeModifier entityAttributeModifier = AttributeModifier.load(tag);

                        if (entityAttributeModifier != null
                                && entityAttributeModifier.getId().getLeastSignificantBits() != 0L
                                && entityAttributeModifier.getId().getMostSignificantBits() != 0L) {
                            map.put(optional.get(), entityAttributeModifier);
                        }
                    }
                }
            }
        }
        return map;
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
