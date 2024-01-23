package io.wispforest.accessories.api;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class AccessoriesAPIImpl implements AccessoriesAPI{

    private static final Map<String, UUID> CACHED_UUIDS = new HashMap<>();

    @Override
    public UUID getOrCreateSlotUUID(SlotType slotType, int index) {
        return CACHED_UUIDS.computeIfAbsent(
                slottedIdentifier(slotType, index),
                s -> UUID.nameUUIDFromBytes(s.getBytes())
        );
    }
}
