package io.wispforest.accessories.mixin.client.owo;

import io.wispforest.accessories.client.gui.AccessoriesExperimentalScreen;
import io.wispforest.owo.compat.emi.OwoEmiPlugin;
import io.wispforest.owo.compat.rei.OwoReiPlugin;
import io.wispforest.owo.ui.base.BaseOwoHandledScreen;
import io.wispforest.owo.ui.core.Component;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

@Mixin(value = OwoEmiPlugin.class, remap = false)
public abstract class OwoEmiPluginMixin {

    @Inject(method = "lambda$register$2", at = @At("HEAD"), remap = false, cancellable = true)
    private static void accessories$preventZonesForAccessoriesScreen(Screen screen, Consumer consumer, CallbackInfo ci) {
        if(screen instanceof AccessoriesExperimentalScreen) ci.cancel();
    }
}
