package io.wispforest.accessories.api.slot;

import io.wispforest.accessories.api.action.ActionResponseBuffer;
import io.wispforest.accessories.api.core.Accessory;
import io.wispforest.accessories.api.slot.validator.EntitySlotValidator;
import io.wispforest.accessories.api.slot.validator.SlotValidator;
import io.wispforest.accessories.api.slot.validator.SlotValidatorRegistry;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@Deprecated(forRemoval = true)
public class SlotPredicateRegistry {

    public static void register(ResourceLocation location, SlotBasedPredicate predicate) {
        SlotValidatorRegistry.register(location, predicate);
    }

    /**
     * @return {@link SlotBasedPredicate} bound to the given {@link ResourceLocation} or an Empty {@link Optional} if absent
     */
    @Nullable
    public static SlotBasedPredicate getPredicate(ResourceLocation location) {
        var validator = SlotValidatorRegistry.getPredicate(location);

        if (validator == null) return null;
        if (validator instanceof SlotBasedPredicate predicate) return predicate;

        return (validator instanceof EntitySlotValidator entityValidator)
            ? new EntitySlotPredicateWrapper(entityValidator)
            : new SlotPredicateWrapper(validator);
    }

    private record SlotPredicateWrapper(SlotValidator validator) implements SlotBasedPredicate {
        @Override
        public TriState isValid(Level level, SlotType slotType, int index, ItemStack stack) {
            var buffer = new ActionResponseBuffer(true);

            validator.isValidForSlot(level, slotType, index, stack, buffer);

            return buffer.canPerformAction().toTriState();
        }
    }

    private record EntitySlotPredicateWrapper(EntitySlotValidator validator) implements EntityBasedPredicate {
        @Override
        public TriState isValid(Level level, @Nullable LivingEntity entity, SlotType slotType, int index, ItemStack stack) {
            var buffer = new ActionResponseBuffer(true);

            validator.isValidForSlot(entity, level, slotType, index, stack, buffer);

            return buffer.canPerformAction().toTriState();
        }
    }

    //--

    /**
     * Used to check if the given {@link ItemStack} is valid for the given LivingEntity and SlotReference
     * based on {@link SlotBasedPredicate}s bound to the Slot and the {@link Accessory} bound to the stack if present
     */
    public static boolean canInsertIntoSlot(ItemStack stack, SlotReference reference){
        return SlotValidatorRegistry.canInsertIntoSlot(stack, reference);
    }

    /**
     * @return All valid {@link SlotType}s for the given {@link ItemStack} based on the {@link LivingEntity}
     * available {@link SlotType}s
     */
    public static Collection<SlotType> getValidSlotTypes(LivingEntity entity, ItemStack stack){
        return SlotValidatorRegistry.getValidSlotTypes(entity, stack);
    }

    public static boolean isValidAccessory(ItemStack stack, Level level){
        return isValidAccessory(stack, level, null);
    }

    /**
     * @return If a given {@link ItemStack} is found either to have an {@link Accessory} besides the
     * default or if the given stack has valid slots which it can be equipped
     */
    public static boolean isValidAccessory(ItemStack stack, Level level, @Nullable LivingEntity entity){
        return SlotValidatorRegistry.isValidAccessory(stack, level, entity);
    }

    public static Collection<SlotType> getStackSlotTypes(Level level, ItemStack stack){
        return getStackSlotTypes(level, null, stack);
    }

    public static Collection<SlotType> getStackSlotTypes(LivingEntity entity, ItemStack stack) {
        return getStackSlotTypes(entity.level(), entity, stack);
    }

    public static Collection<SlotType> getStackSlotTypes(Level level, @Nullable LivingEntity entity, ItemStack stack) {
       return SlotValidatorRegistry.getStackSlotTypes(level, entity, stack);
    }

    public static boolean getPredicateResults(Set<ResourceLocation> predicateIds, Level level, SlotType slotType, int index, ItemStack stack){
        return getPredicateResults(predicateIds, level, null, slotType, index, stack);
    }

    public static boolean getPredicateResults(Set<ResourceLocation> predicateIds, Level level, @Nullable LivingEntity entity, SlotType slotType, int index, ItemStack stack){
        return SlotValidatorRegistry.getPredicateResults(predicateIds, level, entity, slotType, index, stack);
    }
}
