package io.wispforest.accessories.impl.core;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import com.mojang.logging.LogUtils;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.endec.NbtMapCarrier;
import io.wispforest.accessories.impl.PlayerEquipControl;
import io.wispforest.accessories.impl.caching.AccessoriesHolderLookupCache;
import io.wispforest.accessories.impl.option.AccessoriesPlayerOptionsHolder;
import io.wispforest.accessories.impl.option.PlayerOption;
import io.wispforest.accessories.impl.option.PlayerOptions;
import io.wispforest.accessories.pond.AccessoriesLivingEntityExtension;
import io.wispforest.accessories.utils.EndecUtils;
import io.wispforest.endec.Endec;
import io.wispforest.endec.SerializationAttribute;
import io.wispforest.endec.SerializationContext;
import io.wispforest.endec.impl.KeyedEndec;
import io.wispforest.endec.util.MapCarrier;
import io.wispforest.endec.util.MapCarrierDecodable;
import io.wispforest.endec.util.MapCarrierEncodable;
import io.wispforest.accessories.utils.InstanceEndec;
import io.wispforest.owo.serialization.RegistriesAttribute;
import io.wispforest.owo.serialization.format.nbt.NbtEndec;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.*;

import java.util.concurrent.locks.ReentrantLock;

@ApiStatus.Internal
public class AccessoriesHolderImpl implements InstanceEndec {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final MapCarrier EMPTY = new NbtMapCarrier(new CompoundTag());

    private final Map<String, AccessoriesContainer> slotContainers = new LinkedHashMap<>();

    public final List<ItemStack> invalidStacks = new ArrayList<>();

    private final Map<AccessoriesContainer, Boolean> containersRequiringUpdates = new HashMap<>();

    // --

    private MapCarrierDecodable carrier;
    protected boolean loadedFromTag = false;

    public AccessoriesHolderImpl(){}

    public boolean loadedFromTag() {
        return loadedFromTag;
    }

    public Map<AccessoriesContainer, Boolean> containersRequiringUpdates() {
        return containersRequiringUpdates;
    }

    public static AccessoriesHolderImpl of(){
        var holder = new AccessoriesHolderImpl();

        holder.loadedFromTag = true;
        holder.carrier = EMPTY;

        return holder;
    }

    @Nullable
    public static AccessoriesHolderImpl getHolder(LivingEntity livingEntity) {
        var capability = ((AccessoriesLivingEntityExtension)livingEntity).getOrCreateAccessoriesCapability();

        if (capability == null) return null;

        return getHolder(capability);
    }


    public static AccessoriesHolderImpl getHolder(AccessoriesCapability capability) {
        var entity = capability.entity();

        var holder = AccessoriesInternals.getHolder(entity);

        // If data has been yet to be loaded
        if (holder.loadedFromTag) {
            if (entity.level().isClientSide()) {
                // Will init containers from data
                holder.init(capability);
            } else {
                // Reset the container when loaded from tag on the server
                capability.reset(true);
            }
        } else if (!isEntitySlotsValid(entity, holder)) {
            // Prevents containers from not existing even if a given entity will have such slots but have yet to be synced to the client
            holder.init(capability);
        }

        return holder;
    }

    private static final Cache<Integer, Boolean> validatedServerEntities = CacheBuilder.newBuilder()
        .expireAfterAccess(Duration.ofSeconds(30))
        .build();

    private static final Cache<Integer, Boolean> validatedClientEntities = CacheBuilder.newBuilder()
        .expireAfterAccess(Duration.ofSeconds(30))
        .build();

    private static boolean isEntitySlotsValid(LivingEntity entity, AccessoriesHolderImpl holder){
        var validEntities = entity.level().isClientSide()
            ? validatedClientEntities
            : validatedServerEntities;

        var hash = Objects.hash(entity.getUUID(), entity.hashCode());

        var result = validEntities.getIfPresent(hash);

        if (result != null) {
            if (result) return true;

            validEntities.invalidate(hash);
        }

        var currentContainers = holder.getSlotContainers();
        var requiredSlotTypes = EntitySlotLoader.getEntitySlots(entity);

        result = currentContainers.size() == requiredSlotTypes.size();

        if (result) validEntities.put(hash, true);

        return result;
    }

