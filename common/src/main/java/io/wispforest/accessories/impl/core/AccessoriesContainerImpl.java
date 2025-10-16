package io.wispforest.accessories.impl.core;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.api.core.AccessoryRegistry;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.impl.AccessoryAttributeLogic;
import io.wispforest.accessories.impl.slot.ExtraSlotTypeProperties;
import io.wispforest.accessories.utils.AttributeUtils;
import io.wispforest.accessories.utils.BaseContainer;
import io.wispforest.accessories.utils.EndecUtils;
import io.wispforest.accessories.utils.InstanceEndec;
import io.wispforest.endec.Endec;
import io.wispforest.endec.SerializationAttribute;
import io.wispforest.endec.SerializationContext;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.KeyedEndec;
import io.wispforest.endec.util.MapCarrier;
import io.wispforest.endec.util.MapCarrierDecodable;
import io.wispforest.endec.util.MapCarrierEncodable;
import io.wispforest.owo.serialization.CodecUtils;
import io.wispforest.owo.serialization.format.nbt.NbtEndec;
import it.unimi.dsi.fastutil.ints.Int2BooleanLinkedOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerListener;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;

@ApiStatus.Internal
public class AccessoriesContainerImpl implements AccessoriesContainer, InstanceEndec, ContainerListener {

    protected AccessoriesCapability capability;
    private String slotName;

    protected final Map<ResourceLocation, AttributeModifier> modifiers = new HashMap<>();
    protected final Set<AttributeModifier> persistentModifiers = new HashSet<>();
    protected final Set<AttributeModifier> cachedModifiers = new HashSet<>();

    private final Multimap<AttributeModifier.Operation, AttributeModifier> modifiersByOperation = HashMultimap.create();

    @Nullable
    private Integer baseSize;

    private Map<Integer, Boolean> renderOptions;

    private ExpandedContainer accessories;
    private ExpandedContainer cosmeticAccessories;

    private boolean update = false;
    private boolean resizingUpdate = false;
    private boolean trackedForUpdate = false;

    public AccessoriesContainerImpl(AccessoriesCapability capability, SlotType slotType){
        this.capability = capability;

        this.slotName = slotType.name();
        this.baseSize = slotType.amount();

        this.accessories = new ExpandedContainer(this, this.baseSize, "accessories", false);
        this.cosmeticAccessories = new ExpandedContainer(this, this.baseSize, "cosmetic_accessories", false);

        this.renderOptions = new Int2BooleanLinkedOpenHashMap(baseSize);
    }

    @Override
    public boolean isClientSide() {
        return this.capability().entity().level().isClientSide();
    }

    protected boolean containerListenerLock = false;

    @Override
    public void containerChanged(Container container) {
        if(containerListenerLock) return;

        var cache = AccessoriesHolderImpl.getHolder(this.capability()).getLookupCache();

        if (cache != null) cache.clearContainerCache(this.slotName);

        if(((ExpandedContainer) container).name().contains("cosmetic")) return;

        this.markChanged();
        this.update();
    }

    @Nullable
    public Integer getBaseSize(){
        return this.baseSize;
    }

    @Override
    public void markChanged(boolean resizingUpdate){
        this.update = true;
        this.resizingUpdate = resizingUpdate;

        if(this.capability.entity().level().isClientSide()) return;

        var inv = AccessoriesHolderImpl.getHolder(this.capability).containersRequiringUpdates();

        var entry = inv.remove(this);

        this.trackedForUpdate = entry != null;

        inv.put(this, (this.trackedForUpdate ? entry : false) || resizingUpdate);
    }

    @Override
    public boolean hasChanged() {
        return this.update;
    }

