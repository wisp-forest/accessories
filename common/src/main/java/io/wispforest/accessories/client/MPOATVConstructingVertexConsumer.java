package io.wispforest.accessories.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import io.wispforest.accessories.client.gui.AccessoriesScreen;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * Mean Position of all the Vertices™ (MPOATV)
 */
public class MPOATVConstructingVertexConsumer implements VertexConsumer {

    private double minX = Double.MAX_VALUE;
    private double minY = Double.MAX_VALUE;
    private double minZ = Double.MAX_VALUE;
    private double maxX = -Double.MAX_VALUE;
    private double maxY = -Double.MAX_VALUE;
    private double maxZ = -Double.MAX_VALUE;

    public Vec3 meanPos = null;

    public boolean hasBeenClamped = false;

    @Override
    @NotNull
    public VertexConsumer vertex(double x, double y, double z) {
        var leeway = 10;

        boolean xIsGood = x >= AccessoriesScreen.SCISSOR_BOX.x - leeway && x <= AccessoriesScreen.SCISSOR_BOX.z + leeway;
        boolean yIsGood = y >= AccessoriesScreen.SCISSOR_BOX.y - leeway && y <= AccessoriesScreen.SCISSOR_BOX.w + leeway;

        if (xIsGood && yIsGood) {
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);

            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);

            minZ = Math.min(minZ, z);
            maxZ = Math.max(maxZ, z);
        } else {
            hasBeenClamped = true;
        }

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
    public void endVertex() {}

    @Override
    public void defaultColor(int defaultR, int defaultG, int defaultB, int defaultA) {}

    @Override
    public void unsetDefaultColor() {}

    public AABB getBoundingBox() {
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }
}