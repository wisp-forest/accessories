package io.wispforest.accessories.api.slot;

import com.google.common.collect.ImmutableMap;
import com.mojang.logging.LogUtils;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.Accessory;
import io.wispforest.accessories.data.SlotTypeLoader;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;

import java.util.*;

/**
 * Utilities for adding custom slots that are rendered outside the accessories screen with options for restricting
 * modification via data pack.
 */
public class UniqueSlotHandling {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Event for registration of unique slots.
     */
    public static final Event<RegistrationCallback> EVENT = EventFactory.createArrayBacked(RegistrationCallback.class, (invokers) -> factory -> {
        for (var invoker : invokers) invoker.registerSlots(factory);
    });

    public static boolean isUniqueSlot(String slotType) {
        return slotType.split(":").length > 1;
    }

    public static boolean isUniqueGroup(String group, boolean isClient) {
        return (isClient ? GROUPS_CLIENT : GROUPS_SERVER).contains(group);
    }

    public interface RegistrationCallback {
        /**
         * Invoked when unique slots are being loaded.
         *
         * @param factory the factory for registering unique slots
         */
        void registerSlots(UniqueSlotBuilderFactory factory);
    }

    public interface UniqueSlotBuilderFactory {
        /**
         * Starts building a new unique slot.
         */
        UniqueSlotBuilder create(ResourceLocation location, int amount);
    }

    /**
     * Builder Object used to create unique slots with the base required info and some other extra properties that can
     * be adjusted.
     */
    public interface UniqueSlotBuilder {
        /**
         * Sets the unique slot's slot predicates.
         * <p>
         * By default, the tag-based slot predicate is used.
         */
        UniqueSlotBuilder slotPredicates(ResourceLocation... locations);

        /**
         * Sets the list of entity types that will have this slot.
         */
        UniqueSlotBuilder validTypes(EntityType<?>... types);

        /**
         * Controls whether modification of this slot via a data pack is disallowed.
         */
        UniqueSlotBuilder strictMode(boolean value);

        /**
         * Controls whether this slot can be resized later (e.g. with a data pack or attribute)
         */
        UniqueSlotBuilder allowResizing(boolean value);

        /**
         * Controls whether accessories can be equipped from use into this slot.
         * <p>
         * A value of {@code false} overrides {@link Accessory#canEquipFromUse(ItemStack)}.
         */
        UniqueSlotBuilder allowEquipFromUse(boolean value);

        /**
         * Builds and registers the unique slot.
         * @return a reference to the unique slot type
         */
        SlotTypeReference build();
    }

    //--

    @ApiStatus.Internal
    public static void gatherUniqueSlots(TriFunction<ResourceLocation, Integer, Collection<ResourceLocation>, SlotTypeReference> slotRegistration) {
        GROUPS_SERVER.clear();
        SLOT_TO_ENTITIES.clear();

        UniqueSlotBuilderFactory eventRegistration = (location, amount) -> new ServerUniqueSlotBuilder(location, amount, slotRegistration);

        EVENT.invoker().registerSlots(eventRegistration);
    }

    @ApiStatus.Internal
    public static void buildClientSlotReferences() {
        UniqueSlotBuilderFactory eventRegistration = (location, amount) -> new UniqueSlotBuilder() {
            @Override public UniqueSlotBuilder slotPredicates(ResourceLocation... locations) { return this; }
            @Override public UniqueSlotBuilder validTypes(EntityType<?>... types) { return this; }
            @Override public UniqueSlotBuilder strictMode(boolean value) { return this; }
            @Override public UniqueSlotBuilder allowResizing(boolean value) { return this; }
            @Override public UniqueSlotBuilder allowEquipFromUse(boolean value) { return this; }

            @Override
            public SlotTypeReference build() {
                var name = location.toString();

                var slotType = SlotTypeLoader.INSTANCE.getSlotTypes(true).get(name);

                if(slotType == null) {
                    LOGGER.error("Unable to get the given unique slot as the slot has been not been synced to the client! [Name: {}]", name);
                }

                return new SlotTypeReference(name);
            }
        };

        EVENT.invoker().registerSlots(eventRegistration);
    }

    private static final class ServerUniqueSlotBuilder implements UniqueSlotBuilder {
        private final ResourceLocation location;
        private final int amount;
        private Set<ResourceLocation> slotPredicates = Set.of(Accessories.of("tag"));
        private Set<EntityType<?>> validTypes = Set.of();

        private boolean strictMode = true;
        private boolean allowResizing = false;
        private boolean allowEquipFromUse = true;

        private final TriFunction<ResourceLocation, Integer, Collection<ResourceLocation>, SlotTypeReference> slotRegistration;

        ServerUniqueSlotBuilder(ResourceLocation location, int amount, TriFunction<ResourceLocation, Integer, Collection<ResourceLocation>, SlotTypeReference> slotRegistration){
            this.location = location;
            this.amount = amount;

            this.slotRegistration = slotRegistration;
        }

        @Override
        public ServerUniqueSlotBuilder slotPredicates(ResourceLocation... locations) {
            this.slotPredicates = Set.of(locations);

            return this;
        }

        @Override
        public ServerUniqueSlotBuilder validTypes(EntityType<?>... types) {
            this.validTypes = Set.of(types);

            return this;
        }

        @Override
        public ServerUniqueSlotBuilder strictMode(boolean value) {
            this.strictMode = value;

            return this;
        }

        @Override
        public ServerUniqueSlotBuilder allowResizing(boolean value) {
            this.allowResizing = value;

            return this;
        }

        @Override
        public ServerUniqueSlotBuilder allowEquipFromUse(boolean value) {
            this.allowEquipFromUse = value;

            return this;
        }

        @Override
        public SlotTypeReference build() {
            var slotTypeRef = this.slotRegistration.apply(location, amount, slotPredicates);

            SLOT_TO_ENTITIES.put(slotTypeRef.slotName(), Set.copyOf(this.validTypes));

            ExtraSlotTypeProperties.getProperties(false)
                    .put(slotTypeRef.slotName(), new ExtraSlotTypeProperties(this.allowResizing, this.strictMode, this.allowEquipFromUse));

            return slotTypeRef;
        }
    }

    private static final Map<String, Set<EntityType<?>>> SLOT_TO_ENTITIES = new HashMap<>();

    private static final Set<String> GROUPS_SERVER = new HashSet<>();
    private static final Set<String> GROUPS_CLIENT = new HashSet<>();

    @ApiStatus.Internal
    public static Map<String, Set<EntityType<?>>> getSlotToEntities() {
        return ImmutableMap.copyOf(SLOT_TO_ENTITIES);
    }

    @ApiStatus.Internal
    public static void addGroup(String group) {
        GROUPS_SERVER.add(group);
    }

    @ApiStatus.Internal
    public static Set<String> getGroups(boolean isClient) {
        return isClient ? GROUPS_CLIENT : GROUPS_SERVER;
    }

    @ApiStatus.Internal
    public static void setClientGroups(Set<String> set) {
        GROUPS_CLIENT.clear();
        GROUPS_CLIENT.addAll(set);
    }
}
