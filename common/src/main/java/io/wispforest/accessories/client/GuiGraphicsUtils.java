package io.wispforest.accessories.client;

import com.mojang.blaze3d.vertex.*;
import io.wispforest.owo.client.OwoClient;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.gui.GuiSpriteScaling;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.TriState;
import org.apache.logging.log4j.util.BiConsumer;
import org.apache.logging.log4j.util.TriConsumer;
import org.joml.Vector4f;

import java.util.List;
import java.util.function.Function;

public class GuiGraphicsUtils {

    //--

    public static void drawWithSpectrum(GuiGraphics ctx, int x, int y, int blitOffset, int width, int height, TextureAtlasSprite sprite, float alpha) {
        innerDrawWithSpectrum(ctx, sprite.atlasLocation(), x, x + width, y, y + height, blitOffset, sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1(), new Vector4f(alpha));
    }

    public static void drawWithSpectrum(GuiGraphics ctx, int x, int y, int blitOffset, int width, int height, TextureAtlasSprite sprite, Vector4f alphaValues) {
        innerDrawWithSpectrum(ctx, sprite.atlasLocation(), x, x + width, y, y + height, blitOffset, sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1(), alphaValues);
    }

    private static final Function<ResourceLocation, RenderType> SPECTRUM_GUI = Util.memoize(
            resourceLocation -> RenderType.create(
                    "spectrum_gui",
                    DefaultVertexFormat.POSITION_TEX_COLOR,
                    VertexFormat.Mode.QUADS,
                    786432,
                    RenderType.CompositeState.builder()
                            .setTextureState(new RenderStateShard.TextureStateShard(resourceLocation, TriState.FALSE, false))
                            .setShaderState(AccessoriesClient.SPECTRUM_PROGRAM.renderPhaseProgram())
                            .setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
                            .setDepthTestState(RenderType.LEQUAL_DEPTH_TEST)
                            .createCompositeState(false)
            )
    );

    // X: Top Left
    // Y: Top Right
    // Z: Bottom Left
    // W: Bottom Right
    private static void innerDrawWithSpectrum(GuiGraphics guiGraphics, ResourceLocation atlasLocation, int x1, int x2, int y1, int y2, int blitOffset, float minU, float maxU, float minV, float maxV,  Vector4f alphaValues) {
        var ctx = OwoUIDrawContext.of(guiGraphics);

        var matrix4f = ctx.pose().last().pose();

        var bufferBuilder = ctx.vertexConsumers().getBuffer(SPECTRUM_GUI.apply(atlasLocation));

        bufferBuilder.addVertex(matrix4f, (float)x1, (float)y1, (float)blitOffset).setUv(minU, minV).setColor(1.0f, 1.0f, 1.0f, alphaValues.x);
        bufferBuilder.addVertex(matrix4f, (float)x1, (float)y2, (float)blitOffset).setUv(minU, maxV).setColor(0, 1.0f, 1.0f, alphaValues.z);
        bufferBuilder.addVertex(matrix4f, (float)x2, (float)y2, (float)blitOffset).setUv(maxU, maxV).setColor(0, 1.0f, 1.0f, alphaValues.w);
        bufferBuilder.addVertex(matrix4f, (float)x2, (float)y1, (float)blitOffset).setUv(maxU, minV).setColor(1.0f, 1.0f, 1.0f, alphaValues.y);
    }

    public static void drawRectOutlineWithSpectrum(OwoUIDrawContext ctx, int x, int y, int z, int width, int height, float alpha, boolean vertical) {
        drawRectOutlineWithSpectrumWithoutRecord(ctx, x, y, z, width, height, alpha, vertical);
    }

    public static void drawRectOutlineWithSpectrumWithoutRecord(OwoUIDrawContext ctx, int x, int y, int z, int width, int height, float alpha, boolean vertical) {
        innerFill(ctx, x, y, x + width, y + 1, 0, alpha, !vertical);
        innerFill(ctx, x, y + height - 1, x + width, y + height, 0, alpha, !vertical);

        innerFill(ctx, x, y + 1, x + 1, y + height - 1, 0, alpha, vertical);
        innerFill(ctx, x + width - 1, y + 1, x + width, y + height - 1, 0, alpha, vertical);
    }

    private static final RenderType.CompositeRenderType HSV_GUI = RenderType.create(
            "hsv_gui",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            786432,
            RenderType.CompositeState.builder()
                    .setShaderState(OwoClient.HSV_PROGRAM.renderPhaseProgram())
                    .setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(RenderType.LEQUAL_DEPTH_TEST)
                    .createCompositeState(false)
    );

    private static void innerFill(GuiGraphics guiGraphics, int minX, int minY, int maxX, int maxY, int z, float alpha, boolean vertical) {
        var ctx = OwoUIDrawContext.of(guiGraphics);

        if (minX < maxX) {
            int i = minX;
            minX = maxX;
            maxX = i;
        }

        if (minY < maxY) {
            int i = minY;
            minY = maxY;
            maxY = i;
        }

        var vertexConsumer = ctx.vertexConsumers().getBuffer(HSV_GUI);

        var multiplier = (float) ((System.currentTimeMillis() / 20d % 360d) / 360d);

        var topValue = 1f - multiplier;
        var bottomValue = topValue; //0f + multiplier;

        var matrix4f = ctx.pose().last().pose();

        vertexConsumer.addVertex(matrix4f, (float)minX, (float)minY, (float)z).setColor(topValue, 1f, 1f, alpha);
        vertexConsumer.addVertex(matrix4f, (float)minX, (float)maxY, (float)z).setColor(vertical ? bottomValue : topValue, 1f, 1f, alpha);
        vertexConsumer.addVertex(matrix4f, (float)maxX, (float)maxY, (float)z).setColor(bottomValue, 1f, 1f, alpha);
        vertexConsumer.addVertex(matrix4f, (float)maxX, (float)minY, (float)z).setColor(vertical ? topValue : bottomValue, 1f, 1f, alpha);
    }
}
