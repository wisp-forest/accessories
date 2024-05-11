package io.wispforest.testccessories.neoforge.mixin;

import io.wispforest.accessories.networking.AccessoriesNetworkHandler;
import io.wispforest.accessories.networking.AccessoriesPacket;
import io.wispforest.testccessories.neoforge.client.TestScreenPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(AccessoriesNetworkHandler.class)
public abstract class AccessoriesNetworkHandlerMixin {

    @Shadow(remap = false) protected <M extends AccessoriesPacket> void registerBuilderC2S(Class<M> messageType, Supplier<M> supplier){}

    @Inject(method = "register", at = @At("TAIL"), remap = false)
    private void addTestPacket(CallbackInfo ci) {
        this.registerBuilderC2S(TestScreenPacket.class, TestScreenPacket::new);
    }
}