    public void update(){
        var holder = AccessoriesHolderImpl.getHolder(this.capability());

        var hasChangeOccurred = !this.resizingUpdate;

        if(!update) return;

        this.update = false;

        if(this.capability.entity().level().isClientSide()) return;

        var slotType = this.slotType();

        if(this.baseSize == null) this.baseSize = 0;

        if (slotType != null && this.baseSize != slotType.amount()) {
            this.baseSize = slotType.amount();

            hasChangeOccurred = true;
        }

        double baseSize = this.baseSize;

        double size;

        if(ExtraSlotTypeProperties.getProperty(this.slotName, false).allowResizing()) {
            for (AttributeModifier modifier : this.getModifiersForOperation(AttributeModifier.Operation.ADD_VALUE)) {
                baseSize += modifier.amount();
            }

            size = baseSize;

            for (AttributeModifier modifier : this.getModifiersForOperation(AttributeModifier.Operation.ADD_MULTIPLIED_BASE)) {
                size += (this.baseSize * modifier.amount());
            }

            for (AttributeModifier modifier : this.getModifiersForOperation(AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)) {
                size *= modifier.amount();
            }
        } else {
            size = baseSize;
        }

        //--

        var currentSize = (int) Math.round(size);

        if(currentSize != this.accessories.getContainerSize()) {
            hasChangeOccurred = true;

            var invalidAccessories = new ArrayList<Pair<Integer, ItemStack>>();

            var invalidStacks = new ArrayList<ItemStack>();

            this.containerListenerLock = true;

            var newAccessories = new ExpandedContainer(this, currentSize, "accessories");
            var newCosmetics = new ExpandedContainer(this, currentSize, "cosmetic_accessories");

            newAccessories.toggleFlagablity();
            newCosmetics.toggleFlagablity();

            for (int i = 0; i < this.accessories.getContainerSize(); i++) {
                if (i < newAccessories.getContainerSize()) {
                    newAccessories.setItem(i, this.accessories.getItem(i));
                    newCosmetics.setItem(i, this.cosmeticAccessories.getItem(i));
                } else {
                    invalidAccessories.add(Pair.of(i, this.accessories.getItem(i)));
                    invalidStacks.add(this.cosmeticAccessories.getItem(i));
                }
            }

            newAccessories.toggleFlagablity();
            newCosmetics.toggleFlagablity();

            this.containerListenerLock = false;

            newAccessories.copyPrev(this.accessories);
            newCosmetics.copyPrev(this.cosmeticAccessories);

            this.accessories = newAccessories;
            this.cosmeticAccessories = newCosmetics;

            this.renderOptions = getWithSize(currentSize, this.renderOptions);

            var livingEntity = this.capability.entity();

            //TODO: Confirm if this is needed
            for (var invalidAccessory : invalidAccessories) {
                var index = invalidAccessory.getFirst();

                var invalidStack = invalidAccessory.getSecond();

                if (invalidStack.isEmpty()) continue;

                var slotReference = SlotReference.of(livingEntity, this.slotName, index);

                AttributeUtils.removeTransientAttributeModifiers(livingEntity, AccessoryAttributeLogic.getAttributeModifiers(invalidStack, slotReference));

                var accessory = AccessoryRegistry.getAccessoryOrDefault(invalidStack);

                if (accessory != null) accessory.onUnequip(invalidStack, slotReference);

                invalidStacks.add(invalidStack);
            }

            holder.invalidStacks.addAll(invalidStacks);

            if (this.update) this.capability.updateContainers();
        }

        if(!hasChangeOccurred) {
            if (!trackedForUpdate) {
                var inv = holder.containersRequiringUpdates();

                inv.remove(this);
            } else {
                trackedForUpdate = false;
            }
        } else {
            var cache = holder.getLookupCache();

            if (cache != null) cache.clearContainerCache(this.slotName);
        }
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
    public Map<Integer, Boolean> renderOptions() {
        this.update();
        return Collections.unmodifiableMap(this.renderOptions);
    }

    @Override
    public void setShouldRender(int index, boolean value) {
        var size = getSize();

        if (index >= 0 && index < size) {
            this.renderOptions.put(index, value);
        }
    }

    @Override
    public ExpandedContainer getAccessories() {
        this.update();
        return accessories;
    }

    @Override
    public ExpandedContainer getCosmeticAccessories() {
        this.update();
        return cosmeticAccessories;
    }

    @Override
    public Map<ResourceLocation, AttributeModifier> getModifiers() {
        return Collections.unmodifiableMap(this.modifiers);
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
        this.modifiers.put(modifier.id(), modifier);
        this.getModifiersForOperation(modifier.operation()).add(modifier);
        this.markChanged();
    }

    @Override
    public void addPersistentModifier(AttributeModifier modifier) {
        this.addTransientModifier(modifier);
        this.persistentModifiers.add(modifier);
    }

    @Override
    public boolean hasModifier(ResourceLocation location) {
        return this.modifiers.containsKey(location);
    }

    @Override
    public void removeModifier(ResourceLocation location) {
        var modifier = this.modifiers.remove(location);

        if(modifier == null) return;

        this.persistentModifiers.remove(modifier);
        this.getModifiersForOperation(modifier.operation()).remove(modifier);
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
        this.cachedModifiers.forEach(cachedModifier -> this.removeModifier(cachedModifier.id()));
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

    public static final KeyedEndec<String> SLOT_NAME_KEY = Endec.STRING.keyed("slot_name", "UNKNOWN");

    public static final KeyedEndec<Integer> BASE_SIZE_KEY = Endec.INT.keyed("base_size", () -> null);

    public static final KeyedEndec<Integer> CURRENT_SIZE_KEY = Endec.INT.keyed("current_size", 0);

    public static final KeyedEndec<Map<Integer, Boolean>> RENDER_OPTIONS_KEY = CodecUtils.eitherEndec(Endec.BOOLEAN.listOf(), Endec.map(Endec.INT, Endec.BOOLEAN))
        .xmap(either -> {
            return Either.unwrap(either.mapLeft(booleans -> {
                var map = new HashMap<Integer, Boolean>();

                for (int i = 0; i < booleans.size(); i++) {
                    var bl = booleans.get(i);

                    if (!bl) map.put(i, false);
                }

                return map;
            }));
        }, Either::right)
        .keyed("render_options", HashMap::new);

    public static final KeyedEndec<List<CompoundTag>> MODIFIERS_KEY = NbtEndec.COMPOUND.listOf().keyed("modifiers", ArrayList::new);
    public static final KeyedEndec<List<CompoundTag>> PERSISTENT_MODIFIERS_KEY = NbtEndec.COMPOUND.listOf().keyed("persistent_modifiers", ArrayList::new);
    public static final KeyedEndec<List<CompoundTag>> CACHED_MODIFIERS_KEY = NbtEndec.COMPOUND.listOf().keyed("cached_modifiers", ArrayList::new);

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final StructEndec<ItemStackWithSlot> SLOTTED_ITEMSTACK_ENDEC = CodecUtils.toStructEndec(((MapCodec.MapCodecCodec<ItemStackWithSlot>) ItemStackWithSlot.CODEC).codec())
        .structuredCatchErrors((ctx, serializer, struct, exception) -> {
            var container = ctx.getAttributeValue(ContainerAttribute.CONTAINER).container();

            // TODO: MAYBE BETTER ERROR?

            LOGGER.error("[ExpandedSimpleContainer] An error has occured while decoding stack!");
            LOGGER.error(" - Entity Effected: '{}'", container.capability().entity().toString());
            LOGGER.error(" - Container Name: '{}'", container.getSlotName());
            LOGGER.error(" - Tried to load invalid ItemStack: ", exception);

            return new ItemStackWithSlot(-1, ItemStack.EMPTY);
        });

    public static final KeyedEndec<List<ItemStackWithSlot>> ITEMS_KEY = SLOTTED_ITEMSTACK_ENDEC.listOf().keyed("items", ArrayList::new);
    public static final KeyedEndec<List<ItemStackWithSlot>> COSMETICS_KEY = SLOTTED_ITEMSTACK_ENDEC.listOf().keyed("cosmetics", ArrayList::new);

    @Override
    public void encode(MapCarrierEncodable carrier, SerializationContext ctx) {
        write(carrier, ctx, false);
    }

    public void write(MapCarrierEncodable carrier, SerializationContext ctx, boolean sync){
        carrier.put(SLOT_NAME_KEY, this.slotName);

        carrier.putIfNotNull(ctx, BASE_SIZE_KEY, this.baseSize);

        carrier.put(RENDER_OPTIONS_KEY, this.renderOptions);

        if(!sync || this.accessories.wasNewlyConstructed()) {
            carrier.put(CURRENT_SIZE_KEY, accessories.getContainerSize());

            carrier.put(ctx, ITEMS_KEY, accessories.saveItemsToList());
            carrier.put(ctx, COSMETICS_KEY, cosmeticAccessories.saveItemsToList());
        }

        if(sync){
            if(!this.modifiers.isEmpty()){
                var modifiersTag = new ArrayList<CompoundTag>();

                this.modifiers.values().forEach(modifier -> modifiersTag.add((CompoundTag) AttributeModifier.CODEC.encodeStart(NbtOps.INSTANCE, modifier).getOrThrow()));

                carrier.put(MODIFIERS_KEY, modifiersTag);
            }
        } else {
            if(!this.persistentModifiers.isEmpty()){
                var persistentTag = new ArrayList<CompoundTag>();

                this.persistentModifiers.forEach(modifier -> persistentTag.add((CompoundTag) AttributeModifier.CODEC.encodeStart(NbtOps.INSTANCE, modifier).getOrThrow()));

                carrier.put(PERSISTENT_MODIFIERS_KEY, persistentTag);
            }

            if(!this.modifiers.isEmpty()){
                var cachedTag = new ArrayList<CompoundTag>();

                this.modifiers.values().forEach(modifier -> {
                    if(this.persistentModifiers.contains(modifier)) return;

                    cachedTag.add((CompoundTag) AttributeModifier.CODEC.encodeStart(NbtOps.INSTANCE, modifier).getOrThrow());
                });

                carrier.put(CACHED_MODIFIERS_KEY, cachedTag);
            }
        }
    }

    @Override
    public void decode(MapCarrierDecodable carrier, SerializationContext ctx) {
        read(carrier, ctx, false);
    }

    public void read(MapCarrierDecodable carrier, SerializationContext ctx, boolean sync){
        EndecUtils.dfuKeysCarrier(
                carrier,
                Map.of(
                        "SlotName", "slot_name",
                        "BaseSize", "base_size",
                        "CurrentSize", "current_size",
                        "RenderOptions", "render_options",
                        "Modifiers", "modifiers",
                        "PersistentModifiers", "persistent_modifiers",
                        "CachedModifiers", "cached_modifiers",
                        "Items", "items",
                        "Cosmetics", "cosmetics"
                ));

        this.slotName = carrier.get(SLOT_NAME_KEY);

        this.baseSize = carrier.get(BASE_SIZE_KEY);

        if(sync) {
            this.modifiers.clear();
            this.persistentModifiers.clear();
            this.modifiersByOperation.clear();

            if (carrier.has(MODIFIERS_KEY)) {
                var persistentTag = carrier.get(MODIFIERS_KEY);

                for (var compoundTag : persistentTag) {
                    var modifier = AttributeModifier.CODEC.parse(NbtOps.INSTANCE, compoundTag).getOrThrow();

                    if (modifier != null) this.addTransientModifier(modifier);
                }
            }
        } else {
            if (carrier.has(PERSISTENT_MODIFIERS_KEY)) {
                var persistentTag = carrier.get(PERSISTENT_MODIFIERS_KEY);

                for (var compoundTag : persistentTag) {
                    var modifier = AttributeModifier.CODEC.parse(NbtOps.INSTANCE, compoundTag).getOrThrow();

                    if (modifier != null) this.addPersistentModifier(modifier);
                }
            }

            if (carrier.has(CACHED_MODIFIERS_KEY)) {
                var cachedTag = carrier.get(CACHED_MODIFIERS_KEY);

                for (CompoundTag compoundTag : cachedTag) {
                    var modifier = AttributeModifier.CODEC.parse(NbtOps.INSTANCE, compoundTag).getOrThrow();

                    if (modifier != null) {
                        this.cachedModifiers.add(modifier);
                        this.addTransientModifier(modifier);
                    }
                }

                this.update();
            }
        }

        if(carrier.has(CURRENT_SIZE_KEY)) {
            ctx = ctx.withAttributes(ContainerAttribute.CONTAINER.instance(new ContainerAttribute(this)));
            var currentSize = carrier.get(CURRENT_SIZE_KEY);

            var sentOptions = carrier.get(RENDER_OPTIONS_KEY);

            this.renderOptions = getWithSize(currentSize, sentOptions);

            if(this.accessories.getContainerSize() != currentSize) {
                this.accessories = new ExpandedContainer(this, currentSize, "accessories");
                this.cosmeticAccessories = new ExpandedContainer(this, currentSize, "cosmetic_accessories");
            }

            this.accessories.loadItemsFromList(carrier.get(ctx, ITEMS_KEY));
            this.cosmeticAccessories.loadItemsFromList(carrier.get(ctx, COSMETICS_KEY));
        } else {
            this.renderOptions = carrier.get(RENDER_OPTIONS_KEY);
        }
    }

    private Map<Integer, Boolean> getWithSize(int size, Map<Integer, Boolean> map) {
        var sizedList = new Int2BooleanLinkedOpenHashMap(size);

        for (int i = 0; i < size; i++) {
            var value = (i < map.size()) ? map.get(i) : null;

            if (value != null) sizedList.put(i, (boolean) value);
        }

        return sizedList;
    }

    public static BaseContainer readContainer(MapCarrier carrier, SerializationContext ctx, KeyedEndec<List<ItemStackWithSlot>> key){
        return readContainers(carrier, ctx, key).get(0);
    }

    @SafeVarargs
    public static List<BaseContainer> readContainers(MapCarrier carrier, SerializationContext ctx, KeyedEndec<List<ItemStackWithSlot>> ...keys){
        var containers = new ArrayList<BaseContainer>();

        for (var key : keys) {
            var stacks = new BaseContainer();

            if(carrier.has(key)) stacks.loadItemsFromList(carrier.get(ctx, key));

            containers.add(stacks);
        }

        return containers;
    }

    public static BaseContainer copyContainerList(BaseContainer container){
        return new BaseContainer(container.getItems().toArray(ItemStack[]::new));
    }

    private record ContainerAttribute(AccessoriesContainer container) implements SerializationAttribute.Instance{
        public static final SerializationAttribute.WithValue<ContainerAttribute> CONTAINER = SerializationAttribute.withValue("accessories_container");

        @Override public SerializationAttribute attribute() { return CONTAINER; }
        @Override public Object value() { return this;}
    }

    private static final class ListFromMap<T> extends AbstractList<T> {
        private final Map<Integer, T> map;

        private ListFromMap(Map<Integer, T> map) {
            this.map = map;
        }

        public Map<Integer, T> map() {
            return map;
        }

        @Override
        public T get(int index) {
            return null;
        }



        @Override
        public int size() {
            return 0;
        }


//        @Override
//        public boolean equals(Object obj) {
//            if (obj == this) return true;
//            if (obj == null || obj.getClass() != this.getClass()) return false;
//            var that = (ListFromMap) obj;
//            return Objects.equals(this.map, that.map);
//        }
//
//        @Override
//        public int hashCode() {
//            return Objects.hash(map);
//        }
//
//        @Override
//        public String toString() {
//            return "ListFromMap[" +
//                "map=" + map + ']';
//        }


    }
}
