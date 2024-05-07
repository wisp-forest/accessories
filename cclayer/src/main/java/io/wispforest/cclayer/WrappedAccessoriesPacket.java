package io.wispforest.cclayer;

import io.wispforest.accessories.networking.AccessoriesPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class WrappedAccessoriesPacket extends AccessoriesPacket {

    public final AccessoriesPacket packet;

    protected WrappedAccessoriesPacket(AccessoriesPacket packet){
        this.packet = packet;
    }

    @Override
    public ResourceLocation id() {
        return this.packet.id();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        this.packet.write(buf);
    }

    @Override
    protected void read(FriendlyByteBuf buf) {
        throw new IllegalStateException("Read operation called on wrapped packet variant!");
    }
}
