package io.wispforest.accessories.client.gui.components;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class PixelPerfectTextureComponent extends BaseComponent {

    private final ResourceLocation texture;

    private final int textureWidth;
    private final int textureHeight;

    public PixelPerfectTextureComponent(ResourceLocation texture, int textureWidth, int textureHeight, Sizing horizontalSizing, Sizing verticalSizing) {
        super();

        this.texture = texture;

        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;

        if(horizontalSizing.isContent()) throw new IllegalStateException("HorizontalSizing of PixelPerfectTextureComponent was found to be Content Sizing, which is not allowed!");
        if(verticalSizing.isContent()) throw new IllegalStateException("VerticalSizing of PixelPerfectTextureComponent was found to be Content Sizing, which is not allowed!");

        this.horizontalSizing(horizontalSizing);
        this.verticalSizing(verticalSizing);
    }

    @Override
    public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
        drawPixelPerfectTextureQuad(context, texture, textureWidth, textureHeight, this.x(), this.y(), 0, this.width(), this.height());
    }

    public static void drawPixelPerfectTextureQuad(OwoUIDrawContext context, ResourceLocation texture, int textureWidth, int textureHeight, int x1, int y1, float z, int width, int height) {
        int x2 = x1 + width;
        int y2 = y1 + height;

        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);

        Matrix4f matrix4f = context.pose().last().pose();

        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        bufferBuilder.addVertex(matrix4f, x1, y1, z)
                .setUv(0, 0);

        bufferBuilder.addVertex(matrix4f, x1, y2, z)
                .setUv(0, 1);

        bufferBuilder.addVertex(matrix4f, x2, y2, z)
                .setUv(1, 1);

        bufferBuilder.addVertex(matrix4f, x2, y1, z)
                .setUv(1, 0);

        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
    }
}
