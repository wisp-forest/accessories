package io.wispforest.cclayer;

import io.wispforest.accessories.networking.AccessoriesPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class WrappedAccessoriesPacket implements AccessoriesPacket {

    public final AccessoriesPacket packet;

    protected WrappedAccessoriesPacket(AccessoriesPacket packet){
        this.packet = packet;
    }

    @Override
    public Type<? extends AccessoriesPacket> type() {
        return this.packet.type();
    }
}
