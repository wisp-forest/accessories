package io.wispforest.accessories.api;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.logging.LogUtils;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.api.events.AccessoriesEvents;
import io.wispforest.accessories.api.slot.SlotAttribute;
import io.wispforest.accessories.api.slot.SlotBasedPredicate;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.accessories.networking.client.AccessoryBreak;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;

/**
 * Base implementation of API
 */
public class AccessoriesAPI {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final Accessory DEFAULT = new Accessory() {
        @Override
        public int maxStackSize(ItemStack stack) {
            return 1;
        }
    };

    private static final Map<String, UUID> CACHED_UUIDS = new HashMap<>();

    private static final Map<ResourceLocation, SlotBasedPredicate> PREDICATE_REGISTRY = new HashMap<>();

    private static final Map<Item, Accessory> REGISTER = new HashMap<>();

    //--

    /**
     * Main method to register a given {@link Item} to given {@link Accessory}
     */
    public static void registerAccessory(Item item, Accessory accessory) {
        REGISTER.put(item, accessory);
    }

    /**
     * Attempt to get a {@link Accessory} bound to an {@link Item} or an Empty {@link Optional}
     */
    @Nullable
    public static Accessory getAccessory(Item item) {
        return REGISTER.get(item);
    }

    /**
     * Attempt to get a {@link Accessory} bound to an {@link ItemStack}'s Item or an Empty {@link Optional}
     */
    @Nullable
    public static Accessory getAccessory(ItemStack stack){
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
        return REGISTER.getOrDefault(item, defaultAccessory());
    }

    /**
     * @return Default {@link Accessory}
     */
    public static Accessory defaultAccessory(){
        return DEFAULT;
    }

    /**
     * @return If a given {@link ItemStack} is found either to have an {@link Accessory} besides the
     * default or if the given stack has valid slots which it can be equipped
     */
    public static boolean isValidAccessory(ItemStack stack, Level level){
        return getAccessory(stack) != null || (getStackSlotTypes(level, stack).size() > 0);
    }

    //--

    public static Multimap<Attribute, AttributeModifier> getAttributeModifiers(ItemStack stack, SlotReference slotReference, UUID uuid){
        return getAttributeModifiers(stack, slotReference.entity(), slotReference.slotName(), slotReference.slot(), uuid);
    }

    public static Multimap<Attribute, AttributeModifier> getAttributeModifiers(ItemStack stack, String slotName, int slot, UUID uuid){
        return getAttributeModifiers(stack, null, slotName, slot, uuid);
    }

    /**
     * Attempts to get any at all AttributeModifier's found on the stack either though NBT or the Accessory bound
     * to the {@link ItemStack}'s item
     */
    public static Multimap<Attribute, AttributeModifier> getAttributeModifiers(ItemStack stack, @Nullable LivingEntity entity, String slotName, int slot, UUID uuid){
        Multimap<Attribute, AttributeModifier> multimap = HashMultimap.create();

        if(stack.getTag() != null && stack.getTag().contains("AccessoriesAttributeModifiers", Tag.TAG_LIST)){
            var attributes = stack.getTag().getList("AccessoriesAttributeModifiers", Tag.TAG_COMPOUND);

            for (int i = 0; i < attributes.size(); i++) {
                var attributeTag = attributes.getCompound(i);

                if(attributeTag.contains("Slot") && !attributeTag.getString("Slot").equals(slotName)){
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
                    var attributeSlotName = attributeType.getPath();

                    if(entity != null && !SlotTypeLoader.getSlotTypes(entity.level()).containsKey(attributeSlotName)) continue;

                    multimap.put(SlotAttribute.getSlotAttribute(attributeSlotName), attributeModifier);
                } else {
                    var attribute = BuiltInRegistries.ATTRIBUTE.getOptional(attributeType);

                    if(attribute.isEmpty()) continue;

                    multimap.put(attribute.get(), attributeModifier);
                }
            }
        }

        if(entity != null) {
            //TODO: Decide if such presents of modifiers prevents the accessory modifiers from existing
            var accessory = AccessoriesAPI.getAccessory(stack);

            if(accessory != null) {
                var data = accessory.getModifiers(stack, new SlotReference(slotName, entity, slot), uuid);

                multimap.putAll(data);
            }
        }

        return multimap;
    }

