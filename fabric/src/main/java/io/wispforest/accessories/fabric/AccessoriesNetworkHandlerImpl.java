package io.wispforest.accessories.fabric;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.networking.AccessoriesNetworkHandler;
import io.wispforest.accessories.networking.AccessoriesPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class AccessoriesNetworkHandlerImpl extends AccessoriesNetworkHandler {

    public static final AccessoriesNetworkHandlerImpl INSTANCE = new AccessoriesNetworkHandlerImpl();

    private final Map<ResourceLocation, PacketType<AccessoriesFabricPacket<?>>> packetTypes = new HashMap<>();

    @Override
    protected <M extends AccessoriesPacket> void registerC2S(Class<M> messageType, Supplier<M> supplier) {
        var location = Accessories.of(messageType.getName().toLowerCase());
        PacketType<AccessoriesFabricPacket<?>> type = PacketType.create(location, buf -> {
            var emptyPacket = supplier.get();

            emptyPacket.readPacket(buf);

            return new AccessoriesFabricPacket<>(emptyPacket);
        });

        packetTypes.put(location, type);

        ServerPlayNetworking.registerGlobalReceiver(type, (packet, player, responseSender) -> packet.innerPacket().attemptToHandle(player));
    }

    @Override
    @Environment(EnvType.CLIENT)
    protected <M extends AccessoriesPacket> void registerS2C(Class<M> messageType, Supplier<M> supplier) {
        var location = Accessories.of(messageType.getName().toLowerCase());
        PacketType<AccessoriesFabricPacket<?>> type = PacketType.create(location, buf -> {
            var emptyPacket = supplier.get();

            emptyPacket.readPacket(buf);

            return new AccessoriesFabricPacket<>(emptyPacket);
        });

        packetTypes.put(location, type);

        ClientPlayNetworking.registerGlobalReceiver(type, (packet, player, responseSender) -> packet.innerPacket().attemptToHandle(Minecraft.getInstance().player));
    }

    @Override
    @Environment(EnvType.CLIENT)
    public <M extends AccessoriesPacket> void sendToServer(M packet) {
        ClientPlayNetworking.send(new AccessoriesFabricPacket<>(packet));
    }

    @Override
    public <M extends AccessoriesPacket> void sendToPlayer(ServerPlayer player, M packet) {
        ServerPlayNetworking.send(player, new AccessoriesFabricPacket<>(packet));
    }

    @Nullable
    public <M extends AccessoriesPacket> PacketType<AccessoriesFabricPacket<?>> get(Class<M> mClass){
        return packetTypes.get(new ResourceLocation(Accessories.MODID, mClass.getName().toLowerCase()));
    }
}
