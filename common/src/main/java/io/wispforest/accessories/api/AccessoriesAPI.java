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

public abstract class AccessoriesAPI {

    protected static final Logger LOGGER = LogUtils.getLogger();

    public static final Accessory DEFAULT = new Accessory() {};

    private static final Map<String, UUID> CACHED_UUIDS = new HashMap<>();

    private final Map<ResourceLocation, SlotBasedPredicate> PREDICATE_REGISTRY = new HashMap<>();

    protected AccessoriesAPI(){
        initDefaultPredicates();
    }

    //--

    public abstract Optional<AccessoriesCapability> getCapability(LivingEntity livingEntity);

    public abstract void registerAccessory(Item item, Accessory accessory);

    public Optional<Accessory> getAccessory(ItemStack stack){
        return getAccessory(stack.getItem());
    }

    public abstract Optional<Accessory> getAccessory(Item item);

    public Accessory getOrDefaultAccessory(ItemStack stack){
        return getOrDefaultAccessory(stack.getItem());
    }

    public Accessory getOrDefaultAccessory(Item item){
        return getAccessory(item).orElse(defaultAccessory());
    }

    public Accessory defaultAccessory(){
        return DEFAULT;
    }

    //--

    public static Multimap<Attribute, AttributeModifier> getAttributeModifiers(ItemStack stack, SlotReference reference, UUID uuid){
        Multimap<Attribute, AttributeModifier> multimap = HashMultimap.create();
        var api = AccessoriesAccess.getAPI();

        if(stack.getTag() != null && stack.getTag().contains("AccessoriesAttributeModifiers", Tag.TAG_LIST)){
            var attributes = stack.getTag().getList("AccessoriesAttributeModifiers", Tag.TAG_COMPOUND);

            for (int i = 0; i < attributes.size(); i++) {
                var attributeTag = attributes.getCompound(i);

                if(attributeTag.contains("slot") && attributeTag.getString("slot").equals(reference.slotName())){
                    var attributeType = ResourceLocation.tryParse(attributeTag.getString("AttributeName"));
                    var id = uuid;

                    if(attributeType != null) continue;

                    if(attributeTag.contains("UUID")) id = attributeTag.getUUID("UUID");

                    if(id.getLeastSignificantBits() == 0 || id.getMostSignificantBits() == 0) continue;

                    var operation = AttributeModifier.Operation.fromValue(attributeTag.getInt("Operation"));
                    var amount = attributeTag.getDouble("Amount");
                    var name = attributeTag.getString("Name");

                    var attributeModifier = new AttributeModifier(id, name, amount, operation);

                    if(attributeType.getNamespace().equals(Accessories.MODID)){
                        var slotName = attributeType.getPath();

                        if(!api.getAllSlots(reference.entity().level()).containsKey(slotName)) continue;

                        multimap.put(SlotAttribute.getSlotAttribute(slotName), attributeModifier);
                    } else {
                        var attribute = BuiltInRegistries.ATTRIBUTE.getOptional(attributeType);

                        if(attribute.isEmpty()) continue;

                        multimap.put(attribute.get(), attributeModifier);
                    }
                }
            }
        }

        //TODO: Decide if such presents of modifiers prevents the accessory modifiers from existing

        api.getAccessory(stack).ifPresent(accessory -> accessory.getModifiers(stack, reference, uuid));

        return multimap;
    }

    public UUID getOrCreateSlotUUID(SlotType slotType, int index) {
        return getOrCreateSlotUUID(slotType.name(), index);
    }

    public UUID getOrCreateSlotUUID(String slotName, int index) {
        return CACHED_UUIDS.computeIfAbsent(
                slottedId(slotName, index),
                s -> UUID.nameUUIDFromBytes(s.getBytes())
        );
    }

    public static String slottedId(SlotType slotType, int index) {
        return slottedId(slotType.name(), index);
    }

    public static String slottedId(String slotName, int index) {
        return slotName + "/" + index;
    }

    //--

    public Map<String, SlotType> getEntitySlots(LivingEntity livingEntity){
        return getSlots(livingEntity.level(), livingEntity.getType());
    }

    public Map<String, SlotType> getSlots(Level level, EntityType<?> entityType){
        var map = EntitySlotLoader.INSTANCE.getSlotTypes(level.isClientSide, entityType);

        return map != null ? map : Map.of();
    }

    public Optional<SlotType> getSlotType(Level level, String name){
        return Optional.ofNullable(getAllSlots(level).get(name));
    }

    public Map<String, SlotType> getAllSlots(Level level){
        return SlotTypeLoader.INSTANCE.getSlotTypes(level);
    }

    //--

    public boolean canInsertIntoSlot(LivingEntity entity, SlotReference reference, ItemStack stack){
        var predicates = reference.type().map(SlotType::validators).orElse(Set.of());

        return getPredicateResults(predicates, entity, reference, stack)
                && getAccessory(stack).map(accessory -> accessory.canEquip(stack, reference)).orElse(false);
    }

    public Collection<SlotType> getValidSlotTypes(LivingEntity entity, ItemStack stack){
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

    public Optional<SlotBasedPredicate> getPredicate(ResourceLocation location) {
        return Optional.ofNullable(PREDICATE_REGISTRY.get(location));
    }

    public void registerPredicate(ResourceLocation location, SlotBasedPredicate predicate) {
        if(PREDICATE_REGISTRY.containsKey(location)) {
            LOGGER.warn("[AccessoriesAPI]: A SlotBasedPredicate attempted to be registered but a duplicate entry existed already! [Id: " + location + "]");

            return;
        }

        PREDICATE_REGISTRY.put(location, predicate);
    }

    public boolean getPredicateResults(Set<ResourceLocation> predicateIds, LivingEntity entity, SlotReference reference, ItemStack stack){
        InteractionResult result = InteractionResult.PASS;

        for (var predicateId : predicateIds) {
            var predicate = getPredicate(predicateId);

            if(predicate.isEmpty()) continue;

            result = predicate.get().isValid(entity, reference, stack);

            if(result != InteractionResult.PASS) break;
        }

        return result.consumesAction();
    }

    public static final TagKey<Item> ALL_ACCESSORIES = TagKey.create(Registries.ITEM, Accessories.of("all"));

    private void initDefaultPredicates(){
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
    }
}
