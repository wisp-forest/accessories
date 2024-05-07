package top.theillusivec4.curios.common.network.client;

import io.wispforest.accessories.networking.AccessoriesPacket;
import io.wispforest.accessories.networking.server.NukeAccessories;
import io.wispforest.cclayer.WrappedAccessoriesPacket;

public class CPacketDestroy extends WrappedAccessoriesPacket {
    protected CPacketDestroy() {
        super(new NukeAccessories());
    }
}