    public static void clearValidationCache(boolean isClientSide) {
        (isClientSide ? validatedClientEntities : validatedServerEntities).invalidateAll();
    }

    //--

    @ApiStatus.Internal
    public Map<String, AccessoriesContainer> getAllSlotContainers() {
        return Collections.unmodifiableMap(this.slotContainers);
    }

    @Nullable
    private Map<String, AccessoriesContainer> validSlotContainers = null;

    public void setValidTypes(Set<String> validTypes) {
        if (this.currentlyInitializingHolder.isLocked()) {
            var threadOwner = currentlyInitializingHolder.getOwner();

            var threadOwnerName = "";

            if (threadOwner != null) threadOwnerName = threadOwner.getName();

            LOGGER.warn("Valid Slot View was attempted to created but somehow its currently Locked! [Current Thread: {}, Lock Owner: {}]", Thread.currentThread().getName(), threadOwnerName);

            return;
        }

        var validSlotContainers = ImmutableMap.<String, AccessoriesContainer>builder();

        this.slotContainers.forEach((string, container) -> {
            if (validTypes.contains(container.getSlotName())) validSlotContainers.put(string, container);
        });

        this.validSlotContainers = validSlotContainers.build();

//        if (this.lookupCache == null) {
//            this.lookupCache = new AccessoriesHolderLookupCache(this);
//        }
//
//        this.lookupCache.clearCache();
    }

    @ApiStatus.Internal
    public Map<String, AccessoriesContainer> getSlotContainers() {
        return this.validSlotContainers != null ? this.validSlotContainers : Collections.unmodifiableMap(this.getAllSlotContainers());
    }

    @Nullable
    public AccessoriesHolderLookupCache lookupCache = null;

    @Nullable
    public AccessoriesHolderLookupCache getLookupCache() {
        // TODO: FIX ISSUES WITH LOOK UP CACHE LEADING TO IT EITHER:
        /*
            - Not updating on death with YIGD
            - Not updating with old save leading to issues where Accessorie changes not being picked up
         */
        return /*Accessories.config().useExperimentalCaching() ? this.lookupCache :*/ null;
    }

    //--

    private final OwnerAccessibleReentrantLock currentlyInitializingHolder = new OwnerAccessibleReentrantLock();

    public void init(AccessoriesCapability capability) {
        var livingEntity = capability.entity();

        //this.slotContainers.clear();

        var entitySlots = EntitySlotLoader.getEntitySlots(livingEntity);

        //LOGGER.error("Entity Slots for [{}]: {}", livingEntity, entitySlots.keySet());

        if(livingEntity instanceof Player && entitySlots.isEmpty()) {
            LOGGER.warn("It seems the given player has no slots bound to it within a init call, is that desired?");
        }

        this.validSlotContainers = null;

        try {
            this.currentlyInitializingHolder.lock();

            if (loadedFromTag) {
                entitySlots.forEach((s, slotType) -> {
                    this.slotContainers.putIfAbsent(s, new AccessoriesContainerImpl(capability, slotType));
                });

                var ctx = SerializationContext.attributes(
                        new EntityAttribute(livingEntity),
                        RegistriesAttribute.of(livingEntity.registryAccess())
                );

                read(capability, livingEntity, this.carrier, ctx);
            } else {
                entitySlots.forEach((s, slotType) -> {
                    this.slotContainers.put(s, new AccessoriesContainerImpl(capability, slotType));
                });
            }

        } finally {
            this.currentlyInitializingHolder.unlock();
        }

        this.setValidTypes(entitySlots.keySet());
    }

