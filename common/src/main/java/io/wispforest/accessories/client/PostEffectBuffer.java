package io.wispforest.accessories.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.pond.AccessoriesFrameBufferExtension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CoreShaders;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL30C;

public class PostEffectBuffer {

    private RenderTarget framebuffer = null;
    private int prevBuffer = 0;
    private int textureFilter = -1;

    public void clear() {
        this.ensureInitialized();

        int previousBuffer = GlStateManager.getBoundFramebuffer();
        this.framebuffer.clear();
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousBuffer);
    }

    public void beginWrite(boolean clear, int blitFromMain) {
        this.ensureInitialized();

        this.prevBuffer = GlStateManager.getBoundFramebuffer();
        if (clear) this.framebuffer.clear();

        if (blitFromMain != 0) {
            GlStateManager._glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, this.prevBuffer);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, this.framebuffer.frameBufferId);
            GL30.glBlitFramebuffer(
                    0, 0,
                    this.framebuffer.width, this.framebuffer.height,
                    0, 0,
                    this.framebuffer.width, this.framebuffer.height,
                    blitFromMain, GL11.GL_NEAREST
            );
        }

        this.framebuffer.bindWrite(false);
    }

    public void endWrite() {
        GlStateManager._glBindFramebuffer(GL30C.GL_FRAMEBUFFER, this.prevBuffer);
    }

    public void draw(boolean blend) {
        if (blend) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
        }

        var window = Minecraft.getInstance().getWindow();

        RenderSystem.backupProjectionMatrix();
        this.framebuffer.blitAndBlendToScreen(window.getWidth(), window.getHeight());
        RenderSystem.restoreProjectionMatrix();
    }

    public void draw(float[] color) {
        ((AccessoriesFrameBufferExtension)this.framebuffer).accessories$setUseHighlightShader(true);

        var blitShader = Minecraft.getInstance().getShaderManager().getProgram(AccessoriesClient.BLIT_SHADER_KEY);

        blitShader.COLOR_MODULATOR.set(color[0], color[1], color[2], color[3]);
        this.draw(true);
        ((AccessoriesFrameBufferExtension)this.framebuffer).accessories$setUseHighlightShader(false);
    }

    public RenderTarget buffer() {
        this.ensureInitialized();
        return this.framebuffer;
    }

    private void ensureInitialized() {
        if (this.framebuffer != null) return;

        this.framebuffer = new TextureTarget(Minecraft.getInstance().getMainRenderTarget().width, Minecraft.getInstance().getMainRenderTarget().height, true);
        this.framebuffer.setClearColor(0, 0, 0, 0);

        AccessoriesClient.WINDOW_RESIZE_CALLBACK_EVENT.register((client, window) -> {
            this.framebuffer.resize(window.getWidth(), window.getHeight());
            if (this.textureFilter != -1) {
                this.framebuffer.setFilterMode(this.textureFilter);
            }
        });
    }

}
