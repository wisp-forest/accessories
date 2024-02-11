package io.wispforest.accessories.client;

import com.mojang.blaze3d.vertex.VertexConsumer;

public class ModulatedVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final float[] color;

    public ModulatedVertexConsumer(VertexConsumer delegate, float[] color) {
        this.delegate = delegate;
        this.color = color;
    }

    @Override
    public VertexConsumer vertex(double x, double y, double z) {
        delegate.vertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        delegate.color(
                (int) (red * color[0]),
                (int) (green * color[1]),
                (int) (blue * color[2]),
                (int) (alpha * color[3])
        );
        return this;
    }

    @Override
    public VertexConsumer uv(float u, float v) {
        delegate.uv(u, v);
        return this;
    }

    @Override
    public VertexConsumer overlayCoords(int u, int v) {
        delegate.overlayCoords(u, v);
        return this;
    }

    @Override
    public VertexConsumer uv2(int u, int v) {
        delegate.uv2(u, v);
        return this;
    }

    @Override
    public VertexConsumer normal(float x, float y, float z) {
        delegate.normal(x, y, z);
        return this;
    }

    @Override
    public void endVertex() {
        delegate.endVertex();
    }

    @Override
    public void defaultColor(int defaultR, int defaultG, int defaultB, int defaultA) {
        delegate.defaultColor(defaultR, defaultG, defaultB, defaultA);
    }

    @Override
    public void unsetDefaultColor() {
        delegate.unsetDefaultColor();
    }
}