package io.wispforest.accessories.pond;

import org.jetbrains.annotations.Nullable;

public interface ScissorStackManipulation {

    default void accessories$renderWithoutAny(Runnable runnable) {
        accessories$renderWithoutEntries(runnable, null);
    }

    void accessories$renderWithoutEntries(Runnable runnable, @Nullable Integer levels);
}
