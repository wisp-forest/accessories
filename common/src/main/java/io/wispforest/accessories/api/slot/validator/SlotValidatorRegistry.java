package io.wispforest.accessories.api.slot.validator;

import com.mojang.logging.LogUtils;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.action.*;
import io.wispforest.accessories.api.components.AccessoriesDataComponents;
import io.wispforest.accessories.api.components.AccessorySlotValidationComponent;
import io.wispforest.accessories.api.core.Accessory;
import io.wispforest.accessories.api.core.AccessoryRegistry;
import io.wispforest.accessories.api.data.AccessoriesBaseData;
import io.wispforest.accessories.api.data.AccessoriesTags;
import io.wispforest.accessories.api.slot.*;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.accessories.impl.AccessoryAttributeLogic;
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

public class SlotValidatorRegistry {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<ResourceLocation, SlotValidator> PREDICATES = new HashMap<>();

    public static void register(ResourceLocation location, SlotValidator predicate) {
        if(PREDICATES.containsKey(location)) {
            LOGGER.warn("[AccessoriesAPI]: A SlotValidator attempted to be registered but a duplicate entry existed already! [Id: {}]", location);

            return;
        }

        PREDICATES.put(location, predicate);
    }

    /**
     * @return {@link SlotValidator} bound to the given {@link ResourceLocation} or an Empty {@link Optional} if absent
     */
    @Nullable
    public static SlotValidator getPredicate(ResourceLocation location) {
        return PREDICATES.get(location);
    }

    //--

    /**
     * Used to check if the given {@link ItemStack} is valid for the given LivingEntity and SlotReference
     * based on {@link SlotValidator}s bound to the Slot and the {@link Accessory} bound to the stack if present
     */
    public static boolean canInsertIntoSlot(ItemStack stack, SlotReference reference){
        var slotType = reference.type();

        if(slotType == null) {
            throw new IllegalStateException("Unable to get the needed SlotType from the SlotReference passed within `canInsertIntoSlot`! [Name: " + reference.slotName() + "]");
        }

        return getPredicateResults(slotType.validators(), reference.entity().level(), reference.entity(), slotType, 0, stack) && AccessoryRegistry.canEquip(stack, reference);
    }

    public static ActionResponseBuffer canInsertIntoSlotResponse(ItemStack stack, SlotReference reference, ActionResponseBuffer buffer){
        var slotType = reference.type();

        if(slotType == null) {
            throw new IllegalStateException("Unable to get the needed SlotType from the SlotReference passed within `canInsertIntoSlot`! [Name: " + reference.slotName() + "]");
        }

        getPredicateResponse(slotType.validators(), reference.entity().level(), reference.entity(), slotType, 0, stack, buffer);

        AccessoryRegistry.canEquipResponse(stack, reference, buffer);

        return buffer;
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

        for (SlotType value : SlotTypeLoader.INSTANCE.getEntries(level).values()) {
            if(getPredicateResults(value.validators(), level, entity, value, 0, stack)) validSlots.add(value);
        }

        return validSlots;
    }

    public static boolean getPredicateResults(Set<ResourceLocation> predicateIds, Level level, SlotType slotType, int index, ItemStack stack){
        return getPredicateResults(predicateIds, level, null, slotType, index, stack);
    }

    public static boolean getPredicateResults(Set<ResourceLocation> predicateIds, Level level, @Nullable LivingEntity entity, SlotType slotType, int index, ItemStack stack){
        return getPredicateResponse(predicateIds, level, entity, slotType, index, stack, new ActionResponseBuffer(true))
            .canPerformAction()
            .isValid(false);
    }

    public static ActionResponseBuffer getPredicateResponse(Set<ResourceLocation> predicateIds, Level level, @Nullable LivingEntity entity, SlotType slotType, int index, ItemStack stack, ActionResponseBuffer buffer){
        for (var predicateId : predicateIds) {
            var predicate = getPredicate(predicateId);

            if(predicate == null) continue;

            if(predicate instanceof EntitySlotValidator entityBasedPredicate) {
                entityBasedPredicate.isValidForSlot(entity, level, slotType, index, stack, buffer);
            } else {
                predicate.isValidForSlot(level, slotType, index, stack, buffer);
            }

            if(buffer.canPerformAction() != ValidationState.IRRELEVANT) break;
        }

        return buffer;
    }

    private static TagKey<Item> getSlotTag(SlotType slotType) {
        var location = UniqueSlotHandling.isUniqueSlot(slotType.name()) ? ResourceLocation.parse(slotType.name()) : Accessories.of(slotType.name());

        return TagKey.create(Registries.ITEM, location);
    }

    static {
        register(AccessoriesBaseData.ALL_PREDICATE_ID, (level, slotType, i, stack, buffer) -> {
            buffer.respondWith(SlotValidatorReasons.ALWAYS_VALID);
        });
        register(AccessoriesBaseData.NONE_PREDICATE_ID, (level, slotType, i, stack, buffer) -> {
            buffer.respondWith(SlotValidatorReasons.ALWAYS_INVALID);
        });
        register(AccessoriesBaseData.TAG_PREDICATE_ID, (level, slotType, i, stack, buffer) -> {
            buffer.respondWith(new TagValidationResponse<>(stack.getItemHolder(), List.of(getSlotTag(slotType), AccessoriesTags.ANY_TAG), TagValidationResponse.ANY_MATCH));
        });
        register(AccessoriesBaseData.ATTRIBUTE_PREDICATE_ID, SlotValidator.withEntity((entity, level, slotType, index, stack, buffer) -> {
            var bl = !AccessoryAttributeLogic.getAttributeModifiers(stack, entity, slotType.name(), index)
                .getAttributeModifiers(false, true)
                .isEmpty();

            buffer.respondWith(ActionResponse.of(ValidationState.ofOrIrrelevant(bl), (callback, ctx, type) -> {
                var infoType = (bl ? "include" : "excludes");


                if (type.isAdvanced() || type.hasShiftDown()) {
                    callback.add(Accessories.translation("tooltip.validator.attribute.simple", infoType));
                }
            }));
        }));
        register(AccessoriesBaseData.COMPONENT_PREDICATE_ID, (level, slotType, index, stack, buffer) -> {
            var slotValidationData = stack.getOrDefault(AccessoriesDataComponents.SLOT_VALIDATION, AccessorySlotValidationComponent.EMPTY);

            if (slotValidationData.isEmpty()) return;

            buffer.respondWith(new SlotValidationResponse(slotType.name(), slotValidationData.validSlotOverrides(), slotValidationData.invalidSlotOverrides()));
        });
    }
}
