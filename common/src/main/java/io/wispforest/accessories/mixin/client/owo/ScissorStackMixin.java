package io.wispforest.accessories.mixin.client.owo;

import com.mojang.blaze3d.vertex.PoseStack;
import io.wispforest.accessories.pond.owo.ComponentExtension;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.util.ScissorStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ScissorStack.class, remap = false)
public abstract class ScissorStackMixin {

    @Inject(method = "isVisible(Lio/wispforest/owo/ui/core/Component;Lcom/mojang/blaze3d/vertex/PoseStack;)Z", at = @At("HEAD"), cancellable = true, remap = false)
    private static void accessories$allowIndividualOverdraw(Component component, PoseStack matrices, CallbackInfoReturnable<Boolean> cir) {
        if(component instanceof ComponentExtension<?> extension && extension.allowIndividualOverdraw()) {
            cir.setReturnValue(true);
        }
    }
}
