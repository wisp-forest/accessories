package io.wispforest.accessories.api.slot;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.wispforest.accessories.Accessories;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import org.apache.commons.lang3.function.TriFunction;

import java.util.*;

/**
 * Class used to construct Unique slots for specific mods with more ability
 * to restrict certain adjustments possible though datapack's if required
 */
public class UniqueSlotHandling {

    private static final Set<String> UNIQUE_SLOT_GROUPS = new HashSet<>();
    private static final Map<String, Set<EntityType<?>>> SLOT_TO_ENTITIES = new HashMap<>();

    private static final Map<String, ExtraProps> SLOT_TO_PROPERTY = new HashMap<>();

    public static Set<String> getGroups() {
        return ImmutableSet.copyOf(UNIQUE_SLOT_GROUPS);
    }

    public static void addGroup(String group) {
        UNIQUE_SLOT_GROUPS.add(group);
    }

    public static Map<String, Set<EntityType<?>>> getSlotToEntities() {
        return ImmutableMap.copyOf(SLOT_TO_ENTITIES);
    }

    public static boolean allowResizing(String slotType) {
        return SLOT_TO_PROPERTY.getOrDefault(slotType, ExtraProps.DEFAULT).allowResizing;
    }

    public static boolean isStrict(String slotType) {
        return SLOT_TO_PROPERTY.getOrDefault(slotType, ExtraProps.DEFAULT).strictMode;
    }

    public static boolean isUniqueSlot(String slotType) {
        return slotType.split(":").length > 1;
    }

    public static boolean isUniqueGroup(String group) {
        return UNIQUE_SLOT_GROUPS.add(group);
    }

    public static void gatherUniqueSlots(TriFunction<ResourceLocation, Integer, Collection<ResourceLocation>, SlotTypeReference> slotRegistration) {
        UNIQUE_SLOT_GROUPS.clear();
        SLOT_TO_ENTITIES.clear();

        UniqueSlotBuilderFactory eventRegistration = (location, amount) -> new UniqueSlotBuilder(location, amount, slotRegistration);

        EVENT.invoker().registerSlots(eventRegistration);
    }

    /**
     * Main event used to register unique slots for your mod
     */
    public static final Event<RegistrationCallback> EVENT = EventFactory.createArrayBacked(RegistrationCallback.class, (invokers) -> factory -> {
        for (var invoker : invokers) {
            invoker.registerSlots(factory);
        }
    });

    public interface RegistrationCallback {
        void registerSlots(UniqueSlotBuilderFactory factory);
    }

    public interface UniqueSlotBuilderFactory {
        UniqueSlotBuilder create(ResourceLocation location, int amount);
    }

    public static final class UniqueSlotBuilder {
        private final ResourceLocation location;
        private final int amount;
        private Collection<ResourceLocation> slotPredicates = List.of();
        private Collection<EntityType<?>> validTypes = Set.of();

        private boolean strictMode = true;
        private boolean allowResizing = false;

        private final TriFunction<ResourceLocation, Integer, Collection<ResourceLocation>, SlotTypeReference> slotRegistration;

        UniqueSlotBuilder(ResourceLocation location, int amount, TriFunction<ResourceLocation, Integer, Collection<ResourceLocation>, SlotTypeReference> slotRegistration){
            this.location = location;
            this.amount = amount;

            this.slotRegistration = slotRegistration;
        }

        public UniqueSlotBuilder slotPredicates(ResourceLocation ...locations) {
            this.slotPredicates = Set.of(locations);

            return this;
        }

        public UniqueSlotBuilder validTypes(EntityType<?> ...types) {
            this.validTypes = Set.of(types);

            return this;
        }

        public UniqueSlotBuilder strictMode(boolean value){
            this.strictMode = value;

            return this;
        }

        public UniqueSlotBuilder allowResizing(boolean value) {
            this.allowResizing = value;

            return this;
        }

        public SlotTypeReference build() {
            if(this.slotPredicates.isEmpty()) {
                this.slotPredicates = Set.of(Accessories.of("tag"));
            }

            var slotTypeRef = this.slotRegistration.apply(location, amount, slotPredicates);

            SLOT_TO_ENTITIES.put(slotTypeRef.slotName(), Set.copyOf(this.validTypes));

            SLOT_TO_PROPERTY.put(slotTypeRef.slotName(), new ExtraProps(this.allowResizing, this.strictMode));

            return slotTypeRef;
        }
    }

    private record ExtraProps(boolean allowResizing, boolean strictMode) {
        private static final ExtraProps DEFAULT = new ExtraProps(true, false);
    }
}
