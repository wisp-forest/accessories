package io.wispforest.accessories.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import io.wispforest.accessories.api.events.extra.ImplementedEvents;
import net.minecraft.client.Minecraft;
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
        this.framebuffer.clear(Minecraft.ON_OSX);
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousBuffer);
    }

    public void beginWrite(boolean clear, int blitFromMain) {
        this.ensureInitialized();

        this.prevBuffer = GlStateManager.getBoundFramebuffer();
        if (clear) this.framebuffer.clear(Minecraft.ON_OSX);

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

        RenderSystem.backupProjectionMatrix();
        this.framebuffer.blitToScreen(this.framebuffer.width, this.framebuffer.height, !blend);
        RenderSystem.restoreProjectionMatrix();
    }

    public void draw(float[] color) {
        var modulator = Minecraft.getInstance().gameRenderer.blitShader.COLOR_MODULATOR;
        modulator.set(color[0], color[1], color[2], color[3]);
        this.draw(true);
        modulator.set(1f, 1f, 1f, 1f);
    }

    public RenderTarget buffer() {
        this.ensureInitialized();
        return this.framebuffer;
    }

    private void ensureInitialized() {
        if (this.framebuffer != null) return;

        this.framebuffer = new TextureTarget(Minecraft.getInstance().getMainRenderTarget().width, Minecraft.getInstance().getMainRenderTarget().height, true, Minecraft.ON_OSX);
        this.framebuffer.setClearColor(0, 0, 0, 0);

        ImplementedEvents.WINDOW_RESIZE_CALLBACK_EVENT.register((client, window) -> {
            this.framebuffer.resize(window.getWidth(), window.getHeight(), Minecraft.ON_OSX);
            if (this.textureFilter != -1) {
                this.framebuffer.setFilterMode(this.textureFilter);
            }
        });
    }

}