package io.wispforest.accessories.api.slot;

import com.google.common.collect.ImmutableMap;
import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.impl.event.EventUtils;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Class uesd to register custom slots for specific mod use cases rather than a sharded area
 */
public class UniqueSlotHandling {

    private static final Map<String, List<String>> slotGrouped = new HashMap<>();

    /**
     * Main event used to register
     */
    public static final Event<InitializeSlotTypes> EVENT = EventUtils.createEventWithBus(InitializeSlotTypes.class, AccessoriesInternals::getBus, (bus, invokers) -> registrationFunc -> {
        slotGrouped.clear();

        BiFunction<ResourceLocation, Integer, SlotType> wrappedFunc = (location, integer) -> {
            var slotType = registrationFunc.apply(location, integer);

            slotGrouped.computeIfAbsent(location.getNamespace(), s -> new ArrayList<>())
                    .add(slotType.name());

            return slotType;
        };

        for (var invoker : invokers) {
            invoker.initTypes(wrappedFunc);
        }

        bus.ifPresent(eventBus -> eventBus.post(new InitializeSlotTypesEvent(wrappedFunc)));
    });

    public static Map<String, List<String>> getGroups() {
        return ImmutableMap.copyOf(slotGrouped);
    }

    public interface InitializeSlotTypes {
        void initTypes(BiFunction<ResourceLocation, Integer, SlotType> registrationFunc);
    }

    public static final class InitializeSlotTypesEvent extends net.neoforged.bus.api.Event {
        private final BiFunction<ResourceLocation, Integer, SlotType> registrationFunc;

        public InitializeSlotTypesEvent(BiFunction<ResourceLocation, Integer, SlotType> registrationFunc){
            this.registrationFunc = registrationFunc;
        }

        public SlotType register(ResourceLocation location, int slotAmount) {
            return registrationFunc.apply(location, slotAmount);
        }
    }
}
