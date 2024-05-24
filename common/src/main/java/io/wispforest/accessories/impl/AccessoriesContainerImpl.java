package io.wispforest.accessories.impl;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.datafixers.util.Pair;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.api.slot.SlotAttribute;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.data.SlotTypeLoader;
import net.minecraft.Util;
import net.minecraft.nbt.*;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.ApiStatus;

import java.util.*;
import java.util.stream.Collectors;

@ApiStatus.Internal
public class AccessoriesContainerImpl implements AccessoriesContainer, InstanceCodecable {

    protected AccessoriesCapability capability;
    private String slotName;

    protected final Map<UUID, AttributeModifier> modifiers = new HashMap<>();
    protected final Set<AttributeModifier> persistentModifiers = new HashSet<>();
    protected final Set<AttributeModifier> cachedModifiers = new HashSet<>();

    private final Multimap<AttributeModifier.Operation, AttributeModifier> modifiersByOperation = HashMultimap.create();

    private int baseSize;

    private List<Boolean> renderOptions;

    private ExpandedSimpleContainer accessories;
    private ExpandedSimpleContainer cosmeticAccessories;

    private boolean update = false;

    public AccessoriesContainerImpl(AccessoriesCapability capability, SlotType slotType){
        this.capability = capability;

        this.slotName = slotType.name();
        this.baseSize = slotType.amount();

        this.accessories = new ExpandedSimpleContainer(this.baseSize, "Accessories", false);
        this.cosmeticAccessories = new ExpandedSimpleContainer(this.baseSize, "Cosmetic Accessories", false);

        this.renderOptions = Util.make(new ArrayList<>(baseSize), booleans -> {
            for (int i = 0; i < baseSize; i++) booleans.add(i, true);
        });
    }

    public int getBaseSize(){
        return this.baseSize;
    }

    @Override
    public void markChanged(){
        this.update = true;

        if(this.capability.entity().level().isClientSide) return;

        var inv = ((AccessoriesCapabilityImpl) this.capability).getUpdatingInventories();

        inv.remove(this);
        inv.add(this);
    }

    @Override
    public boolean hasChanged() {
        return this.update;
    }

    public void update(){
        if(!update) return;

        this.update = false;

        if(this.capability.entity().level().isClientSide) return;

        var slotType = this.slotType();

        if (slotType != null) {
            this.baseSize = slotType.amount();
        }

        int baseSize = this.baseSize;

        for(AttributeModifier modifier : this.getModifiersForOperation(AttributeModifier.Operation.ADDITION)){
            baseSize += modifier.getAmount();
        }

        var size = baseSize;

        for(AttributeModifier modifier : this.getModifiersForOperation(AttributeModifier.Operation.MULTIPLY_BASE)){
            size += this.baseSize * modifier.getAmount();
        }

        for(AttributeModifier modifier : this.getModifiersForOperation(AttributeModifier.Operation.MULTIPLY_TOTAL)){
            size *= modifier.getAmount();
        }

        //--

        if(size == this.accessories.getContainerSize()) return;

        var invalidAccessories = new ArrayList<Pair<Integer, ItemStack>>();

        var invalidStacks = new ArrayList<ItemStack>();

        var newAccessories = new ExpandedSimpleContainer(size, "Accessories");
        var newCosmetics = new ExpandedSimpleContainer(size, "Cosmetic Accessories");

        for (int i = 0; i < this.accessories.getContainerSize(); i++) {
            if(i < newAccessories.getContainerSize()){
                newAccessories.setItem(i, this.accessories.getItem(i));
                newCosmetics.setItem(i, this.cosmeticAccessories.getItem(i));
            } else {
                invalidAccessories.add(Pair.of(i, this.accessories.getItem(i)));
                invalidStacks.add(this.cosmeticAccessories.getItem(i));
            }
        }

        newAccessories.copyPrev(this.accessories);
        newCosmetics.copyPrev(this.cosmeticAccessories);

        this.accessories = newAccessories;
        this.cosmeticAccessories = newCosmetics;

        var newRenderOptions = new ArrayList<Boolean>(size);

        for (int i = 0; i < size; i++) {
            newRenderOptions.add(i < this.renderOptions.size() ? this.renderOptions.get(i) : true);
        }

        this.renderOptions = newRenderOptions;

        var livingEntity = this.capability.entity();

        //TODO: Confirm if such is needed
        for (var invalidAccessory : invalidAccessories) {
            var index = invalidAccessory.getFirst();

            UUID uuid = UUID.nameUUIDFromBytes((slotName + invalidAccessory.getFirst()).getBytes());

            var invalidStack = invalidAccessory.getSecond();

            if(invalidStack.isEmpty()) continue;

            var slotReference = SlotReference.of(livingEntity, this.slotName, index);

            var attributes = AccessoriesAPI.getAttributeModifiers(invalidStack, slotReference, uuid);

            Multimap<String, AttributeModifier> slots = HashMultimap.create();

            Set<Attribute> toBeRemoved = new HashSet<>();

            attributes.asMap().forEach((attribute, modifier) -> {
                if(!(attribute instanceof SlotAttribute slotAttribute)) return;

                slots.putAll(slotAttribute.slotName(), modifier);
                toBeRemoved.add(slotAttribute);
            });

            for (Attribute attribute : toBeRemoved) attributes.removeAll(attribute);

            livingEntity.getAttributes().removeAttributeModifiers(attributes);
            this.capability.removeSlotModifiers(slots);

            var accessory = AccessoriesAPI.getAccessory(invalidStack);

            if(accessory != null) accessory.onUnequip(invalidStack, slotReference);

            invalidStacks.add(invalidStack);
        }

        ((AccessoriesHolderImpl) this.capability.getHolder()).invalidStacks.addAll(invalidStacks);

        if(this.update) this.capability.updateContainers();
    }

