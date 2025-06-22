package io.wispforest.accessories.client.gui.utils;

import net.minecraft.util.Mth;
import org.joml.Vector3d;
import org.joml.Vector3f;

public record Line3d(Vector3d p1, Vector3d p2) {
    public Vector3f lerpPoint(double delta) {
        return new Vector3d(
                Mth.lerp(delta, p1().x, p2().x),
                Mth.lerp(delta, p1().y, p2().y),
                Mth.lerp(delta, p1().z, p2().z)
        ).get(new Vector3f());
    }
}
