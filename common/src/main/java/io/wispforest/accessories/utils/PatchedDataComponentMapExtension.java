package io.wispforest.accessories.utils;

import net.minecraft.core.component.PatchedDataComponentMap;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface PatchedDataComponentMapExtension {
    void startCheckingForChanges();

    boolean hasChanged();

    void endCheckingForChanges();

    default boolean hasChangedAndEndChecking() {
        var bl = hasChanged();

        endCheckingForChanges();

        return bl;
    }
}
