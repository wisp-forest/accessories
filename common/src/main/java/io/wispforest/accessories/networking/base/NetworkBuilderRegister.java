package io.wispforest.accessories.networking.base;

import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.ReflectiveEndecBuilder;

public interface NetworkBuilderRegister {
    ReflectiveEndecBuilder builder();

    default <M extends HandledPacketPayload> void registerBuilderC2S(Class<M> messageType) {
        registerBuilderC2S(messageType, builder().get(messageType));
    }

    <M extends HandledPacketPayload> void registerBuilderC2S(Class<M> messageType, Endec<M> endec);

    default <M extends HandledPacketPayload> void registerBuilderS2C(Class<M> messageType) {
        registerBuilderC2S(messageType, builder().get(messageType));
    }

    <M extends HandledPacketPayload> void registerBuilderS2C(Class<M> messageType, Endec<M> endec);

    default <M extends HandledPacketPayload> void registerBuilderBiDi(Class<M> messageType) {
        registerBuilderC2S(messageType, builder().get(messageType));
    }

    <M extends HandledPacketPayload> void registerBuilderBiDi(Class<M> messageType, Endec<M> endec);
}
