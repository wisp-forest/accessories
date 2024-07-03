package io.wispforest.testccessories.fabric.mixin;

import io.wispforest.accessories.networking.AccessoriesPackets;
import io.wispforest.accessories.networking.base.NetworkBuilderRegister;
import io.wispforest.testccessories.fabric.client.TestScreenPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AccessoriesPackets.class)
public abstract class AccessoriesPacketsMixin {
    @Inject(method = "register", at = @At("TAIL"), remap = false)
    private static void addTestPacket(NetworkBuilderRegister register, CallbackInfo ci) {
        register.registerBuilderC2S(TestScreenPacket.class, TestScreenPacket.ENDEC);
    }
}