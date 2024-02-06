package io.wispforest.accessories.api.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * Mean Position of all the Vertices™ (MPOATV)
 */
public class MPOATVConstructingVertexConsumer implements VertexConsumer {
    public double minX = Double.MAX_VALUE;
    public double minY = Double.MAX_VALUE;
    public double minZ = Double.MAX_VALUE;
    public double maxX = -Double.MAX_VALUE;
    public double maxY = -Double.MAX_VALUE;
    public double maxZ = -Double.MAX_VALUE;
    public Vec3 meanPos = null;

    @Override
    @NotNull
    public VertexConsumer vertex(double x, double y, double z) {
        minX = Math.min(minX, x);
        maxX = Math.max(maxX, x);

        minY = Math.min(minY, y);
        maxY = Math.max(maxY, y);

        minZ = Math.min(minZ, z);
        maxZ = Math.max(maxZ, z);

        meanPos = new Vec3((minX + maxX) / 2, (minY + maxY) / 2, (minZ + maxZ) / 2);

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