package io.wispforest.accessories.api.client;

import net.minecraft.util.context.ContextKey;

public interface RenderStateStorage {

    default <T> T getStateData(ContextKey<T> key) {
        throw new IllegalStateException("Interface Stub method not implemented!");
    }

    default <T> boolean hasStateData(ContextKey<T> key)  {
        throw new IllegalStateException("Interface Stub method not implemented!");
    }

    default <T> void setStateData(ContextKey<T> key, T data)  {
        throw new IllegalStateException("Interface Stub method not implemented!");
    }

    default void clearExtraData() {
        throw new IllegalStateException("Interface Stub method not implemented!");
    }
}
