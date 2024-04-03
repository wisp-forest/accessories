package io.wispforest.accessories.pond;

import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesHolder;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public interface AccessoriesAPIAccess {

    @Nullable
    default AccessoriesCapability accessoriesCapability() {
        throw new IllegalStateException("[AccessoriesAPIAccess]: Default interface method not implemented!");
    }

    default AccessoriesHolder accessoriesHolder() {
        throw new IllegalStateException("[AccessoriesAPIAccess]: Default interface method not implemented!");
    }
}