    // TODO: SPLIT DECODING AND VALIDATION SAFETY DOWN THE ROAD
    private static final KeyedEndec<Map<String, AccessoriesContainer>> CONTAINERS_KEY = NbtEndec.COMPOUND.xmapWithContext(
            (ctx, containersMap) -> {
                var entity = ctx.requireAttributeValue(EntityAttribute.ENTITY).livingEntity();
                var slotContainers = ctx.requireAttributeValue(ContainersAttribute.CONTAINERS).slotContainers();
                var invalidStacks = ctx.requireAttributeValue(InvalidStacksAttribute.INVALID_STACKS).invalidStacks();

                var slots = EntitySlotLoader.getEntitySlots(entity);

                for (var key : containersMap.keySet()) {
                    var containerElement = containersMap.getCompoundOrEmpty(key);

                    if (containerElement.isEmpty()) continue; // TODO: Handle this case?

                    if (slots.containsKey(key)) {
                        var container = slotContainers.get(key);
                        var prevAccessories = AccessoriesContainerImpl.copyContainerList(container.getAccessories());
                        var prevCosmetics = AccessoriesContainerImpl.copyContainerList(container.getCosmeticAccessories());

                        ((AccessoriesContainerImpl) container).decode(new NbtMapCarrier(containerElement), ctx);

                        if (prevAccessories.getContainerSize() > container.getSize()) {
                            for (int i = container.getSize() - 1; i < prevAccessories.getContainerSize(); i++) {
                                var prevStack = prevAccessories.getItem(i);

                                if (!prevStack.isEmpty()) invalidStacks.add(prevStack);

                                var prevCosmetic = prevCosmetics.getItem(i);

                                if (!prevCosmetic.isEmpty()) invalidStacks.add(prevCosmetic);
                            }
                        }
                    } else {
                        var containers = AccessoriesContainerImpl.readContainers(
                            new NbtMapCarrier(containerElement),
                            ctx,
                            AccessoriesContainerImpl.COSMETICS_KEY, AccessoriesContainerImpl.ITEMS_KEY);

                        for (var simpleContainer : containers) {
                            for (int i = 0; i < simpleContainer.getContainerSize(); i++) {
                                var stack = simpleContainer.getItem(i);

                                if (!stack.isEmpty()) invalidStacks.add(stack);
                            }
                        }
                    }
                }

                return slotContainers;
            }, (ctx, containers) -> {
                var containerMap = new CompoundTag();

                containers.forEach((s, container) -> {
                    containerMap.put(s, Util.make(NbtMapCarrier.of(), innerCarrier -> ((AccessoriesContainerImpl) container).encode(innerCarrier, ctx)).compoundTag());
                });

                return containerMap;
            }).keyed("accessories_containers", HashMap::new);

    @Override
    public void encode(MapCarrierEncodable carrier, SerializationContext ctx) {
        if(slotContainers.isEmpty()) return;

        carrier.put(ctx, CONTAINERS_KEY, this.slotContainers);
    }

    public void read(LivingEntity entity, MapCarrier carrier, SerializationContext ctx) {
        read(entity.accessoriesCapability(), entity, carrier, ctx);
    }

    private static final KeyedEndec<PlayerEquipControl> EQUIP_CONTROL_KEY = Endec.forEnum(PlayerEquipControl.class).keyed("equip_control", PlayerEquipControl.MUST_CROUCH);

    private static final KeyedEndec<Boolean> SHOW_UNUSED_SLOTS_KEY = Endec.BOOLEAN.keyed("show_unused_slots", false);
    private static final KeyedEndec<Boolean> SHOW_COSMETICS_KEY = Endec.BOOLEAN.keyed("show_cosmetics", false);

    private static final KeyedEndec<Integer> COLUMN_AMOUNT_KEY = Endec.INT.keyed("column_amount", 1);
    private static final KeyedEndec<Integer> WIDGET_TYPE_KEY = Endec.INT.keyed("widget_type", 2);
    private static final KeyedEndec<Boolean> MAIN_WIDGET_POSITION = Endec.BOOLEAN.keyed("main_widget_position", true);
    private static final KeyedEndec<Boolean> SIDE_WIDGET_POSITION = Endec.BOOLEAN.keyed("side_widget_position", false);

    private static final KeyedEndec<Boolean> SHOW_GROUP_FILTER = Endec.BOOLEAN.keyed("show_group_filter", false);
    private static final KeyedEndec<Set<String>> FILTERED_GROUPS_KEY = Endec.STRING.setOf().keyed("filtered_groups", HashSet::new);

    private static final KeyedEndec<Boolean> SHOW_CRAFTING_GRID = Endec.BOOLEAN.keyed("cosmetics_shown", false);

