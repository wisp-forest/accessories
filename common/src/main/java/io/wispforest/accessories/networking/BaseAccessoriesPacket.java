package io.wispforest.accessories.networking;

import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.networking.base.BaseNetworkHandler;
import io.wispforest.accessories.networking.base.HandledPacketPayload;

public interface BaseAccessoriesPacket extends HandledPacketPayload {
    @Override
    default BaseNetworkHandler handler() {
        return AccessoriesInternals.getNetworkHandler();
    }
}
