package io.wispforest.accessories.client;

import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import io.wispforest.owo.ui.core.OwoUIPipelines;
import io.wispforest.owo.ui.renderstate.GradientQuadElementRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3x2f;
import org.joml.Vector4f;

public class DrawUtils {

    public static void drawWithSpectrum(GuiGraphics ctx, int x, int y, int blitOffset, int width, int height, ResourceLocation texture, float alpha) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager()
            .getAtlasOrThrow(AtlasIds.GUI)
            .getSprite(texture);

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
    private static void innerDrawWithSpectrum(GuiGraphics guiGraphics, ResourceLocation atlasLocation, int x1, int x2, int y1, int y2, int blitOffset, float minU, float maxU, float minV, float maxV, Vector4f alphaValues) {
        guiGraphics.guiRenderState.submitGuiElement(
            new BlitSpectrumRenderState(
                AccessoriesPipelines.SPECTRUM,
                TextureSetup.noTexture(),
                new Matrix3x2f(guiGraphics.pose()),
                x1, y1, x2, y2,
                minU, maxU,
                minV, maxV,
                alphaValues,
                guiGraphics.scissorStack.peek()
            )
        );
    }

    public static void drawRectOutlineWithSpectrum(OwoUIDrawContext ctx, int x, int y, int width, int height, float alpha, boolean vertical) {
        innerFill(ctx, x, y, width, 1, alpha, !vertical);
        innerFill(ctx, x, y + height - 1, width, 1, alpha, !vertical);

        innerFill(ctx, x, y + 1, 1, height - 2, alpha, vertical);
        innerFill(ctx, x + width - 1, y + 1, 1, height - 2, alpha, vertical);
    }

    private static void innerFill(GuiGraphics guiGraphics, int x, int y, int width, int height, float alpha, boolean vertical) {
        var multiplier = (float) ((System.currentTimeMillis() / 20d % 360d) / 360d);

        var topValue = 1f - multiplier;
        var bottomValue = topValue; //0f + multiplier;

        var matrix = new Matrix3x2f(guiGraphics.pose());
        var scissorRect = guiGraphics.scissorStack.peek();

        // TODO: SEEMS BROKEN WHEN ANYTHING IS MANIPULATING THE MATRIX STACK SOOOOOO
        guiGraphics.guiRenderState.submitGuiElement(
            new GradientQuadElementRenderState(
                OwoUIPipelines.GUI_HSV,
                matrix,
                new ScreenRectangle(x, y, width, height).transformMaxBounds(matrix),
                scissorRect,
                new Color(topValue, 1f, 1f, alpha),
                new Color(bottomValue, 1f, 1f, alpha),
                new Color(vertical ? bottomValue : topValue, 1f, 1f, alpha),
                new Color(vertical ? topValue : bottomValue, 1f, 1f, alpha)
            )
        );
    }

    private static ScreenRectangle getBounds(int x1, int y1, int x2, int y2, Matrix3x2f matrix3x2f) {
        return new ScreenRectangle(x1, y1, Math.abs(x2 - x1), Math.abs(y2 - y1))/*.transformMaxBounds(matrix3x2f)*/;
    }

    public static void blitSprite(GuiGraphics context, ResourceLocation atlasLocation, int x, int y, int width, int height) {
        blitSprite(context, atlasLocation, x, y, width, height, -1);
    }

    public static void blitSprite(GuiGraphics context, ResourceLocation atlasLocation, int x, int y, int width, int height, int blitOffset) {
        context.blitSprite(RenderPipelines.GUI_TEXTURED, atlasLocation, x, y, width, height, blitOffset);
    }

    public static void blit(GuiGraphics context, ResourceLocation atlasLocation, int x, int y, int width, int height) {
        blit(context, atlasLocation, x, y, 0, 0, width, height, width, height);
    }

    public static void blit(GuiGraphics context, ResourceLocation atlasLocation, int x, int y, int uWidth, int vHeight, int textureWidth, int textureHeight) {
        blit(context, atlasLocation, x, y, 0, 0, uWidth, vHeight, textureWidth, textureHeight);
    }

    public static void blit(GuiGraphics context, ResourceLocation atlasLocation, int x, int y, float uOffset, float vOffset, int uWidth, int vHeight, int textureWidth, int textureHeight) {
        context.blit(RenderPipelines.GUI_TEXTURED, atlasLocation, x, y, uOffset, vOffset, uWidth, vHeight, uWidth, vHeight, textureWidth, textureHeight);
    }

    public static void blit(GuiGraphics context, ResourceLocation atlasLocation, int x, int y, float uOffset, float vOffset, int uWidth, int vHeight, int width, int height, int textureWidth, int textureHeight) {
        context.blit(RenderPipelines.GUI_TEXTURED, atlasLocation, x, y, uOffset, vOffset, uWidth, vHeight, width, height, textureWidth, textureHeight, -1);
    }

    // TODO: THIS CURRENTLY DOSE NOT MAKE THE ICONS WHITE AT ALL AND REQUIRES HEAVY MODIFICATION SIMILAR TO OWO BLUR TO SETUP THE UNIFORMS CORRECTLY
    public static void blitSpriteWithColor(GuiGraphics context, TextureAtlasSprite sprite, int x, int y, int width, int height, Color color) {
        context.blitSprite(RenderPipelines.GUI_TEXTURED/*location -> AccessoriesPipelines.COLORED_GUI_TEXTURED.apply(color, location)*/, sprite, x, y, width, height, color.argb());
    }
}
