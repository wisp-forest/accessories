package io.wispforest.accessories.networking.base;

import net.minecraft.world.entity.player.Player;

public interface HandledPacketPayload {

    BaseNetworkHandler handler();

    default Type<? extends HandledPacketPayload> type() {
        return handler().getId(this.getClass());
    }

    default void handle(Player player){
        //NOOP
    }
}