    public static void addAttribute(ItemStack stack, String slotName, Attribute attribute, String name, UUID id, double amount, AttributeModifier.Operation operation) {
        addAttribute(stack.getOrCreateTag(), slotName, attribute, name, id, amount, operation);
    }

    public static void addAttribute(CompoundTag tag, String slotName, Attribute attribute, String name, UUID id, double amount, AttributeModifier.Operation operation) {
        var attributes = tag.getList("AccessoriesAttributeModifiers", Tag.TAG_COMPOUND);

        var attributeTag = new CompoundTag();

        attributeTag.putString("Name", name);
        attributeTag.putUUID("UUID", id);
        attributeTag.putDouble("Amount", amount);
        attributeTag.putInt("Operation", operation.toValue());

        attributeTag.putString("Slot", slotName);

        var attributeId = (attribute instanceof SlotAttribute slotAttribute)
                ? Accessories.of(slotAttribute.slotName())
                : BuiltInRegistries.ATTRIBUTE.getKey(attribute);

        attributeTag.putString("AttributeName", attributeId.toString());

        attributes.add(attributeTag);
    }

    //--

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
     * Used to check if the given {@link ItemStack} is valid for the given LivingEntity and SlotReference
     * based on {@link SlotBasedPredicate}s bound to the Slot and the {@link Accessory} bound to the stack if present
     */
    public static boolean canInsertIntoSlot(ItemStack stack, SlotReference reference){
        var slotType = reference.type();

        if(slotType == null) {
            throw new IllegalStateException("Unable to get the needed SlotType from the SlotReference passed within `canInsertIntoSlot`! [Name: " + reference.slotName() + "]");
        }

        return getPredicateResults(slotType.validators(), slotType, 0, stack) && canEquip(stack, reference);
    }

    /**
     * Method used to check weather or not the given stack can be equipped within the slot referenced
     *
     * @param stack
     * @param reference
     * @return if the stack can be equipped or not
     */
    public static boolean canEquip(ItemStack stack, SlotReference reference){
        var accessory = getOrDefaultAccessory(stack);

        var state = AccessoriesEvents.CAN_EQUIP_EVENT.invoker().onEquip(stack, reference);

        if(state != TriState.DEFAULT) return state.get();

        return accessory.canEquip(stack, reference);
    }

