package io.wispforest.accessories.api.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * Mean Position of all the Vertices™ (MPOATV)
 */
public class MPOATVConstructingVertexConsumer implements VertexConsumer {
    public Vec3 meanPos = null;

    @Override
    @NotNull
    public VertexConsumer vertex(double x, double y, double z) {
        if (meanPos == null) {
            meanPos = new Vec3(x, y, z);
        } else {
            meanPos = new Vec3((meanPos.x + x) / 2, (meanPos.y + y) / 2, (meanPos.z + z) / 2);
        }

        return this;
    }

    @Override
    @NotNull
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        return this;
    }

    @Override
    @NotNull
    public VertexConsumer uv(float u, float v) {
        return this;
    }

    @Override
    @NotNull
    public VertexConsumer overlayCoords(int u, int v) {
        return this;
    }

    @Override
    @NotNull
    public VertexConsumer uv2(int u, int v) {
        return this;
    }

    @Override
    @NotNull
    public VertexConsumer normal(float x, float y, float z) {
        return this;
    }

    @Override
    public void endVertex() {

    }

    @Override
    public void defaultColor(int defaultR, int defaultG, int defaultB, int defaultA) {

    }

    @Override
    public void unsetDefaultColor() {

    }
}