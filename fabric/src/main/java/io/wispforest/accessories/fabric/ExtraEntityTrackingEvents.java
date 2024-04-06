package io.wispforest.accessories.fabric;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;

public class ExtraEntityTrackingEvents {

    /**
     * Variant of Fabric API's event but fired at the tail of the method instead of the head of the method
     */
    public static final Event<EntityTrackingEvents.StartTracking> POST_START_TRACKING = EventFactory.createArrayBacked(EntityTrackingEvents.StartTracking.class, callbacks -> (trackedEntity, player) -> {
        for (EntityTrackingEvents.StartTracking callback : callbacks) {
            callback.onStartTracking(trackedEntity, player);
        }
    });
}
