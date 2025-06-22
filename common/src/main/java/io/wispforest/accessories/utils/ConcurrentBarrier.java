package io.wispforest.accessories.utils;

public interface ConcurrentBarrier extends AutoCloseable {
    void setSinking(boolean value);

    @Override
    default void close() {
        setSinking(false);
    }
}
