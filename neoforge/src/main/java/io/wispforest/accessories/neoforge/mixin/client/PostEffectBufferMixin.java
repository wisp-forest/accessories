package io.wispforest.accessories.neoforge.mixin.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import io.wispforest.accessories.client.PostEffectBuffer;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PostEffectBuffer.class)
public class PostEffectBufferMixin {

    private RenderTarget framebuffer;

    @Inject(method = "ensureInitialized", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;setClearColor(FFFF)V"))
    private void adjustBuffer(CallbackInfo ci) {
        var mainTarget = Minecraft.getInstance().getMainRenderTarget();

        if (mainTarget.isStencilEnabled()) {
            framebuffer.enableStencil();
        }
    }
}
