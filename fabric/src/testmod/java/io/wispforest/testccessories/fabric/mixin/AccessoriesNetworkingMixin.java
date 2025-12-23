package io.wispforest.testccessories.fabric.mixin;

import io.wispforest.accessories.networking.AccessoriesNetworking;
import io.wispforest.testccessories.fabric.Testccessories;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AccessoriesNetworking.class, remap = false)
public abstract class AccessoriesNetworkingMixin {
    @Inject(method = "init", at = @At("TAIL"))
    private static void registerOtherPackets(CallbackInfo ci) {
        Testccessories.initNetworkPackets();
    }
}