package top.theillusivec4.curios.common.network.client;

import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.accessories.networking.server.SyncCosmeticToggle;
import io.wispforest.cclayer.WrappedAccessoriesPacket;

public class CPacketToggleRender extends WrappedAccessoriesPacket {

    public CPacketToggleRender(String id, int index) {
        super(new SyncCosmeticToggle(SlotTypeLoader.INSTANCE.getSlotTypes(true).get(id), index));
    }
}
