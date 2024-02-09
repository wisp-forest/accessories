package io.wispforest.accessories.api;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.logging.LogUtils;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.AccessoriesAccess;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.data.SlotTypeLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

import java.util.*;

/**
 * Base implementation of API
 */
public class AccessoriesAPI {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final Accessory DEFAULT = new Accessory() {};

    private static final Map<String, UUID> CACHED_UUIDS = new HashMap<>();

    private static final Map<ResourceLocation, SlotBasedPredicate> PREDICATE_REGISTRY = new HashMap<>();

    private static final Map<Item, Accessory> REGISTER = new HashMap<>();

    //--

    /**
     * @return The Capability Bound to the given living entity if such is present
     */
    public static Optional<AccessoriesCapability> getCapability(LivingEntity livingEntity){
        return AccessoriesAccess.getCapability(livingEntity);
    }

    /**
     * Main method to register a given {@link Item} to given {@link Accessory}
     */
    public static void registerAccessory(Item item, Accessory accessory) {
        REGISTER.put(item, accessory);
    }

    /**
     * Attempt to get a {@link Accessory} bound to an {@link Item} or an Empty {@link Optional}
     */
    public static Optional<Accessory> getAccessory(Item item) {
        return Optional.ofNullable(REGISTER.get(item));
    }

    /**
     * Attempt to get a {@link Accessory} bound to an {@link ItemStack}'s Item or an Empty {@link Optional}
     */
    public static Optional<Accessory> getAccessory(ItemStack stack){
        return getAccessory(stack.getItem());
    }

    /**
     * Get any bound {@link Accessory} to the given {@link ItemStack}'s Item or return {@link #DEFAULT} Accessory
     */
    public static Accessory getOrDefaultAccessory(ItemStack stack){
        return getOrDefaultAccessory(stack.getItem());
    }

    /**
     * Get any bound {@link Accessory} to the given {@link Item} or return {@link #DEFAULT} Accessory
     */
    public static Accessory getOrDefaultAccessory(Item item){
        return getAccessory(item).orElse(defaultAccessory());
    }

    /**
     * @return Default {@link Accessory}
     */
    public static Accessory defaultAccessory(){
        return DEFAULT;
    }

    //--

    public static Multimap<Attribute, AttributeModifier> getAttributeModifiers(ItemStack stack, SlotReference reference, UUID uuid){
        Multimap<Attribute, AttributeModifier> multimap = HashMultimap.create();

        if(stack.getTag() != null && stack.getTag().contains("AccessoriesAttributeModifiers", Tag.TAG_LIST)){
            var attributes = stack.getTag().getList("AccessoriesAttributeModifiers", Tag.TAG_COMPOUND);

            for (int i = 0; i < attributes.size(); i++) {
                var attributeTag = attributes.getCompound(i);

                if(attributeTag.contains("Slot") && !attributeTag.getString("Slot").equals(reference.slotName())){
                    continue;
                }

                var attributeType = ResourceLocation.tryParse(attributeTag.getString("AttributeName"));
                var id = uuid;

                if(attributeType == null) continue;

                if(attributeTag.contains("UUID")) id = attributeTag.getUUID("UUID");

                if(id.getLeastSignificantBits() == 0 || id.getMostSignificantBits() == 0) continue;

                var operation = AttributeModifier.Operation.fromValue(attributeTag.getInt("Operation"));
                var amount = attributeTag.getDouble("Amount");
                var name = attributeTag.getString("Name");

                var attributeModifier = new AttributeModifier(id, name, amount, operation);

                if(attributeType.getNamespace().equals(Accessories.MODID)){
                    var slotName = attributeType.getPath();

                    if(!AccessoriesAPI.getAllSlots(reference.entity().level()).containsKey(slotName)) continue;

                    multimap.put(SlotAttribute.getSlotAttribute(slotName), attributeModifier);
                } else {
                    var attribute = BuiltInRegistries.ATTRIBUTE.getOptional(attributeType);

                    if(attribute.isEmpty()) continue;

                    multimap.put(attribute.get(), attributeModifier);
                }
            }
        }

        //TODO: Decide if such presents of modifiers prevents the accessory modifiers from existing
        AccessoriesAPI.getAccessory(stack).ifPresent(accessory -> {
            var data = accessory.getModifiers(stack, reference, uuid);

            multimap.putAll(data);
        });

        return multimap;
    }

    /**
     * @return {@link UUID} based on the provided {@link SlotType#name} and entry index
     */
    public static UUID getOrCreateSlotUUID(SlotType slotType, int index) {
        return getOrCreateSlotUUID(slotType.name(), index);
    }

    /**
     * @return {@link UUID} based on the provided slot name and entry index
     */
    public static UUID getOrCreateSlotUUID(String slotName, int index) {
        return CACHED_UUIDS.computeIfAbsent(
                slotName + "/" + index,
                s -> UUID.nameUUIDFromBytes(s.getBytes())
        );
    }

    //--

    /**
     * @return The valid {@link SlotType}'s for given {@link LivingEntity} based on its {@link EntityType}
     */
    public static Map<String, SlotType> getEntitySlots(LivingEntity livingEntity){
        return getEntitySlots(livingEntity.level(), livingEntity.getType());
    }

    /**
     * @return The valid {@link SlotType}'s for given {@link EntityType}
     */
    public static Map<String, SlotType> getEntitySlots(Level level, EntityType<?> entityType){
        var map = EntitySlotLoader.INSTANCE.getSlotTypes(level.isClientSide, entityType);

        return map != null ? map : Map.of();
    }