    public void read(AccessoriesCapability capability, LivingEntity entity, MapCarrierDecodable carrier, SerializationContext ctx) {
        this.loadedFromTag = false;

        EndecUtils.dfuKeysCarrier(
                carrier,
                Map.of(
                        "AccessoriesContainers", "accessories_containers",
                        "CosmeticsShown", "cosmetics_shown",
                        "LinesShown", "lines_shown",
                        "EquipControl", "equip_control"
                ));

        carrier.getWithErrors(ctx.withAttributes(new ContainersAttribute(this.slotContainers), new InvalidStacksAttribute(this.invalidStacks)), CONTAINERS_KEY);

        // TODO: REMOVE WITHIN THE FUTURE WHEN A GOOD AMOUNT OF TIME TO TRANSITION HAS OCCURRED
        if (entity instanceof ServerPlayer player) {
            var options = AccessoriesPlayerOptionsHolder.getOptions(player);

            setIfPresent(carrier, options, EQUIP_CONTROL_KEY, PlayerOptions.EQUIP_CONTROL);

            setIfPresent(carrier, options, COLUMN_AMOUNT_KEY, PlayerOptions.COLUMN_AMOUNT);
            setIfPresent(carrier, options, WIDGET_TYPE_KEY, PlayerOptions.WIDGET_TYPE);
            setIfPresent(carrier, options, MAIN_WIDGET_POSITION, PlayerOptions.MAIN_WIDGET_POSITION);
            setIfPresent(carrier, options, SIDE_WIDGET_POSITION, PlayerOptions.SIDE_WIDGET_POSITION);

            setIfPresent(carrier, options, SHOW_COSMETICS_KEY, PlayerOptions.SHOW_COSMETIC_SLOTS);
            setIfPresent(carrier, options, SHOW_UNUSED_SLOTS_KEY, PlayerOptions.SHOW_UNUSED_SLOTS);

            setIfPresent(carrier, options, SHOW_GROUP_FILTER, PlayerOptions.SHOW_GROUP_FILTER);
//            setIfPresent(carrier, options, IS_GROUP_FILTERS_OPEN_KEY, AccessoriesPlayerOptions::isGroupFiltersOpen);
            setIfPresent(carrier, options, FILTERED_GROUPS_KEY, PlayerOptions.FILTERED_GROUPS);

            setIfPresent(carrier, options, SHOW_CRAFTING_GRID, PlayerOptions.SHOW_CRAFTING_GRID);
        }

        capability.clearCachedSlotModifiers();

        this.carrier = EMPTY;

        var cache = this.getLookupCache();

        if (cache != null) cache.clearCache();
    }

    private static <F> void setIfPresent(MapCarrierDecodable carrier, AccessoriesPlayerOptionsHolder options, KeyedEndec<F> keyedEndec, PlayerOption<F> option) {
        if (carrier.has(keyedEndec)) {
            options.setData(option, carrier.get(keyedEndec));
        }
    }

    @Override
    public void decode(MapCarrierDecodable carrier, SerializationContext context) {
        this.loadedFromTag = true;

        this.carrier = carrier;
    }

    private record ContainersAttribute(Map<String, AccessoriesContainer> slotContainers) implements SerializationAttribute.Instance {
        public static final SerializationAttribute.WithValue<ContainersAttribute> CONTAINERS = SerializationAttribute.withValue(Accessories.translationKey("containers"));

        @Override public SerializationAttribute attribute() { return CONTAINERS; }
        @Override public Object value() { return this; }
    }

    private record InvalidStacksAttribute(List<ItemStack> invalidStacks) implements SerializationAttribute.Instance {
        public static final SerializationAttribute.WithValue<InvalidStacksAttribute> INVALID_STACKS = SerializationAttribute.withValue(Accessories.translationKey("invalidStacks"));

        @Override public SerializationAttribute attribute() { return INVALID_STACKS; }
        @Override public Object value() { return this; }
    }

    private record EntityAttribute(LivingEntity livingEntity) implements SerializationAttribute.Instance{
        public static final SerializationAttribute.WithValue<EntityAttribute> ENTITY = SerializationAttribute.withValue("entity");

        @Override public SerializationAttribute attribute() { return ENTITY; }
        @Override public Object value() { return this;}
    }

    private class OwnerAccessibleReentrantLock extends ReentrantLock {
        @Override
        public Thread getOwner() {
            return super.getOwner();
        }
    }
}