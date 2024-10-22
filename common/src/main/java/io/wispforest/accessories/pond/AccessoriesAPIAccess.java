package io.wispforest.accessories.pond;

import io.wispforest.accessories.api.AccessoriesCapability;
import org.jetbrains.annotations.Nullable;

public interface AccessoriesAPIAccess {

    @Nullable
    default AccessoriesCapability accessoriesCapability() {
        throw new IllegalStateException("[AccessoriesAPIAccess]: Default interface method not implemented!");
    }
}