    /**
     * Method used to check weather or not the given stack can be unequipped within the slot referenced
     *
     * @param stack
     * @param reference
     * @return if the stack can be unequipped or not
     */
    public static boolean canUnequip(ItemStack stack, SlotReference reference){
        var accessory = getOrDefaultAccessory(stack);

        var state = AccessoriesEvents.CAN_UNEQUIP_EVENT.invoker().onUnequip(stack, reference);

        if(state != TriState.DEFAULT) return state.get();

        return accessory.canUnequip(stack, reference);
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

                for (var accessory : containers.get(value.name()).getAccessories()) {
                    var reference = new SlotReference(container.getSlotName(), entity, accessory.getFirst());

                    if (canInsertIntoSlot(stack, reference)) validSlots.add(value);
                }
            }
        }

        return validSlots;
    }

    public static Collection<SlotType> getStackSlotTypes(Level level, ItemStack stack){
        var validSlots = new ArrayList<SlotType>();

        for (SlotType value : SlotTypeLoader.getSlotTypes(level).values()) {
            if(getPredicateResults(value.validators(), value, 0, stack)) validSlots.add(value);
        }

        return validSlots;
    }

    public static Collection<SlotType> getUsedSlotsFor(Player player) {
        return getUsedSlotsFor(player, player.getInventory());
    }

    public static Collection<SlotType> getUsedSlotsFor(LivingEntity entity, Container container) {
        var slots = new HashSet<SlotType>();

        for (int i = 0; i < container.getContainerSize(); i++) {
            var stack = container.getItem(i);

            if (stack.isEmpty()) continue;

            slots.addAll(AccessoriesAPI.getValidSlotTypes(entity, stack));
        }

        var capability = entity.accessoriesCapability();

        for (var ref : capability.getAllEquipped()) {
            slots.addAll(AccessoriesAPI.getValidSlotTypes(entity, ref.stack()));
        }

        for (var slot : SlotTypeLoader.getSlotTypes(entity.level()).values()) {
            var bl = BuiltInRegistries.ITEM.getTag(AccessoriesAPI.getSlotTag(slot))
                    .map(holders -> holders.size() > 0)
                    .orElse(false);

            if (bl) slots.add(slot);
        }

        return slots;
    }

    /**
     * Helper method to trigger effects of a given accessory being broken on any tracking clients for the given entity
     */
    public static void breakStack(SlotReference reference){
        AccessoriesInternals.getNetworkHandler().sendToTrackingAndSelf(reference.entity(), new AccessoryBreak(reference));
    }

    //--

    /**
     * @return {@link SlotBasedPredicate} bound to the given {@link ResourceLocation} or an Empty {@link Optional} if absent
     */
    @Nullable
    public static SlotBasedPredicate getPredicate(ResourceLocation location) {
        return PREDICATE_REGISTRY.get(location);
    }

    public static void registerPredicate(ResourceLocation location, SlotBasedPredicate predicate) {
        if(PREDICATE_REGISTRY.containsKey(location)) {
            LOGGER.warn("[AccessoriesAPI]: A SlotBasedPredicate attempted to be registered but a duplicate entry existed already! [Id: " + location + "]");

            return;
        }

        PREDICATE_REGISTRY.put(location, predicate);
    }

    public static boolean getPredicateResults(Set<ResourceLocation> predicateIds, SlotType slotType, int index, ItemStack stack){
        var result = TriState.DEFAULT;

        for (var predicateId : predicateIds) {
            var predicate = getPredicate(predicateId);

            if(predicate == null) continue;

            result = predicate.isValid(slotType, index, stack);

            if(result != TriState.DEFAULT) break;
        }

        return result.orElse(false);
    }

    /**
     * TagKey in which allows for a given Item to pass {@link SlotBasedPredicate} allowing such to be equipped if
     * desired
     */
    public static final TagKey<Item> ALL_ACCESSORIES = TagKey.create(Registries.ITEM, Accessories.of("all"));

    public static final String ACCESSORY_PREDICATES_KEY = "AccessoryPredicates";

    public static final String ACCESSORY_VALID_SLOTS_KEY = "ValidSlotOverrides";
    public static final String ACCESSORY_INVALID_SLOTS_KEY = "InvalidSlotOverrides";

    public static TagKey<Item> getSlotTag(SlotType slotType) {
        var location = slotType.uniqueSlot() ? new ResourceLocation(slotType.name()) : Accessories.of(slotType.name());

        return TagKey.create(Registries.ITEM, location);
    }

    static {
        registerPredicate(Accessories.of("all"), (slotType, i, stack) -> TriState.TRUE);
        registerPredicate(Accessories.of("none"), (slotType, i, stack) -> TriState.FALSE);
        registerPredicate(Accessories.of("tag"), (slotType, i, stack) -> {
            return (stack.is(getSlotTag(slotType)) || stack.is(ALL_ACCESSORIES)) ? TriState.TRUE : TriState.DEFAULT;
        });
        registerPredicate(Accessories.of("relevant"), (slotType, i, stack) -> {
            var bl = !getAttributeModifiers(stack, slotType.name(), i, getOrCreateSlotUUID(slotType.name(), i)).isEmpty();

            return bl ? TriState.TRUE : TriState.DEFAULT;
        });
        registerPredicate(Accessories.of("compound"), (slotType, index, stack) -> {
            var tag = stack.getTag();
            var name = slotType.name();

            if(tag != null && tag.contains(ACCESSORY_PREDICATES_KEY)) {
                var extraData = tag.getCompound(ACCESSORY_PREDICATES_KEY);

                var invalidSlots = extraData.getList(ACCESSORY_INVALID_SLOTS_KEY, Tag.TAG_STRING);

                for (int i = 0; i < invalidSlots.size(); i++) {
                    var invalidSlot = invalidSlots.getString(i);

                    if (name.equals(invalidSlot)) return TriState.FALSE;
                }

                //--

                var validSlots = extraData.getList(ACCESSORY_VALID_SLOTS_KEY, Tag.TAG_STRING);

                for (int i = 0; i < validSlots.size(); i++) {
                    var validSlot = validSlots.getString(i);

                    if(validSlot.equals("any")) return TriState.TRUE;

                    if (name.equals(validSlot)) return TriState.TRUE;
                }
            }

            return TriState.DEFAULT;
        });
    }
}