package io.wispforest.accessories.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.pipeline.RenderTarget;
import io.wispforest.accessories.client.AccessoriesFunkyRenderingState;
import io.wispforest.accessories.client.AccessoriesPipelines;
import net.minecraft.client.renderer.RenderStateShard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RenderStateShard.class)
public abstract class RenderPhaseMixin {

    @ModifyExpressionValue(method = "method_68490", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getMainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;"))
    private static RenderTarget injectProperRenderTarget(RenderTarget original) {
        return (AccessoriesFunkyRenderingState.INSTANCE.isOverrideRenderTarget())
            ? AccessoriesPipelines.getOrCreateBuffer()
            : original;
    }
}
