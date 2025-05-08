package io.wispforest.accessories.data.api;

import io.wispforest.endec.Endec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public interface SyncedDataLoader<D> {
    Endec<D> syncDataEndec();

    void onReceivedData(D data);

    @Nullable
    default Exception onReceivedDataUnsafe(Object data) {
        try {
            onReceivedData((D) data);
        } catch (Exception e) {
            return e;
        }

        return null;
    }

    ResourceLocation getLoaderId();

    D getServerData();
}
