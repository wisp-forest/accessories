package io.wispforest.accessories.mixin.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import io.wispforest.accessories.client.AccessoriesClient;
import io.wispforest.accessories.pond.AccessoriesFrameBufferExtension;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(RenderTarget.class)
public abstract class RenderTargetMixin implements AccessoriesFrameBufferExtension {

    @Unique
    private boolean useHighlightShader = false;

    @ModifyVariable(method = "_blitToScreen", at = @At(value = "CONSTANT", args = "stringValue=DiffuseSampler", shift = At.Shift.BEFORE))
    private ShaderInstance gliscoLikesShaders(ShaderInstance value) {
        if (this.useHighlightShader) return AccessoriesClient.BLIT_SHADER;
        return value;
    }

    @Override
    public void accessories$setUseHighlightShader(boolean useHighlightShader) {
        this.useHighlightShader = useHighlightShader;
    }
}
