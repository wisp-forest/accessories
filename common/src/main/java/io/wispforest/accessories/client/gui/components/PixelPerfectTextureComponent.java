package io.wispforest.accessories.client.gui.components;

import io.wispforest.accessories.client.DrawUtils;
import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

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
        drawPixelPerfectTextureQuad(context, texture, this.x(), this.y(), 0, this.width(), this.height());
    }

    public static void drawPixelPerfectTextureQuad(OwoUIDrawContext context, ResourceLocation texture, int x1, int y1, float z, int width, int height) {
        int x2 = x1 + width;
        int y2 = y1 + height;

        var vertexConsumer = context.vertexConsumers().getBuffer(RenderType.guiTextured(texture));
        var matrix4f = context.pose().last().pose();

        DrawUtils.addToVertexBuffer(vertexConsumer, matrix4f, x1, y1, z, 0, 0);
        DrawUtils.addToVertexBuffer(vertexConsumer, matrix4f, x1, y2, z, 0, 1);
        DrawUtils.addToVertexBuffer(vertexConsumer, matrix4f, x2, y2, z, 1, 1);
        DrawUtils.addToVertexBuffer(vertexConsumer, matrix4f, x2, y1, z, 1, 0);
    }
}
