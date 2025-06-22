package io.wispforest.accessories.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.function.Function;

public class DrawUtils {

    public static void drawWithSpectrum(GuiGraphics ctx, int x, int y, int blitOffset, int width, int height, ResourceLocation texture, float alpha) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getGuiSprites().getSprite(texture);

        innerDrawWithSpectrum(ctx, sprite.atlasLocation(), x, x + width, y, y + height, blitOffset, sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1(), new Vector4f(alpha));
    }

    public static void drawWithSpectrum(GuiGraphics ctx, int x, int y, int blitOffset, int width, int height, TextureAtlasSprite sprite, float alpha) {
        innerDrawWithSpectrum(ctx, sprite.atlasLocation(), x, x + width, y, y + height, blitOffset, sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1(), new Vector4f(alpha));
    }

    public static void drawWithSpectrum(GuiGraphics ctx, int x, int y, int blitOffset, int width, int height, TextureAtlasSprite sprite, Vector4f alphaValues) {
        innerDrawWithSpectrum(ctx, sprite.atlasLocation(), x, x + width, y, y + height, blitOffset, sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1(), alphaValues);
    }

    // X: Top Left
    // Y: Top Right
    // Z: Bottom Left
    // W: Bottom Right
    private static void innerDrawWithSpectrum(GuiGraphics guiGraphics, ResourceLocation atlasLocation, int x1, int x2, int y1, int y2, int blitOffset, float minU, float maxU, float minV, float maxV,  Vector4f alphaValues) {
        var ctx = OwoUIDrawContext.of(guiGraphics);

        var matrix4f = ctx.pose().last().pose();

        var bufferBuilder = ctx.vertexConsumers().getBuffer(AccessoriesPipelines.SPECTRUM_GUI.apply(atlasLocation));

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

        var vertexConsumer = ctx.vertexConsumers().getBuffer(AccessoriesPipelines.HSV_GUI);

        var multiplier = (float) ((System.currentTimeMillis() / 20d % 360d) / 360d);

        var topValue = 1f - multiplier;
        var bottomValue = topValue; //0f + multiplier;

        var matrix4f = ctx.pose().last().pose();

        vertexConsumer.addVertex(matrix4f, (float)minX, (float)minY, (float)z).setColor(topValue, 1f, 1f, alpha);
        vertexConsumer.addVertex(matrix4f, (float)minX, (float)maxY, (float)z).setColor(vertical ? bottomValue : topValue, 1f, 1f, alpha);
        vertexConsumer.addVertex(matrix4f, (float)maxX, (float)maxY, (float)z).setColor(bottomValue, 1f, 1f, alpha);
        vertexConsumer.addVertex(matrix4f, (float)maxX, (float)minY, (float)z).setColor(vertical ? topValue : bottomValue, 1f, 1f, alpha);
    }

    public static void blitSprite(GuiGraphics context, ResourceLocation atlasLocation, int x, int y, int width, int height) {
        blitSprite(context, atlasLocation, x, y, width, height, -1);
    }

    public static void blitSprite(GuiGraphics context, ResourceLocation atlasLocation, int x, int y, int width, int height, int blitOffset) {
        context.blitSprite(RenderType::guiTextured, atlasLocation, x, y, width, height, blitOffset);
    }

    public static void blit(GuiGraphics context, ResourceLocation atlasLocation, int x, int y, int width, int height) {
        blit(context, atlasLocation, x, y, 0, 0, width, height, width, height);
    }

    public static void blit(GuiGraphics context, ResourceLocation atlasLocation, int x, int y, int uWidth, int vHeight, int textureWidth, int textureHeight) {
        blit(context, atlasLocation, x, y, 0, 0, uWidth, vHeight, textureWidth, textureHeight);
    }

    public static void blit(GuiGraphics context, ResourceLocation atlasLocation, int x, int y, float uOffset, float vOffset, int uWidth, int vHeight, int textureWidth, int textureHeight) {
        context.blit(RenderType::guiTextured, atlasLocation, x, y, uOffset, vOffset, uWidth, vHeight, uWidth, vHeight, textureWidth, textureHeight);
    }

    public static void blit(GuiGraphics context, boolean blend, ResourceLocation atlasLocation, int x, int y, float uOffset, float vOffset, int uWidth, int vHeight, int width, int height, int textureWidth, int textureHeight) {
        Function<ResourceLocation, RenderType> renderTypeGetter = blend
                ? RenderType::guiTextured
                : RenderType::guiTexturedOverlay;

        context.blit(renderTypeGetter, atlasLocation, x, y, uOffset, vOffset, uWidth, vHeight, width, height, textureWidth, textureHeight, -1);
    }

    public static void blitWithColor(GuiGraphics context, Color color, ResourceLocation atlasLocation, int x, int y, int width, int height) {
        context.blit(location -> AccessoriesPipelines.COLORED_GUI_TEXTURED.apply(color, location), atlasLocation, x, y, 0, 0, width, height, width, height, width, height);

        context.flush();
    }

    public static void blitSpriteWithColor(GuiGraphics context, TextureAtlasSprite sprite, int x, int y, int width, int height, Color color) {
        context.blitSprite(location -> AccessoriesPipelines.COLORED_GUI_TEXTURED.apply(color, location), sprite, x, y, width, height, color.argb());
    }

    public static void addToVertexBuffer(VertexConsumer buf, Vector3f pos, PoseStack.Pose pose, Vector3f normalVec) {
        buf.addVertex(pos)
                .setColor(255, 255, 255, 255)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                //.uv2(LightTexture.FULL_BLOCK)
                .setNormal(pose, normalVec.x, normalVec.y, normalVec.z);
                //.endVertex();
    }

    public static void addToVertexBuffer(VertexConsumer buf, Matrix4f pose, float x, float y, float z, int u, int v) {
        buf.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v);
    }
}
