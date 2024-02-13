package io.wispforest.accessories.neoforge;

import io.wispforest.accessories.networking.AccessoriesPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

public record AccessoriesForgePacket<P extends AccessoriesPacket>(P innerPacket) implements CustomPacketPayload {

    public static <P extends AccessoriesPacket> AccessoriesForgePacket<P> of(Supplier<P> supplier, FriendlyByteBuf buf){
        return new AccessoriesForgePacket<>(supplier.get().readPacket(buf));
    }

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
