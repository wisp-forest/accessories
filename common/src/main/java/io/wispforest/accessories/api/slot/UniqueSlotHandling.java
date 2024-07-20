package io.wispforest.accessories.api.slot;

import com.google.common.collect.ImmutableMap;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.Accessory;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.ApiStatus;

import java.util.*;

/**
 * Utilities for adding custom slots that are rendered outside the accessories screen with options for restricting
 * modification via data pack.
 */
public class UniqueSlotHandling {

    /**
     * Event for registration of unique slots.
     */
    public static final Event<RegistrationCallback> EVENT = EventFactory.createArrayBacked(RegistrationCallback.class, (invokers) -> factory -> {
        for (var invoker : invokers) {
            invoker.registerSlots(factory);
        }
    });

    @ApiStatus.Internal
    public static void gatherUniqueSlots(TriFunction<ResourceLocation, Integer, Collection<ResourceLocation>, SlotTypeReference> slotRegistration) {
        GROUPS_SERVER.clear();
        SLOT_TO_ENTITIES.clear();

        UniqueSlotBuilderFactory eventRegistration = (location, amount) -> new UniqueSlotBuilder(location, amount, slotRegistration);

        EVENT.invoker().registerSlots(eventRegistration);
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
     * be adjusted for such.
     */
    public static final class UniqueSlotBuilder {
        private final ResourceLocation location;
        private final int amount;
        private Set<ResourceLocation> slotPredicates = Set.of(Accessories.of("tag"));
        private Set<EntityType<?>> validTypes = Set.of();

        private boolean strictMode = true;
        private boolean allowResizing = false;
        private boolean allowEquipFromUse = false;

        private final TriFunction<ResourceLocation, Integer, Collection<ResourceLocation>, SlotTypeReference> slotRegistration;

        UniqueSlotBuilder(ResourceLocation location, int amount, TriFunction<ResourceLocation, Integer, Collection<ResourceLocation>, SlotTypeReference> slotRegistration){
            this.location = location;
            this.amount = amount;

            this.slotRegistration = slotRegistration;
        }

        /**
         * Sets the unique slot's slot predicates.
         * <p>
         * By default, the tag-based slot predicate is used.
         */
        public UniqueSlotBuilder slotPredicates(ResourceLocation... locations) {
            this.slotPredicates = Set.of(locations);

            return this;
        }

        /**
         * Sets the list of entity types that will have this slot.
         */
        public UniqueSlotBuilder validTypes(EntityType<?>... types) {
            this.validTypes = Set.of(types);

            return this;
        }

        /**
         * Controls whether modification of this slot via a data pack is disallowed.
         */
        public UniqueSlotBuilder strictMode(boolean value) {
            this.strictMode = value;

            return this;
        }

        /**
         * Controls whether this slot can be resized later (e.g. with a data pack or attribute)
         */
        public UniqueSlotBuilder allowResizing(boolean value) {
            this.allowResizing = value;

            return this;
        }

        /**
         * Controls whether accessories can be equipped from use into this slot.
         * <p>
         * A value of {@code false} overrides {@link Accessory#canEquipFromUse(ItemStack, SlotReference)}.
         */
        public UniqueSlotBuilder allowEquipFromUse(boolean value) {
            this.allowEquipFromUse = value;

            return this;
        }

        /**
         * Builds and registers the unique slot.
         * @return a reference to the unique slot type
         */
        public SlotTypeReference build() {
            var slotTypeRef = this.slotRegistration.apply(location, amount, slotPredicates);

            SLOT_TO_ENTITIES.put(slotTypeRef.slotName(), Set.copyOf(this.validTypes));

            ExtraSlotTypeProperties.getProperties(false)
                    .put(slotTypeRef.slotName(), new ExtraSlotTypeProperties(this.allowResizing, this.strictMode, this.allowEquipFromUse));

            return slotTypeRef;
        }
    }

    //--

    private static final Map<String, Set<EntityType<?>>> SLOT_TO_ENTITIES = new HashMap<>();

    private static final Set<String> GROUPS_SERVER = new HashSet<>();
    private static final Set<String> GROUPS_CLIENT = new HashSet<>();

    public static boolean isUniqueSlot(String slotType) {
        return slotType.split(":").length > 1;
    }

    public static boolean isUniqueGroup(String group, boolean isClient) {
        return (isClient ? GROUPS_CLIENT : GROUPS_SERVER).contains(group);
    }

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
