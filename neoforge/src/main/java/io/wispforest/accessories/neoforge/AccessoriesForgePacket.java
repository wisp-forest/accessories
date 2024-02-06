package io.wispforest.accessories.neoforge;

import io.wispforest.accessories.networking.AccessoriesPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AccessoriesForgePacket <P extends AccessoriesPacket>(P innerPacket) implements CustomPacketPayload {

    @Override
    public void write(FriendlyByteBuf buffer) {
        this.innerPacket.write(buffer);
    }

    @Override
    public ResourceLocation id() {
        var clazz = this.innerPacket.getClass();

        return AccessoriesForgeNetworkHandler.INSTANCE.getId(clazz);
    }
}
