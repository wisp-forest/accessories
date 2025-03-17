package io.wispforest.accessories.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import io.wispforest.accessories.mixin.client.GuiGraphicsAccessor;
import io.wispforest.owo.client.OwoClient;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import io.wispforest.owo.util.pond.OwoTessellatorExtension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.gui.GuiSpriteScaling;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.util.BiConsumer;
import org.apache.logging.log4j.util.TriConsumer;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.List;

public class GuiGraphicsUtils {

    public static void blitSpriteBatched(GuiGraphics guiGraphics, ResourceLocation sprite, int x, int y, int width, int height) {
        blitSpriteBatched(guiGraphics, sprite, x, y, 0, width, height);
    }

    public static void blitSpriteBatched(GuiGraphics guiGraphics, ResourceLocation sprite, int x, int y, int blitOffset, int width, int height) {
        var sprites = Minecraft.getInstance().getGuiSprites();

        TextureAtlasSprite textureAtlasSprite = sprites.getSprite(sprite);
        GuiSpriteScaling guiSpriteScaling = sprites.getSpriteScaling(textureAtlasSprite);

        if (guiSpriteScaling instanceof GuiSpriteScaling.Stretch) {
            ((GuiGraphicsAccessor) guiGraphics).callBlitSprite(textureAtlasSprite, x, y, blitOffset, width, height);
        } else if (guiSpriteScaling instanceof GuiSpriteScaling.Tile tile) {
            blitTiledSpriteBatched(guiGraphics, textureAtlasSprite, x, y, blitOffset, width, height, 0, 0, tile.width(), tile.height(), tile.width(), tile.height());
        } else if (guiSpriteScaling instanceof GuiSpriteScaling.NineSlice nineSlice) {
            blitNineSlicedSpriteBatched(guiGraphics, textureAtlasSprite, nineSlice, x, y, blitOffset, width, height);
        }
    }

    private static void blitNineSlicedSpriteBatched(GuiGraphics guiGraphics, TextureAtlasSprite sprite, GuiSpriteScaling.NineSlice nineSlice, int x, int y, int blitOffset, int width, int height) {
        GuiSpriteScaling.NineSlice.Border border = nineSlice.border();

        int i = Math.min(border.left(), width / 2);
        int j = Math.min(border.right(), width / 2);
        int k = Math.min(border.top(), height / 2);
        int l = Math.min(border.bottom(), height / 2);

        batched(guiGraphics, sprite.atlasLocation(), (bufferBuilder, poseStack) -> {
            if (width == nineSlice.width() && height == nineSlice.height()) {
                blitSprite(bufferBuilder, poseStack, sprite, nineSlice.width(), nineSlice.height(), 0, 0, x, y, blitOffset, width, height);
            } else if (height == nineSlice.height()) {
                blitSprite(bufferBuilder, poseStack, sprite, nineSlice.width(), nineSlice.height(), 0, 0, x, y, blitOffset, i, height);
                blitTiledSprite(bufferBuilder, poseStack, sprite, x + i, y, blitOffset, width - j - i, height, i, 0, nineSlice.width() - j - i, nineSlice.height(), nineSlice.width(), nineSlice.height());
                blitSprite(bufferBuilder, poseStack, sprite, nineSlice.width(), nineSlice.height(), nineSlice.width() - j, 0, x + width - j, y, blitOffset, j, height);
            } else if (width == nineSlice.width()) {
                blitSprite(bufferBuilder, poseStack, sprite, nineSlice.width(), nineSlice.height(), 0, 0, x, y, blitOffset, width, k);
                blitTiledSprite(bufferBuilder, poseStack, sprite, x, y + k, blitOffset, width, height - l - k, 0, k, nineSlice.width(), nineSlice.height() - l - k, nineSlice.width(), nineSlice.height());
                blitSprite(bufferBuilder, poseStack, sprite, nineSlice.width(), nineSlice.height(), 0, nineSlice.height() - l, x, y + height - l, blitOffset, width, l);
            } else {
                blitSprite(bufferBuilder, poseStack, sprite, nineSlice.width(), nineSlice.height(), 0, 0, x, y, blitOffset, i, k);
                blitTiledSprite(bufferBuilder, poseStack, sprite, x + i, y, blitOffset, width - j - i, k, i, 0, nineSlice.width() - j - i, k, nineSlice.width(), nineSlice.height());
                blitSprite(bufferBuilder, poseStack, sprite, nineSlice.width(), nineSlice.height(), nineSlice.width() - j, 0, x + width - j, y, blitOffset, j, k);
                blitSprite(bufferBuilder, poseStack, sprite, nineSlice.width(), nineSlice.height(), 0, nineSlice.height() - l, x, y + height - l, blitOffset, i, l);
                blitTiledSprite(bufferBuilder, poseStack, sprite, x + i, y + height - l, blitOffset, width - j - i, l, i, nineSlice.height() - l, nineSlice.width() - j - i, l, nineSlice.width(), nineSlice.height());
                blitSprite(bufferBuilder, poseStack, sprite, nineSlice.width(), nineSlice.height(), nineSlice.width() - j, nineSlice.height() - l, x + width - j, y + height - l, blitOffset, j, l);
                blitTiledSprite(bufferBuilder, poseStack, sprite, x, y + k, blitOffset, i, height - l - k, 0, k, i, nineSlice.height() - l - k, nineSlice.width(), nineSlice.height());
                blitTiledSprite(bufferBuilder, poseStack, sprite, x + i, y + k, blitOffset, width - j - i, height - l - k, i, k, nineSlice.width() - j - i, nineSlice.height() - l - k, nineSlice.width(), nineSlice.height());
                blitTiledSprite(bufferBuilder, poseStack, sprite, x + width - j, y + k, blitOffset, i, height - l - k, nineSlice.width() - j, k, j, nineSlice.height() - l - k, nineSlice.width(), nineSlice.height());
            }
        });
    }