    @Override
    public int getSize() {
        this.update();
        return this.accessories.getContainerSize();
    }

    @Override
    public String getSlotName(){
        return this.slotName;
    }

    @Override
    public AccessoriesCapability capability() {
        return this.capability;
    }

    @Override
    public List<Boolean> renderOptions() {
        this.update();
        return this.renderOptions;
    }

    @Override
    public ExpandedSimpleContainer getAccessories() {
        this.update();
        return accessories;
    }

    @Override
    public ExpandedSimpleContainer getCosmeticAccessories() {
        this.update();
        return cosmeticAccessories;
    }

    @Override
    public Map<UUID, AttributeModifier> getModifiers() {
        return this.modifiers;
    }

    public Set<AttributeModifier> getCachedModifiers(){
        return this.cachedModifiers;
    }

    @Override
    public Collection<AttributeModifier> getModifiersForOperation(AttributeModifier.Operation operation) {
        return this.modifiersByOperation.get(operation);
    }

    @Override
    public void addTransientModifier(AttributeModifier modifier) {
        this.modifiers.put(modifier.getId(), modifier);
        this.getModifiersForOperation(modifier.getOperation()).add(modifier);
        this.markChanged();
    }

    @Override
    public void addPersistentModifier(AttributeModifier modifier) {
        this.addTransientModifier(modifier);
        this.persistentModifiers.add(modifier);
    }

    @Override
    public void removeModifier(UUID uuid) {
        var modifier = this.modifiers.get(uuid);

        if(modifier == null) return;

        this.persistentModifiers.remove(modifier);
        this.getModifiersForOperation(modifier.getOperation()).remove(modifier);
        this.markChanged();
    }

    @Override
    public void clearModifiers() {
        this.getModifiers().keySet().iterator().forEachRemaining(this::removeModifier);
    }

    @Override
    public void removeCachedModifiers(AttributeModifier modifier) {
        this.cachedModifiers.remove(modifier);
    }

    @Override
    public void clearCachedModifiers() {
        this.cachedModifiers.forEach(cachedModifier -> this.removeModifier(cachedModifier.getId()));
        this.cachedModifiers.clear();
    }

    //--

    public void copyFrom(AccessoriesContainerImpl other){
        this.modifiers.clear();
        this.modifiersByOperation.clear();
        this.persistentModifiers.clear();
        other.modifiers.values().forEach(this::addTransientModifier);
        other.persistentModifiers.forEach(this::addPersistentModifier);
        this.update();
    }

    //TODO: Confirm Cross Dimension stuff works!
//    public static void copyFrom(LivingEntity oldEntity, LivingEntity newEntity){
//        var api = AccessoriesAccess.getAPI();
//
//        var oldCapability = api.getCapability(oldEntity);
//        var newCapability = api.getCapability(newEntity);
//
//        if(oldCapability.isEmpty() || newCapability.isEmpty()) return;
//
//        var newContainers = newCapability.get().getContainers();
//        for (var containerEntries : oldCapability.get().getContainers().entrySet()) {
//            if(!newContainers.containsKey(containerEntries.getKey())) continue;
//        }
//    }

    //--

    public static final String SLOT_NAME_KEY = "SlotName";

    public static final String BASE_SIZE_KEY = "BaseSize";

    public static final String RENDER_OPTIONS_KEY = "RenderOptions";

    public static final String MODIFIERS_KEY = "Modifiers";
    public static final String PERSISTENT_MODIFIERS_KEY = "PersistentModifiers";
    public static final String CACHED_MODIFIERS_KEY = "CachedModifiers";

    public static final String ITEMS_KEY = "Items";
    public static final String COSMETICS_KEY = "Cosmetics";

    @Override
    public void write(CompoundTag tag) {
        write(tag, false);
    }

    @Override
    public void read(CompoundTag tag) {
        read(tag, false);
    }

