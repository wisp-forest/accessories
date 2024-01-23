package io.wispforest.accessories.fabric;

import io.wispforest.accessories.networking.AccessoriesPacket;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

public record AccessoriesFabricPacket<P extends AccessoriesPacket>(P innerPacket) implements FabricPacket {

    @Override
    public void write(FriendlyByteBuf buf) {
        this.innerPacket.write(buf);
    }

    @Override
    public PacketType<?> getType() {
        return AccessoriesNetworkHandlerImpl.INSTANCE.get(this.innerPacket.getClass());
    }
}
