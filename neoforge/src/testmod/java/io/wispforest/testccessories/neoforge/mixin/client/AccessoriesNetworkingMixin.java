package io.wispforest.testccessories.neoforge.mixin.client;

import io.wispforest.accessories.networking.AccessoriesNetworking;
import io.wispforest.testccessories.neoforge.client.TestccessoriesClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AccessoriesNetworking.class)
public abstract class AccessoriesNetworkingMixin {
    @Inject(method = "initClient", at = @At("TAIL"))
    private static void registerOtherPacketsClient(CallbackInfo ci) {
        TestccessoriesClient.initNetworkPackets();
    }
}
