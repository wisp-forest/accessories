package io.wispforest.tclayer.mixin;

import dev.emi.trinkets.data.SlotLoader;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


import java.util.function.Consumer;

@Pseudo
@Mixin(targets = "io/wispforest/accessories/neoforge/AccessoriesForge", remap = false)
public abstract class AccessoriesForgeMixin {

    @Inject(method = "intermediateRegisterListeners", at = @At("HEAD"))
    private void registerAdditionalResourceLoaders(Consumer<PreparableReloadListener> registrationMethod, CallbackInfo ci) {
        registrationMethod.accept(SlotLoader.INSTANCE);
        registrationMethod.accept(dev.emi.trinkets.data.EntitySlotLoader.SERVER);
    }
}
