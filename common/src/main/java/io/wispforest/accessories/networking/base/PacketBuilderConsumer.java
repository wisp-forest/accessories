package io.wispforest.accessories.networking.base;

import io.wispforest.endec.Endec;

public interface PacketBuilderConsumer {
    <M extends HandledPacketPayload> void accept(Class<M> messageType, Endec<M> endec);
}
