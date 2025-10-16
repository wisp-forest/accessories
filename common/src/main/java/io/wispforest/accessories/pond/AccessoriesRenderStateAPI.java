package io.wispforest.accessories.pond;

import io.wispforest.accessories.api.AccessoriesStorageLookup;
import io.wispforest.accessories.api.client.AccessoriesRenderStateKeys;
import io.wispforest.accessories.api.client.RenderStateStorage;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface AccessoriesRenderStateAPI extends RenderStateStorage {
    @Nullable
    default AccessoriesStorageLookup getStorageLookup() {
        return getStateData(AccessoriesRenderStateKeys.STORAGE_LOOKUP);
    }

    default UUID getEntityUUIDForState() {
        return getStateData(AccessoriesRenderStateKeys.ENTITY_UUID);
    }

    default float getEntityPartialTicksForState() {
        return getStateData(AccessoriesRenderStateKeys.PARTIAL_TICKS);
    }

    default int getEntityIdForState() {
        return getStateData(AccessoriesRenderStateKeys.ENTITY_ID);
    }
}
