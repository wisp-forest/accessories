package io.wispforest.accessories.pond;

import net.minecraft.client.model.geom.ModelPart;
import org.jetbrains.annotations.Nullable;

public interface ModelPartLoadingHelper {
    default void accessories$pushRoot(ModelPart root) {
        throw new IllegalStateException("Interface Method not overridden!");
    }

    @Nullable
    default ModelPart accessories$pollRoot() {
        throw new IllegalStateException("Interface Method not overridden!");
    }

    default void accessories$clearQueue() {
        throw new IllegalStateException("Interface Method not overridden!");
    }
}
