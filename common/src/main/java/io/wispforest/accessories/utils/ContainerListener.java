package io.wispforest.accessories.utils;

import net.minecraft.world.Container;

@FunctionalInterface
public interface ContainerListener {
    void containerChanged(Container container);
}