    private static void blitTiledSpriteBatched(GuiGraphics guiGraphics, TextureAtlasSprite sprite, int x, int y, int blitOffset, int width, int height, int i, int j, int spriteWidth, int spriteHeight, int nineSliceWidth, int nineSliceHeight) {
        batched(guiGraphics, sprite.atlasLocation(), (bufferBuilder, poseStack) -> {
            if (width <= 0 || height <= 0) return;

            if (spriteWidth <= 0 || spriteHeight <= 0) {
                throw new IllegalArgumentException("Tiled sprite texture size must be positive, got " + spriteWidth + "x" + spriteHeight);
            }

            for(int k = 0; k < width; k += spriteWidth) {
                int l = Math.min(spriteWidth, width - k);

                for(int m = 0; m < height; m += spriteHeight) {
                    int n = Math.min(spriteHeight, height - m);

                    blitSprite(bufferBuilder, poseStack, sprite, nineSliceWidth, nineSliceHeight, i, j, x + k, y + m, blitOffset, l, n);
                }
            }
        });
    }

    private static void blitTiledSprite(BufferBuilder bufferBuilder, PoseStack poseStack, TextureAtlasSprite sprite, int x, int y, int blitOffset, int width, int height, int i, int j, int spriteWidth, int spriteHeight, int nineSliceWidth, int nineSliceHeight) {
        if (width <= 0 || height <= 0) return;

        if (spriteWidth <= 0 || spriteHeight <= 0) {
            throw new IllegalArgumentException("Tiled sprite texture size must be positive, got " + spriteWidth + "x" + spriteHeight);
        }

        for(int k = 0; k < width; k += spriteWidth) {
            int l = Math.min(spriteWidth, width - k);

            for(int m = 0; m < height; m += spriteHeight) {
                int n = Math.min(spriteHeight, height - m);

                blitSprite(bufferBuilder, poseStack, sprite, nineSliceWidth, nineSliceHeight, i, j, x + k, y + m, blitOffset, l, n);
            }
        }
    }