    /**
     * Attempt to get the given SlotType based on the provided slotName
     */
    public static Optional<SlotType> getSlotType(Level level, String slotName){
        return Optional.ofNullable(getAllSlots(level).get(slotName));
    }

    /**
     * Get all SlotTypes registered
     */
    public static Map<String, SlotType> getAllSlots(Level level){
        return SlotTypeLoader.INSTANCE.getSlotTypes(level);
    }

    //--

    /**
     * Used to check if the given {@link ItemStack} is valid for the given LivingEntity and SlotReference
     * based on {@link SlotBasedPredicate}s bound to the Slot and the {@link Accessory} bound to the stack if present
     */
    public static boolean canInsertIntoSlot(LivingEntity entity, SlotReference reference, ItemStack stack){
        var predicates = reference.type().map(SlotType::validators).orElse(Set.of());

        if(getPredicateResults(predicates, entity, reference, stack)){
            var accessory = getOrDefaultAccessory(stack);

            return accessory.canEquip(stack, reference);
        }

        return false;
    }

    /**
     * @return All valid {@link SlotType}s for the given {@link ItemStack} based on the {@link LivingEntity}
     * available {@link SlotType}s
     */
    public static Collection<SlotType> getValidSlotTypes(LivingEntity entity, ItemStack stack){
        var slots = getEntitySlots(entity);

        var validSlots = new ArrayList<SlotType>();

        var capability = getCapability(entity);

        if(capability.isPresent()) {
            var containers = capability.get().getContainers();

            for (SlotType value : slots.values()) {
                if (!containers.containsKey(value.name())) continue;

                var container = containers.get(value.name());

                for (var accessory : containers.get(value.name()).getAccessories()) {
                    var reference = new SlotReference(container.getSlotName(), entity, accessory.getFirst());

                    if (canInsertIntoSlot(entity, reference, stack)) validSlots.add(value);
                }
            }
        }

        return validSlots;
    }

    //--

    /**
     * @return {@link SlotBasedPredicate} bound to the given {@link ResourceLocation} or an Empty {@link Optional} if absent
     */
    public static Optional<SlotBasedPredicate> getPredicate(ResourceLocation location) {
        return Optional.ofNullable(PREDICATE_REGISTRY.get(location));
    }

    public static void registerPredicate(ResourceLocation location, SlotBasedPredicate predicate) {
        if(PREDICATE_REGISTRY.containsKey(location)) {
            LOGGER.warn("[AccessoriesAPI]: A SlotBasedPredicate attempted to be registered but a duplicate entry existed already! [Id: " + location + "]");

            return;
        }

        PREDICATE_REGISTRY.put(location, predicate);
    }

    public static boolean getPredicateResults(Set<ResourceLocation> predicateIds, LivingEntity entity, SlotReference reference, ItemStack stack){
        InteractionResult result = InteractionResult.PASS;

        for (var predicateId : predicateIds) {
            var predicate = getPredicate(predicateId);

            if(predicate.isEmpty()) continue;

            result = predicate.get().isValid(entity, reference, stack);

            if(result != InteractionResult.PASS) break;
        }

        return result.consumesAction();
    }

    /**
     * TagKey in which allows for a given Item to pass {@link SlotBasedPredicate} allowing such to be equipped if
     * desired
     */
    public static final TagKey<Item> ALL_ACCESSORIES = TagKey.create(Registries.ITEM, Accessories.of("all"));

    public static final String ACCESSORY_PREDICATES_KEY = "AccessoryPredicates";

    public static final String ACCESSORY_VALID_SLOTS_KEY = "ValidSlotOverrides";
    public static final String ACCESSORY_INVALID_SLOTS_KEY = "InvalidSlotOverrides";

    static {
        registerPredicate(Accessories.of("all"), (entity, reference, stack) -> InteractionResult.SUCCESS);
        registerPredicate(Accessories.of("none"), (entity, reference, stack) -> InteractionResult.FAIL);
        registerPredicate(Accessories.of("tag"), (entity, reference, stack) -> {
            var tag = TagKey.create(Registries.ITEM, Accessories.of(reference.slotName()));

            return (stack.is(tag) || stack.is(ALL_ACCESSORIES)) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        });
        registerPredicate(Accessories.of("relevant"), (entity, reference, stack) -> {
            var bl = !getAttributeModifiers(stack, reference, getOrCreateSlotUUID(reference.slotName(), reference.slot())).isEmpty();

            return bl ? InteractionResult.SUCCESS : InteractionResult.PASS;
        });
        registerPredicate(Accessories.of("compound"), (entity, reference, stack) -> {
            var tag = stack.getTag();

            if(tag != null && tag.contains(ACCESSORY_PREDICATES_KEY)) {
                var extraData = tag.getCompound(ACCESSORY_PREDICATES_KEY);

                var invalidSlots = extraData.getList(ACCESSORY_INVALID_SLOTS_KEY, Tag.TAG_STRING);

                for (int i = 0; i < invalidSlots.size(); i++) {
                    var invalidSlot = invalidSlots.getString(i);

                    if (reference.slotName().equals(invalidSlot)) return InteractionResult.FAIL;
                }

                //--

                var validSlots = extraData.getList(ACCESSORY_VALID_SLOTS_KEY, Tag.TAG_STRING);

                for (int i = 0; i < validSlots.size(); i++) {
                    var validSlot = validSlots.getString(i);

                    if (reference.slotName().equals(validSlot)) return InteractionResult.SUCCESS;
                }
            }

            return InteractionResult.PASS;
        });
    }
}
