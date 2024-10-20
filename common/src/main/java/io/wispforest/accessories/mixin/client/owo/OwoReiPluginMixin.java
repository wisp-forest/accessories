package io.wispforest.accessories.mixin.client.owo;

import io.wispforest.accessories.client.gui.AccessoriesExperimentalScreen;
import io.wispforest.owo.compat.rei.OwoReiPlugin;
import io.wispforest.owo.ui.base.BaseOwoHandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.List;

@Mixin(value = OwoReiPlugin.class, remap = false)
public abstract class OwoReiPluginMixin {

    @Inject(method = "lambda$registerExclusionZones$2", at = @At("HEAD"), remap = false, cancellable = true)
    private static void accessories$preventZonesForAccessoriesScreen(BaseOwoHandledScreen screen, CallbackInfoReturnable<Collection> cir) {
        if(screen instanceof AccessoriesExperimentalScreen) cir.setReturnValue(List.of());
    }
}
