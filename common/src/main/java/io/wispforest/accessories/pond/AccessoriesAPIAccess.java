package io.wispforest.accessories.pond;

import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesHolder;

import java.util.Optional;

public interface AccessoriesAPIAccess {

    default Optional<AccessoriesCapability> accessoriesCapability() {
        throw new IllegalStateException("[AccessoriesAPIAccess]: Default interface method not implemented!");
    }

    default Optional<AccessoriesHolder> accessoriesHolder() {
        throw new IllegalStateException("[AccessoriesAPIAccess]: Default interface method not implemented!");
    }
}
