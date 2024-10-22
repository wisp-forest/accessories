package io.wispforest.accessories.mixin.client;

import io.wispforest.owo.ui.layers.Layer;
import io.wispforest.owo.ui.layers.Layers;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractRecipeBookScreen.class)
public abstract class AbstractRecipeBookScreenMixin {
    @Inject(method = "method_64513", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/Button;setPosition(II)V"))
    private void adjustAccessoryButton(CallbackInfo ci){
        Layers.getInstances((InventoryScreen)(Object) this).forEach(Layer.Instance::dispatchLayoutUpdates);
    }
}
