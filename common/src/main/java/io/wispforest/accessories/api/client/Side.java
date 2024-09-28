package io.wispforest.accessories.api.client;

import io.wispforest.endec.Endec;
import net.minecraft.core.Direction;

/**
 * Class acting as a wrapper around {@link Direction} with easy
 * to understand names used within rendering
 */
public enum Side {
    BOTTOM(Direction.DOWN),
    TOP(Direction.UP),
    BACK(Direction.NORTH),
    FRONT(Direction.SOUTH),
    LEFT(Direction.WEST),
    RIGHT(Direction.EAST);

    public static final Endec<Side> ENDEC = Endec.forEnum(Side.class);

    public final Direction direction;

    Side(Direction direction) {
        this.direction = direction;
    }
}