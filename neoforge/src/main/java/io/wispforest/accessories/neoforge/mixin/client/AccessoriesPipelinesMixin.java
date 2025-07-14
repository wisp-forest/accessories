package io.wispforest.accessories.neoforge.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.TextureTarget;
import io.wispforest.accessories.client.AccessoriesPipelines;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = AccessoriesPipelines.class)
public class AccessoriesPipelinesMixin {
    @WrapOperation(
        method = "getOrCreateBuffer",
        at = @At(value = "NEW", target = "(Ljava/lang/String;IIZ)Lcom/mojang/blaze3d/pipeline/TextureTarget;")
    )
    private static TextureTarget adjustBuffer(String string, int i, int j, boolean bl, Operation<TextureTarget> original) {
        var mainTarget = Minecraft.getInstance().getMainRenderTarget();

        return (mainTarget.useStencil)
            ? new TextureTarget(string, i, j, bl, true)
            : original.call(string, i, j, bl);
    }
}
