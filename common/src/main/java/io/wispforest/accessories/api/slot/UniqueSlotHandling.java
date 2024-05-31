package io.wispforest.accessories.api.slot;

import com.google.common.collect.ImmutableMap;
import io.wispforest.accessories.Accessories;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import org.apache.commons.lang3.function.TriFunction;

import java.util.*;

/**
 * Class used to register custom slots for specific mod use cases rather than a sharded area
 */
public class UniqueSlotHandling {

    private static final Map<String, List<String>> slotGrouped = new HashMap<>();
    private static final Map<SlotTypeReference, Set<EntityType<?>>> slotToTypes = new HashMap<>();

    public static Map<String, List<String>> getGroups() {
        return ImmutableMap.copyOf(slotGrouped);
    }

    public static Map<SlotTypeReference, Set<EntityType<?>>> getSlotToTypes() {
        return ImmutableMap.copyOf(slotToTypes);
    }

    public static void gatherUniqueSlots(TriFunction<ResourceLocation, Integer, Collection<ResourceLocation>, SlotTypeReference> slotRegistration) {
        slotGrouped.clear();

        UniqueSlotRegistration eventRegistration = (location, integer, slotPredicates, types) -> {
            var slotType = slotRegistration.apply(location, integer, slotPredicates);

            slotGrouped.computeIfAbsent(location.getNamespace(), s -> new ArrayList<>())
                    .add(location.toString());

            slotToTypes.put(slotType, Set.of(types));

            return slotType;
        };

        EVENT.invoker().registerSlots(eventRegistration);
    }

    /**
     * Main event used to register unique slots for your mod
     */
    public static final Event<RegistrationCallback> EVENT = EventFactory.createArrayBacked(RegistrationCallback.class, (invokers) -> registrationFunction -> {
        for (var invoker : invokers) {
            invoker.registerSlots(registrationFunction);
        }
    });

    public interface RegistrationCallback {
        void registerSlots(UniqueSlotRegistration registration);
    }

    public interface UniqueSlotRegistration {
        default SlotTypeReference registerSlot(ResourceLocation location, int amount, EntityType<?> ...types) {
            return registerSlot(location, amount, Set.of(Accessories.of("tag")), types);
        }

        default SlotTypeReference registerSlot(ResourceLocation location, int amount, ResourceLocation slotPredicate, EntityType<?> ...types) {
            return registerSlot(location, amount, Set.of(slotPredicate), types);
        }

        SlotTypeReference registerSlot(ResourceLocation location, int amount, Collection<ResourceLocation> slotPredicates, EntityType<?> ...types);
    }
}
