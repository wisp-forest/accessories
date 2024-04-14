package io.wispforest.accessories.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import io.wispforest.accessories.mixin.GuiGraphicsAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.gui.GuiSpriteScaling;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.util.BiConsumer;
import org.apache.logging.log4j.util.TriConsumer;

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

        var bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        consumer.accept(bufferBuilder, poseStack);

        BufferUploader.drawWithShader(bufferBuilder.end());
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

    public static void blitInner(BufferBuilder bufferBuilder, PoseStack poseStack, int x1, int x2, int y1, int y2, int blitOffset, float minU, float maxU, float minV, float maxV) {
        var matrix4f = poseStack.last().pose();

        bufferBuilder.vertex(matrix4f, (float) x1, (float) y1, (float) blitOffset).uv(minU, minV).endVertex();
        bufferBuilder.vertex(matrix4f, (float) x1, (float) y2, (float) blitOffset).uv(minU, maxV).endVertex();
        bufferBuilder.vertex(matrix4f, (float) x2, (float) y2, (float) blitOffset).uv(maxU, maxV).endVertex();
        bufferBuilder.vertex(matrix4f, (float) x2, (float) y1, (float) blitOffset).uv(maxU, minV).endVertex();
    }
}