    public void write(CompoundTag tag, boolean sync){
        tag.putString(SLOT_NAME_KEY, this.slotName);

        tag.putInt(BASE_SIZE_KEY, this.baseSize);

        ByteArrayTag renderOptionsTag = new ByteArrayTag(new byte[renderOptions.size()]);

        for (int i = 0; i < this.renderOptions.size(); i++) {
            renderOptionsTag.set(i, ByteTag.valueOf(this.renderOptions.get(i)));
        }

        tag.put(RENDER_OPTIONS_KEY, renderOptionsTag);

        if(!sync || this.accessories.wasNewlyConstructed()) {
            tag.putInt("size", accessories.getContainerSize());

            tag.put(ITEMS_KEY, accessories.createTag());
            tag.put(COSMETICS_KEY, cosmeticAccessories.createTag());
        }

        if(sync){
            if(!this.modifiers.isEmpty()){
                ListTag modifiersTag = new ListTag();

                this.modifiers.values().forEach(modifier -> modifiersTag.add(modifier.save()));

                tag.put(MODIFIERS_KEY, modifiersTag);
            }
        } else {
            if(!this.persistentModifiers.isEmpty()){
                var persistentTag = new ListTag();

                this.persistentModifiers.forEach(modifier -> persistentTag.add(modifier.save()));

                tag.put(PERSISTENT_MODIFIERS_KEY, persistentTag);
            }

            if(!this.modifiers.isEmpty()){
                var cachedTag = new ListTag();

                this.modifiers.values().forEach(modifier -> {
                    if(this.persistentModifiers.contains(modifier)) return;

                    cachedTag.add(modifier.save());
                });

                tag.put(CACHED_MODIFIERS_KEY, cachedTag);
            }
        }
    }

    public void read(CompoundTag tag, boolean sync){
        this.slotName = tag.getString(SLOT_NAME_KEY);

        var sizeFromTag = (tag.contains(BASE_SIZE_KEY)) ? tag.getInt(BASE_SIZE_KEY) : baseSize;

        var slotType = SlotTypeLoader.getSlotType(this.capability.entity().level(), this.slotName);

        this.baseSize = slotType != null ? slotType.amount() : sizeFromTag;

        var renderOptionsTag = tag.get(RENDER_OPTIONS_KEY);

        if(renderOptionsTag instanceof ByteArrayTag byteArrayTag) {
            this.renderOptions = byteArrayTag.stream()
                    .map(ByteTag::getAsByte)
                    .map(BooleanUtils::toBoolean)
                    .collect(Collectors.toList());
        }

        if(tag.contains("size")) {
            var size = tag.getInt("size");

            if(this.accessories.getContainerSize() != size) {
                this.accessories = new ExpandedSimpleContainer(size, "Accessories");
                this.cosmeticAccessories = new ExpandedSimpleContainer(size, "Cosmetic Accessories");
            }

            this.accessories.fromTag(tag.getList(ITEMS_KEY, Tag.TAG_COMPOUND));
            this.cosmeticAccessories.fromTag(tag.getList(COSMETICS_KEY, Tag.TAG_COMPOUND));
        }

        if(sync) {
            this.modifiers.clear();
            this.persistentModifiers.clear();
            this.modifiersByOperation.clear();

            if (tag.contains(MODIFIERS_KEY)) {
                var persistentTag = tag.getList(MODIFIERS_KEY, Tag.TAG_COMPOUND);

                for (int i = 0; i < persistentTag.size(); i++) {
                    var modifier = AttributeModifier.load(persistentTag.getCompound(i));

                    if (modifier != null) this.addTransientModifier(modifier);
                }
            }
        } else {
            if (tag.contains(PERSISTENT_MODIFIERS_KEY)) {
                var persistentTag = tag.getList(PERSISTENT_MODIFIERS_KEY, Tag.TAG_COMPOUND);

                for (int i = 0; i < persistentTag.size(); i++) {
                    var modifier = AttributeModifier.load(persistentTag.getCompound(i));

                    if (modifier != null) this.addPersistentModifier(modifier);
                }
            }

            if (tag.contains(CACHED_MODIFIERS_KEY)) {
                var cachedTag = tag.getList(PERSISTENT_MODIFIERS_KEY, Tag.TAG_COMPOUND);

                for (int i = 0; i < cachedTag.size(); i++) {
                    var modifier = AttributeModifier.load(cachedTag.getCompound(i));

                    if (modifier != null) {
                        this.cachedModifiers.add(modifier);
                        this.addTransientModifier(modifier);
                    }

                    this.update();
                }
            }
        }
    }

    public static SimpleContainer readContainer(CompoundTag tag, String key){
        return readContainers(tag, key).get(0);
    }

    public static List<SimpleContainer> readContainers(CompoundTag tag, String ...keys){
        var containers = new ArrayList<SimpleContainer>();

        for (String key : keys) {
            var stacks = new SimpleContainer();

            if(tag.contains(key)) stacks.fromTag(tag.getList(key, Tag.TAG_COMPOUND));

            containers.add(stacks);
        }

        return containers;
    }

    public static SimpleContainer copyContainerList(SimpleContainer container){
        return new SimpleContainer(container.getItems().toArray(ItemStack[]::new));
    }
}
