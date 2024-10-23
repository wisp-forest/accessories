package io.wispforest.accessories.api.slot;

import com.mojang.logging.LogUtils;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.Accessory;
import io.wispforest.accessories.api.AccessoryRegistry;
import io.wispforest.accessories.api.components.AccessoriesDataComponents;
import io.wispforest.accessories.api.data.AccessoriesBaseData;
import io.wispforest.accessories.api.data.AccessoriesTags;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.accessories.impl.AccessoryAttributeLogic;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;

public class SlotPredicateRegistry {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<ResourceLocation, SlotBasedPredicate> PREDICATES = new HashMap<>();

    public static void registerPredicate(ResourceLocation location, SlotBasedPredicate predicate) {
        if(PREDICATES.containsKey(location)) {
            LOGGER.warn("[AccessoriesAPI]: A SlotBasedPredicate attempted to be registered but a duplicate entry existed already! [Id: {}]", location);

            return;
        }

        PREDICATES.put(location, predicate);
    }

    /**
     * @return {@link SlotBasedPredicate} bound to the given {@link ResourceLocation} or an Empty {@link Optional} if absent
     */
    @Nullable
    public static SlotBasedPredicate getPredicate(ResourceLocation location) {
        return PREDICATES.get(location);
    }

    //--

    /**
     * Used to check if the given {@link ItemStack} is valid for the given LivingEntity and SlotReference
     * based on {@link SlotBasedPredicate}s bound to the Slot and the {@link Accessory} bound to the stack if present
     */
    public static boolean canInsertIntoSlot(ItemStack stack, SlotReference reference){
        var slotType = reference.type();

        if(slotType == null) {
            throw new IllegalStateException("Unable to get the needed SlotType from the SlotReference passed within `canInsertIntoSlot`! [Name: " + reference.slotName() + "]");
        }

        return getPredicateResults(slotType.validators(), reference.entity().level(), reference.entity(), slotType, 0, stack) && AccessoryRegistry.canEquip(stack, reference);
    }

    /**
     * @return All valid {@link SlotType}s for the given {@link ItemStack} based on the {@link LivingEntity}
     * available {@link SlotType}s
     */
    public static Collection<SlotType> getValidSlotTypes(LivingEntity entity, ItemStack stack){
        var slots = EntitySlotLoader.getEntitySlots(entity);

        var validSlots = new ArrayList<SlotType>();

        var capability = AccessoriesCapability.get(entity);

        if(capability != null) {
            var containers = capability.getContainers();

            for (SlotType value : slots.values()) {
                if (!containers.containsKey(value.name())) continue;

                var container = containers.get(value.name());

                var size = containers.get(value.name()).getSize();

                if(size == 0) size = 1;

                for (int i = 0; i < size; i++) {
                    var reference = SlotReference.of(entity, container.getSlotName(), i);

                    if (canInsertIntoSlot(stack, reference)) {
                        validSlots.add(value);

                        break;
                    }
                }
            }
        }

        return validSlots;
    }

    public static boolean isValidAccessory(ItemStack stack, Level level){
        return isValidAccessory(stack, level, null);
    }

    /**
     * @return If a given {@link ItemStack} is found either to have an {@link Accessory} besides the
     * default or if the given stack has valid slots which it can be equipped
     */
    public static boolean isValidAccessory(ItemStack stack, Level level, @Nullable LivingEntity entity){
        return !AccessoryRegistry.isDefaultAccessory(AccessoryRegistry.getAccessoryOrDefault(stack))
                || !getStackSlotTypes(level, entity, stack).isEmpty();
    }

    public static Collection<SlotType> getStackSlotTypes(Level level, ItemStack stack){
        return getStackSlotTypes(level, null, stack);
    }

    public static Collection<SlotType> getStackSlotTypes(LivingEntity entity, ItemStack stack) {
        return getStackSlotTypes(entity.level(), entity, stack);
    }

    public static Collection<SlotType> getStackSlotTypes(Level level, @Nullable LivingEntity entity, ItemStack stack) {
        var validSlots = new ArrayList<SlotType>();

        for (SlotType value : SlotTypeLoader.getSlotTypes(level).values()) {
            if(getPredicateResults(value.validators(), level, entity, value, 0, stack)) validSlots.add(value);
        }

        return validSlots;
    }

    public static boolean getPredicateResults(Set<ResourceLocation> predicateIds, Level level, SlotType slotType, int index, ItemStack stack){
        return getPredicateResults(predicateIds, level, null, slotType, index, stack);
    }

    public static boolean getPredicateResults(Set<ResourceLocation> predicateIds, Level level, @Nullable LivingEntity entity, SlotType slotType, int index, ItemStack stack){
        var result = TriState.DEFAULT;

        for (var predicateId : predicateIds) {
            var predicate = getPredicate(predicateId);

            if(predicate == null) continue;

            if(predicate instanceof EntityBasedPredicate entityBasedPredicate) {
                result = entityBasedPredicate.isValid(level, entity, slotType, index, stack);
            } else {
                result = predicate.isValid(level, slotType, index, stack);
            }

            if(result != TriState.DEFAULT) break;
        }

        return result.orElse(false);
    }

    private static TagKey<Item> getSlotTag(SlotType slotType) {
        var location = UniqueSlotHandling.isUniqueSlot(slotType.name()) ? ResourceLocation.parse(slotType.name()) : Accessories.of(slotType.name());

        return TagKey.create(Registries.ITEM, location);
    }

    static {
        registerPredicate(AccessoriesBaseData.ALL_PREDICATE_ID, (level, slotType, i, stack) -> TriState.TRUE);
        registerPredicate(AccessoriesBaseData.NONE_PREDICATE_ID, (level, slotType, i, stack) -> TriState.FALSE);
        registerPredicate(AccessoriesBaseData.TAG_PREDICATE_ID, (level, slotType, i, stack) -> {
            return (stack.is(getSlotTag(slotType)) || stack.is(AccessoriesTags.ANY_TAG)) ? TriState.TRUE : TriState.DEFAULT;
        });
        registerPredicate(AccessoriesBaseData.RELEVANT_PREDICATE_ID, (level, slotType, i, stack) -> {
            var bl = !AccessoryAttributeLogic.getAttributeModifiers(stack, null, slotType.name(), i).getAttributeModifiers(false).isEmpty();

            return bl ? TriState.TRUE : TriState.DEFAULT;
        });
        registerPredicate(AccessoriesBaseData.COMPONENT_PREDICATE_ID, (level, slotType, index, stack) -> {
            if(stack.has(AccessoriesDataComponents.SLOT_VALIDATION)) {
                var slotValidationData = stack.get(AccessoriesDataComponents.SLOT_VALIDATION);
                var name = slotType.name();

                //--

                var invalidSlots = slotValidationData.invalidSlotOverrides();

                for (var invalidSlot : invalidSlots) {
                    if (name.equals(invalidSlot)) return TriState.FALSE;
                }

                //--

                var validSlots = slotValidationData.validSlotOverrides();

                for (var validSlot : validSlots) {
                    if (validSlot.equals("any")) return TriState.TRUE;

                    if (name.equals(validSlot)) return TriState.TRUE;
                }
            }

            return TriState.DEFAULT;
        });
    }
}
