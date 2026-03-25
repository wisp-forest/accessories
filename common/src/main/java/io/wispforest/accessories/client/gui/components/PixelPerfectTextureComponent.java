package io.wispforest.accessories.client.gui.components;

import io.wispforest.accessories.pond.GuiGraphicsAccess;
import io.wispforest.owo.ui.base.BaseUIComponent;
import io.wispforest.owo.ui.core.OwoUIGraphics;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;

public class PixelPerfectTextureComponent extends BaseUIComponent {

    private final Identifier texture;

    public PixelPerfectTextureComponent(Identifier texture, int textureWidth, int textureHeight, int scale) {
        this(texture, Sizing.fixed(textureWidth * scale), Sizing.fixed(textureHeight * scale));
    }

    public PixelPerfectTextureComponent(Identifier texture, Sizing horizontalSizing, Sizing verticalSizing) {
        super();

        this.texture = texture;

        if(horizontalSizing.isContent()) throw new IllegalStateException("HorizontalSizing of PixelPerfectTextureComponent was found to be Content Sizing, which is not allowed!");
        if(verticalSizing.isContent()) throw new IllegalStateException("VerticalSizing of PixelPerfectTextureComponent was found to be Content Sizing, which is not allowed!");

        this.horizontalSizing(horizontalSizing);
        this.verticalSizing(verticalSizing);
    }

    @Override
    public void draw(OwoUIGraphics context, int mouseX, int mouseY, float partialTicks, float delta) {
        drawPixelPerfectTextureQuad(context, texture, this.x(), this.y(), this.width(), this.height());
    }

    public static void drawPixelPerfectTextureQuad(OwoUIGraphics context, Identifier texture, int x1, int y1, int width, int height) {
        int x2 = x1 + width;
        int y2 = y1 + height;

        var textureObj = Minecraft.getInstance().getTextureManager().getTexture(texture);
        var gpuTextureView = textureObj.getTextureView();
        var gpuSampler = textureObj.getSampler();

        var access = (GuiGraphicsAccess) (Object) context;
        access.accessories$guiRenderState().addGuiElement(
            new BlitRenderState(
                RenderPipelines.GUI_TEXTURED,
                TextureSetup.singleTexture(gpuTextureView, gpuSampler),
                new Matrix3x2f(context.pose()),
                x1, y1,
                x2, y2,
                0, 1,
                0, 1,
                0xFFFFFFFF,
                access.accessories$scissorStack().peek()
            )
        );
    }
}
