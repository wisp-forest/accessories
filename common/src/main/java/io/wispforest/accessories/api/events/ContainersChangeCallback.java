package io.wispforest.accessories.api.events;

import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesContainer;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;

public interface ContainersChangeCallback {

    Event<ContainersChangeCallback> EVENT = EventFactory.createArrayBacked(ContainersChangeCallback.class,
            (invokers) -> (livingEntity, capability, changedContainers) -> {
                for (var invoker : invokers) {
                    invoker.onChange(livingEntity, capability, changedContainers);
                }
            }
    );

    void onChange(LivingEntity livingEntity, AccessoriesCapability capability, Map<AccessoriesContainer, Boolean> changedContainers);
}
