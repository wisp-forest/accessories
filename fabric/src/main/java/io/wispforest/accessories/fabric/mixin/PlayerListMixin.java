package io.wispforest.accessories.fabric.mixin;

import io.wispforest.accessories.impl.AccessoriesEventHandler;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public class PlayerListMixin {
    @Inject(
            method = "reloadResources",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/common/ClientboundUpdateTagsPacket;<init>(Ljava/util/Map;)V")
    )
    private void hookOnDataPacksReloaded(CallbackInfo ci) {
        AccessoriesEventHandler.dataSync(((PlayerList) (Object) this), null);
    }
}
