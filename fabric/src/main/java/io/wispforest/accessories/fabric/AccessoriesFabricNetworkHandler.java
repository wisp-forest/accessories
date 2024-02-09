package io.wispforest.accessories.fabric;

import io.wispforest.accessories.networking.AccessoriesNetworkHandler;
import io.wispforest.accessories.networking.AccessoriesPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class AccessoriesFabricNetworkHandler extends AccessoriesNetworkHandler {

    public static final AccessoriesFabricNetworkHandler INSTANCE = new AccessoriesFabricNetworkHandler();

    private final Map<ResourceLocation, PacketType<AccessoriesFabricPacket<?>>> packetTypes = new HashMap<>();

    @Override
    public void init() {
        s2cBuilders.forEach((location, builder) -> builder.registerPacket(this::registerS2CDeferred));

        c2sBuilders.forEach((location, builder) -> builder.registerPacket(this::registerC2S));
    }

    @Environment(EnvType.CLIENT)
    public void initClient() {
        s2cBuilders.forEach((location, builder) -> builder.registerPacket(this::registerS2C));

        c2sBuilders.forEach((location, builder) -> builder.registerPacket(this::registerC2S));
    }

    protected <M extends AccessoriesPacket> void registerC2S(Class<M> messageType, Supplier<M> supplier) {
        PacketType<AccessoriesFabricPacket<?>> type = getOrCreate(messageType, supplier);

        ServerPlayNetworking.registerGlobalReceiver(type, (packet, player, responseSender) -> packet.innerPacket().handle(player));
    }

    protected <M extends AccessoriesPacket> void registerS2CDeferred(Class<M> messageType, Supplier<M> supplier) {
        getOrCreate(messageType, supplier);
    }

    @Environment(EnvType.CLIENT)
    protected <M extends AccessoriesPacket> void registerS2C(Class<M> messageType, Supplier<M> supplier) {
        PacketType<AccessoriesFabricPacket<?>> type = getOrCreate(messageType, supplier);

        //TODO: CLASS LOADING ISSUE!
        ClientPlayNetworking.registerGlobalReceiver(type, (packet, player, responseSender) -> packet.innerPacket().handle(player));
    }

    private <M extends AccessoriesPacket> PacketType<AccessoriesFabricPacket<?>> getOrCreate(Class<M> messageType, Supplier<M> supplier){
        return packetTypes.computeIfAbsent(
                getId(messageType),
                location -> PacketType.create(location, buf -> new AccessoriesFabricPacket<>(supplier.get().readPacket(buf)))
        );
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
        return packetTypes.get(getId(mClass));
    }
}
