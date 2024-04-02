package io.wispforest.accessories.api.slot;

import com.google.common.collect.ImmutableMap;
import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.impl.event.EventUtils;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Class uesd to register custom slots for specific mod use cases rather than a sharded area
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

    /**
     * Main event used to register
     */
    public static final Event<RegistrationCallback> EVENT = EventUtils.createEventWithBus(RegistrationCallback.class, AccessoriesInternals::getBus, (bus, invokers) -> registrationFunc -> {
        slotGrouped.clear();

        UniqueSlotRegistration registration = (location, integer, slotPredicate, types) -> {
            var slotType = registrationFunc.registerSlot(location, integer, slotPredicate);

            slotGrouped.computeIfAbsent(location.getNamespace(), s -> new ArrayList<>())
                    .add(location.toString());

            slotToTypes.put(slotType, Set.of(types));

            return slotType;
        };

        for (var invoker : invokers) {
            invoker.registerSlots(registration);
        }

        bus.ifPresent(eventBus -> eventBus.post(new InitializeSlotTypesEvent(registration)));
    });

    public static final class InitializeSlotTypesEvent extends net.neoforged.bus.api.Event implements UniqueSlotRegistration {
        private final UniqueSlotRegistration registration;

        public InitializeSlotTypesEvent(UniqueSlotRegistration registration){
            this.registration = registration;
        }

        @Override
        public SlotTypeReference registerSlot(ResourceLocation location, int amount, @Nullable ResourceLocation slotPredicate, EntityType<?> ...types) {
            return registration.registerSlot(location, amount, slotPredicate, types);
        }
    }

    public interface RegistrationCallback {
        void registerSlots(UniqueSlotRegistration registration);
    }

    public interface UniqueSlotRegistration {
        default SlotTypeReference registerSlot(ResourceLocation location, int amount, EntityType<?> ...types) {
            return registerSlot(location, amount, null, types);
        }

        SlotTypeReference registerSlot(ResourceLocation location, int amount, @Nullable ResourceLocation slotPredicate, EntityType<?> ...types);
    }

}
