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
        var clazz = this.innerPacket.getClass();

        var packetType = AccessoriesNetworkHandlerImpl.INSTANCE.get(clazz);

        if(packetType == null) throw new IllegalStateException("Unable to get the FabricPacket Type for the following class! [Name: " + clazz + "]");

        return packetType;
    }
}
