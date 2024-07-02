package io.wispforest.testccessories.neoforge.mixin;

import io.wispforest.accessories.networking.base.BaseNetworkHandler;
import io.wispforest.accessories.networking.base.HandledPacketPayload;
import io.wispforest.endec.Endec;
import io.wispforest.testccessories.neoforge.client.TestScreenPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BaseNetworkHandler.class)
public abstract class AccessoriesNetworkHandlerMixin {

    @Shadow(remap = false) protected <M extends HandledPacketPayload> void registerBuilderC2S(Class<M> messageType, Endec<M> endec){}

    @Inject(method = "register", at = @At("TAIL"), remap = false)
    private void addTestPacket(CallbackInfo ci) {
        this.registerBuilderC2S(TestScreenPacket.class, TestScreenPacket.ENDEC);
    }
}
