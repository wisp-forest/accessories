package io.wispforest.accessories.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;
import org.joml.Vector4f;

public record BlitSpectrumRenderState(
    RenderPipeline pipeline,
    TextureSetup textureSetup,
    Matrix3x2f pose,
    int x0,
    int y0,
    int x1,
    int y1,
    float u0,
    float u1,
    float v0,
    float v1,
    Vector4f alphaValues,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
    public BlitSpectrumRenderState(
        RenderPipeline pipeline,
        TextureSetup textureSetup,
        Matrix3x2f pose,
        int x0,
        int y0,
        int x1,
        int y1,
        float u0,
        float u1,
        float v0,
        float v1,
        Vector4f alphaValues,
        @Nullable ScreenRectangle scissorArea
    ) {
        this(pipeline, textureSetup, pose, x0, y0, x1, y1, u0, u1, v0, v1, alphaValues, scissorArea, getBounds(x0, y0, x1, y1, pose, scissorArea));
    }

    @Override
    public void buildVertices(VertexConsumer vertexConsumer) {
        vertexConsumer.addVertexWith2DPose(this.pose(), this.x0(), this.y0()).setUv(this.u0(), this.v0()).setColor(1.0f, 1.0f, 1.0f, alphaValues.x);
        vertexConsumer.addVertexWith2DPose(this.pose(), this.x0(), this.y1()).setUv(this.u0(), this.v1()).setColor(0, 1.0f, 1.0f, alphaValues.z);
        vertexConsumer.addVertexWith2DPose(this.pose(), this.x1(), this.y1()).setUv(this.u1(), this.v1()).setColor(0, 1.0f, 1.0f, alphaValues.w);
        vertexConsumer.addVertexWith2DPose(this.pose(), this.x1(), this.y0()).setUv(this.u1(), this.v0()).setColor(1.0f, 1.0f, 1.0f, alphaValues.y);
    }

    @Nullable
    private static ScreenRectangle getBounds(int i, int j, int k, int l, Matrix3x2f matrix3x2f, @Nullable ScreenRectangle screenRectangle) {
        ScreenRectangle screenRectangle2 = new ScreenRectangle(i, j, k - i, l - j).transformMaxBounds(matrix3x2f);
        return screenRectangle != null ? screenRectangle.intersection(screenRectangle2) : screenRectangle2;
    }
}
