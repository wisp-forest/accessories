package io.wispforest.accessories.api.slot;

import com.google.common.collect.ImmutableMap;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.data.SlotTypeLoader;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.ApiStatus;

import java.util.*;

/**
 * Class used to construct Unique slots for specific mods with more ability
 * to restrict certain adjustments possible though datapack's if required
 */
public class UniqueSlotHandling {

    /**
     * Main event used to register unique slots for your mod in which is called on loading call of {@link SlotTypeLoader#apply}
     */
    public static final Event<RegistrationCallback> EVENT = EventFactory.createArrayBacked(RegistrationCallback.class, (invokers) -> factory -> {
        for (var invoker : invokers) {
            invoker.registerSlots(factory);
        }
    });

    public static void gatherUniqueSlots(TriFunction<ResourceLocation, Integer, Collection<ResourceLocation>, SlotTypeReference> slotRegistration) {
        GROUPS_SERVER.clear();
        SLOT_TO_ENTITIES.clear();

        UniqueSlotBuilderFactory eventRegistration = (location, amount) -> new UniqueSlotBuilder(location, amount, slotRegistration);

        EVENT.invoker().registerSlots(eventRegistration);
    }

    public interface RegistrationCallback {
        void registerSlots(UniqueSlotBuilderFactory factory);
    }

    /**
     * Util interface used within {@link RegistrationCallback} to allow for creating a new {@link UniqueSlotBuilder}
     */
    public interface UniqueSlotBuilderFactory {
        UniqueSlotBuilder create(ResourceLocation location, int amount);
    }

    /**
     * Builder Object used to create unique slots with the base required info and some other extra properies that can
     * be adjusted for such.
     */
    public static final class UniqueSlotBuilder {
        private final ResourceLocation location;
        private final int amount;
        private Collection<ResourceLocation> slotPredicates = List.of();
        private Collection<EntityType<?>> validTypes = Set.of();

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
         * Adds the given slot predicates as valid methods to check for if a given accessory can be equipped
         */
        public UniqueSlotBuilder slotPredicates(ResourceLocation ...locations) {
            this.slotPredicates = Set.of(locations);

            return this;
        }

        /**
         * Allows the given entity types to have the slot as a valid container
         */
        public UniqueSlotBuilder validTypes(EntityType<?> ...types) {
            this.validTypes = Set.of(types);

            return this;
        }

        /**
         * Indicates that the given slot added can not be adjusted though datapack method
         */
        public UniqueSlotBuilder strictMode(boolean value){
            this.strictMode = value;

            return this;
        }

        /**
         * Prevents the given slot from being resized in any way from the given size set on builder creation
         */
        public UniqueSlotBuilder allowResizing(boolean value) {
            this.allowResizing = value;

            return this;
        }

        /**
         * Allows the ability to toggle if the given slot as a whole allows for equipping from use
         */
        public UniqueSlotBuilder allowEquipFromUse(boolean value) {
            this.allowEquipFromUse = value;

            return this;
        }

        public SlotTypeReference build() {
            if(this.slotPredicates.isEmpty()) {
                this.slotPredicates = Set.of(Accessories.of("tag"));
            }

            var slotTypeRef = this.slotRegistration.apply(location, amount, slotPredicates);

            SLOT_TO_ENTITIES.put(slotTypeRef.slotName(), Set.copyOf(this.validTypes));

            ExtraSlotTypeProperties.getPropertiess(false)
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
