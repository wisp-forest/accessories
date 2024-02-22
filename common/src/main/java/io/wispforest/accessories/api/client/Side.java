package io.wispforest.accessories.api.client;

import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;

public enum Side {
    BOTTOM(Direction.DOWN),
    TOP(Direction.UP),
    BACK(Direction.NORTH),
    FRONT(Direction.SOUTH),
    LEFT(Direction.WEST),
    RIGHT(Direction.EAST);

    public final Direction direction;

    Side(Direction direction) {
        this.direction = direction;
    }
}