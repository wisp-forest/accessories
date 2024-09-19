package io.wispforest.cclayer;

import io.wispforest.accessories.networking.BaseAccessoriesPacket;
import io.wispforest.accessories.networking.base.HandledPacketPayload;

public class WrappedAccessoriesPacket implements BaseAccessoriesPacket {

    public final HandledPacketPayload packet;

    protected WrappedAccessoriesPacket(HandledPacketPayload packet){
        this.packet = packet;
    }

    @Override
    public Type<? extends HandledPacketPayload> type() {
        return this.packet.type();
    }
}
