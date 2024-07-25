package io.wispforest.accessories.networking.base;

import net.minecraft.world.entity.player.Player;

public interface HandledPacketPayload {

    default Type<? extends HandledPacketPayload> type() {
        return BaseNetworkHandler.getId(this.getClass());
    }

    default void handle(Player player){
        //NOOP
    }
}
