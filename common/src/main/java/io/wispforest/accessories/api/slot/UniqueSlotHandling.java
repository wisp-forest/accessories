package io.wispforest.accessories.api.slot;

import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.impl.event.EventUtils;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class UniqueSlotHandling {

    private static final Map<String, List<String>> slotGrouped = new HashMap<>();

    public static final Event<InitializeSlotTypes> EVENT = EventUtils.createEventWithBus(InitializeSlotTypes.class, AccessoriesInternals::getBus, (bus, invokers) -> registrationFunc -> {
        slotGrouped.clear();

        for (var invoker : invokers) {
            invoker.initTypes((location, integer) -> {
                var slotType = registrationFunc.apply(location, integer);

                slotGrouped.computeIfAbsent(location.getPath(), s -> new ArrayList<>())
                        .add(slotType.name());

                return slotType;
            });
        }

        bus.ifPresent(eventBus -> eventBus.post(new InitializeSlotTypesEvent(registrationFunc)));
    });

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
