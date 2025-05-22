package io.wispforest.accessories.data.api;

import io.wispforest.endec.Endec;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

public interface SyncedDataLoaderExtended<D, E> extends SyncedDataLoader<D> {

    Endec<E> extraDataEndec();

    @ApiStatus.OverrideOnly
    void onReceivedExtraData(E data);

    @ApiStatus.Internal
    @Nullable
    default Exception onReceivedExtraDataUnsafe(Object data) {
        try {
            onReceivedExtraData((E) data);
        } catch (Exception e) {
            return e;
        }

        return null;
    }

    @ApiStatus.OverrideOnly
    E getServerExtraData();
}