    public static <T> void batched(GuiGraphics guiGraphics, ResourceLocation location, List<T> list, TriConsumer<BufferBuilder, PoseStack, T> consumer) {
        batched(guiGraphics, location, (bufferBuilder, poseStack) -> list.forEach(t -> consumer.accept(bufferBuilder, poseStack, t)));
    }

    public static void batched(GuiGraphics guiGraphics, ResourceLocation location, BiConsumer<BufferBuilder, PoseStack> consumer) {
        RenderSystem.setShaderTexture(0, location);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);

        var poseStack = guiGraphics.pose();

        var bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        consumer.accept(bufferBuilder, poseStack);

        var data = bufferBuilder.build();

        if(data != null) BufferUploader.drawWithShader(data);
    }

    private static void blitSprite(BufferBuilder bufferBuilder, PoseStack poseStack, TextureAtlasSprite sprite, int sliceWidth, int sliceHeight, int uOffset, int vOffset, int x, int y, int blitOffset, int width, int height) {
        if (width == 0 || height == 0) return;

        blitInner(bufferBuilder, poseStack, x, x + width, y, y + height, blitOffset, sprite.getU((float)uOffset / (float)sliceWidth), sprite.getU((float)(uOffset + width) / (float)sliceWidth), sprite.getV((float)vOffset / (float)sliceHeight), sprite.getV((float)(vOffset + height) / (float)sliceHeight));
    }

    public static void blit(BufferBuilder bufferBuilder, PoseStack poseStack, int x, int y, int size) {
        blit(bufferBuilder, poseStack, x, y, 0, 0, 0, size, size, size, size);
    }

    public static void blit(BufferBuilder bufferBuilder, PoseStack poseStack, int x1, int y1, int blitOffset, float uOffset, float vOffset, int width, int height, int textureWidth, int textureHeight) {
        var minU = (uOffset) / (float) textureWidth;
        var maxU = (uOffset + (float) width) / (float) textureWidth;
        var minV = (vOffset) / (float) textureHeight;
        var maxV = (vOffset + (float) height) / (float) textureHeight;

        blitInner(bufferBuilder, poseStack, x1, x1 + width, y1, y1 + height, blitOffset, minU, maxU, minV, maxV);
    }

    private static void blitInner(BufferBuilder bufferBuilder, PoseStack poseStack, int x1, int x2, int y1, int y2, int blitOffset, float minU, float maxU, float minV, float maxV) {
        var matrix4f = poseStack.last().pose();

        bufferBuilder.addVertex(matrix4f, (float) x1, (float) y1, (float) blitOffset).setUv(minU, minV);
        bufferBuilder.addVertex(matrix4f, (float) x1, (float) y2, (float) blitOffset).setUv(minU, maxV);
        bufferBuilder.addVertex(matrix4f, (float) x2, (float) y2, (float) blitOffset).setUv(maxU, maxV);
        bufferBuilder.addVertex(matrix4f, (float) x2, (float) y1, (float) blitOffset).setUv(maxU, minV);
    }

    //--

    public static void blitWithAlpha(GuiGraphics ctx, ResourceLocation atlasLocation, int x, int y, float uOffset, float vOffset, int width, int height, int textureWidth, int textureHeight, Vector4f alphaValues) {
        blitWithAlpha(ctx, atlasLocation, x, y, width, height, uOffset, vOffset, width, height, textureWidth, textureHeight, alphaValues);
    }

    private static void blitWithAlpha(GuiGraphics ctx, ResourceLocation atlasLocation, int x, int y, int width, int height, float uOffset, float vOffset, int uWidth, int vHeight, int textureWidth, int textureHeight, Vector4f alphaValues) {
        blitWithAlpha(ctx, atlasLocation, x, x + width, y, y + height, 0, uWidth, vHeight, uOffset, vOffset, textureWidth, textureHeight, alphaValues);
    }

    private static void blitWithAlpha(GuiGraphics ctx, ResourceLocation atlasLocation, int x1, int x2, int y1, int y2, int blitOffset, int uWidth, int vHeight, float uOffset, float vOffset, int textureWidth, int textureHeight, Vector4f alphaValues) {
        innerBlitWithAlpha(ctx, atlasLocation, x1, x2, y1, y2, blitOffset, (uOffset + 0.0F) / (float)textureWidth, (uOffset + (float)uWidth) / (float)textureWidth, (vOffset + 0.0F) / (float)textureHeight, (vOffset + (float)vHeight) / (float)textureHeight, alphaValues);
    }

    // X: Top Left
    // Y: Top Right
    // Z: Bottom Left
    // W: Bottom Right
    private static void innerBlitWithAlpha(GuiGraphics ctx, ResourceLocation atlasLocation, int x1, int x2, int y1, int y2, int blitOffset, float minU, float maxU, float minV, float maxV, Vector4f alphaValues) {
        RenderSystem.setShaderTexture(0, atlasLocation);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.enableBlend();
        Matrix4f matrix4f = ctx.pose().last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        bufferBuilder.addVertex(matrix4f, (float)x1, (float)y1, (float)blitOffset).setUv(minU, minV).setColor(1.0f, 1.0f, 1.0f, alphaValues.x);
        bufferBuilder.addVertex(matrix4f, (float)x1, (float)y2, (float)blitOffset).setUv(minU, maxV).setColor(1.0f, 1.0f, 1.0f, alphaValues.z);
        bufferBuilder.addVertex(matrix4f, (float)x2, (float)y2, (float)blitOffset).setUv(maxU, maxV).setColor(1.0f, 1.0f, 1.0f, alphaValues.w);
        bufferBuilder.addVertex(matrix4f, (float)x2, (float)y1, (float)blitOffset).setUv(maxU, minV).setColor(1.0f, 1.0f, 1.0f, alphaValues.y);
        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
        RenderSystem.disableBlend();
    }

    //--

    public static void blitWithColor(GuiGraphics ctx, ResourceLocation atlasLocation, int x, int y, float uOffset, float vOffset, int width, int height, int textureWidth, int textureHeight, float red, float green, float blue, float alpha) {
        blitWithColor(ctx, atlasLocation, x, y, width, height, uOffset, vOffset, width, height, textureWidth, textureHeight, red, green, blue, alpha);
    }

    private static void blitWithColor(GuiGraphics ctx, ResourceLocation atlasLocation, int x, int y, int width, int height, float uOffset, float vOffset, int uWidth, int vHeight, int textureWidth, int textureHeight, float red, float green, float blue, float alpha) {
        blitWithColor(ctx, atlasLocation, x, x + width, y, y + height, 0, uWidth, vHeight, uOffset, vOffset, textureWidth, textureHeight, red, green, blue, alpha);
    }

    private static void blitWithColor(GuiGraphics ctx, ResourceLocation atlasLocation, int x1, int x2, int y1, int y2, int blitOffset, int uWidth, int vHeight, float uOffset, float vOffset, int textureWidth, int textureHeight, float red, float green, float blue, float alpha) {
        innerBlitWithColor(ctx, atlasLocation, x1, x2, y1, y2, blitOffset, (uOffset + 0.0F) / (float)textureWidth, (uOffset + (float)uWidth) / (float)textureWidth, (vOffset + 0.0F) / (float)textureHeight, (vOffset + (float)vHeight) / (float)textureHeight, red, green, blue, alpha);
    }

    private static void innerBlitWithColor(GuiGraphics ctx, ResourceLocation atlasLocation, int x1, int x2, int y1, int y2, int blitOffset, float minU, float maxU, float minV, float maxV, float red, float green, float blue, float alpha) {
        RenderSystem.setShaderTexture(0, atlasLocation);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.enableBlend();
        Matrix4f matrix4f = ctx.pose().last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        bufferBuilder.addVertex(matrix4f, (float)x1, (float)y1, (float)blitOffset).setUv(minU, minV).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(matrix4f, (float)x1, (float)y2, (float)blitOffset).setUv(minU, maxV).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(matrix4f, (float)x2, (float)y2, (float)blitOffset).setUv(maxU, maxV).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(matrix4f, (float)x2, (float)y1, (float)blitOffset).setUv(maxU, minV).setColor(red, green, blue, alpha);
        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
        RenderSystem.disableBlend();
    }

    //--

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
    private static void innerDrawWithSpectrum(GuiGraphics ctx, ResourceLocation atlasLocation, int x1, int x2, int y1, int y2, int blitOffset, float minU, float maxU, float minV, float maxV,  Vector4f alphaValues) {
        RenderSystem.setShaderTexture(0, atlasLocation);

        RenderSystem.enableBlend();

        Matrix4f matrix4f = ctx.pose().last().pose();

        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        bufferBuilder.addVertex(matrix4f, (float)x1, (float)y1, (float)blitOffset).setUv(minU, minV).setColor(1.0f, 1.0f, 1.0f, alphaValues.x);
        bufferBuilder.addVertex(matrix4f, (float)x1, (float)y2, (float)blitOffset).setUv(minU, maxV).setColor(0, 1.0f, 1.0f, alphaValues.z);
        bufferBuilder.addVertex(matrix4f, (float)x2, (float)y2, (float)blitOffset).setUv(maxU, maxV).setColor(0, 1.0f, 1.0f, alphaValues.w);
        bufferBuilder.addVertex(matrix4f, (float)x2, (float)y1, (float)blitOffset).setUv(maxU, minV).setColor(1.0f, 1.0f, 1.0f, alphaValues.y);

        AccessoriesClient.SPECTRUM_PROGRAM.use();
        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());

        RenderSystem.disableBlend();
    }

    public static void drawRectOutlineWithSpectrum(OwoUIDrawContext ctx, int x, int y, int z, int width, int height, float alpha, boolean vertical) {
        ctx.recordQuads();

        drawRectOutlineWithSpectrumWithoutRecord(ctx, x, y, z, width, height, alpha, vertical);

        ctx.submitQuads();
    }


    public static void drawRectOutlineWithSpectrumWithoutRecord(OwoUIDrawContext ctx, int x, int y, int z, int width, int height, float alpha, boolean vertical) {
        innerFill(ctx, x, y, x + width, y + 1, 0, alpha, !vertical);
        innerFill(ctx, x, y + height - 1, x + width, y + height, 0, alpha, !vertical);

        innerFill(ctx, x, y + 1, x + 1, y + height - 1, 0, alpha, vertical);
        innerFill(ctx, x + width - 1, y + 1, x + width, y + height - 1, 0, alpha, vertical);
    }

    private static void innerFill(GuiGraphics ctx, int minX, int minY, int maxX, int maxY, int z, float alpha, boolean vertical) {
        RenderSystem.enableBlend();

        Matrix4f matrix4f = ctx.pose().last().pose();
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

        if (ctx instanceof OwoUIDrawContext context && context.recording()) {
            ((OwoTessellatorExtension) Tesselator.getInstance()).owo$skipNextBegin();
        }

        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        var multiplier = (float) ((System.currentTimeMillis() / 20d % 360d) / 360d);

        var topValue = 1f - multiplier;
        var bottomValue = topValue; //0f + multiplier;

        builder.addVertex(matrix4f, (float)minX, (float)minY, (float)z).setColor(topValue, 1f, 1f, alpha);
        builder.addVertex(matrix4f, (float)minX, (float)maxY, (float)z).setColor(vertical ? bottomValue : topValue, 1f, 1f, alpha);
        builder.addVertex(matrix4f, (float)maxX, (float)maxY, (float)z).setColor(bottomValue, 1f, 1f, alpha);
        builder.addVertex(matrix4f, (float)maxX, (float)minY, (float)z).setColor(vertical ? topValue : bottomValue, 1f, 1f, alpha);

        //--

        OwoClient.HSV_PROGRAM.use();

        if (ctx instanceof OwoUIDrawContext context && context.recording()) {
            ((OwoTessellatorExtension) Tesselator.getInstance()).owo$setStoredBuilder(builder);

            return;
        }

        BufferUploader.drawWithShader(builder.buildOrThrow());

        RenderSystem.disableBlend();
    }
}
