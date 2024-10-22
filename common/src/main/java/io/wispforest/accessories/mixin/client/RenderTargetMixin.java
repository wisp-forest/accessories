package io.wispforest.accessories.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderTarget;
import io.wispforest.accessories.client.AccessoriesClient;
import io.wispforest.accessories.pond.AccessoriesFrameBufferExtension;
import net.minecraft.client.renderer.CompiledShaderProgram;
import net.minecraft.client.renderer.ShaderProgram;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RenderTarget.class)
public abstract class RenderTargetMixin implements AccessoriesFrameBufferExtension {

    @Unique
    private boolean useHighlightShader = false;

    @WrapOperation(method = "blitAndBlendToScreen", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShader(Lnet/minecraft/client/renderer/ShaderProgram;)Lnet/minecraft/client/renderer/CompiledShaderProgram;"))
    private CompiledShaderProgram gliscoLikesShaders(ShaderProgram shaderProgram, Operation<CompiledShaderProgram> original) {
        if (this.useHighlightShader) return original.call(AccessoriesClient.BLIT_SHADER_KEY);
        return original.call(shaderProgram);
    }

    @Override
    public void accessories$setUseHighlightShader(boolean useHighlightShader) {
        this.useHighlightShader = useHighlightShader;
    }
}
