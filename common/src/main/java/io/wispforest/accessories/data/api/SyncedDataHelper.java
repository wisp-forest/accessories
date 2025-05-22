package io.wispforest.accessories.data.api;

import io.wispforest.endec.Endec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface SyncedDataHelper<D> {

    static <D> SyncedDataHelper<D> of(ResourceLocation id, Endec<D> endec, Consumer<D> onReceived, Supplier<D> getDataSupplier, ResourceLocation ...dependencies){
        return new SyncedDataHelper<D>() {
            @Override public ResourceLocation getId() { return id; }
            @Override public Set<ResourceLocation> getDependencyIds() { return Set.of(dependencies); }

            @Override public Endec<D> syncDataEndec() { return endec; }

            @Override public D getServerData() { return getDataSupplier.get(); }
            @Override public void onReceivedData(D data) { onReceived.accept(data); }
        };
    }

    Endec<D> syncDataEndec();

    @ApiStatus.OverrideOnly
    void onReceivedData(D data);

    @ApiStatus.Internal
    @Nullable
    default Exception onReceivedDataUnsafe(Object data) {
        try {
            onReceivedData((D) data);
        } catch (Exception e) {
            return e;
        }

        return null;
    }

    ResourceLocation getId();

    Set<ResourceLocation> getDependencyIds();

    @ApiStatus.OverrideOnly
    D getServerData();
}
