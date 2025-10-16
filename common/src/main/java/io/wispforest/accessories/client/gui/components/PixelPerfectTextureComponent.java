package io.wispforest.accessories.client.gui.components;

import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.BlitRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3x2f;

public class PixelPerfectTextureComponent extends BaseComponent {

    private final ResourceLocation texture;

    public PixelPerfectTextureComponent(ResourceLocation texture, int textureWidth, int textureHeight, int scale) {
        this(texture, Sizing.fixed(textureWidth * scale), Sizing.fixed(textureHeight * scale));
    }

    public PixelPerfectTextureComponent(ResourceLocation texture, Sizing horizontalSizing, Sizing verticalSizing) {
        super();

        this.texture = texture;

        if(horizontalSizing.isContent()) throw new IllegalStateException("HorizontalSizing of PixelPerfectTextureComponent was found to be Content Sizing, which is not allowed!");
        if(verticalSizing.isContent()) throw new IllegalStateException("VerticalSizing of PixelPerfectTextureComponent was found to be Content Sizing, which is not allowed!");

        this.horizontalSizing(horizontalSizing);
        this.verticalSizing(verticalSizing);
    }

    @Override
    public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
        drawPixelPerfectTextureQuad(context, texture, this.x(), this.y(), this.width(), this.height());
    }

    public static void drawPixelPerfectTextureQuad(OwoUIDrawContext context, ResourceLocation texture, int x1, int y1, int width, int height) {
        int x2 = x1 + width;
        int y2 = y1 + height;

        var gpuTextureView = Minecraft.getInstance().getTextureManager().getTexture(texture).getTextureView();

        context.guiRenderState.submitGuiElement(
            new BlitRenderState(
                RenderPipelines.GUI_TEXTURED,
                TextureSetup.singleTexture(gpuTextureView),
                new Matrix3x2f(context.pose()),
                x1, y1,
                x2, y2,
                0, 1,
                0, 1,
                0xFFFFFFFF,
                context.scissorStack.peek()
            )
        );
    }
}
