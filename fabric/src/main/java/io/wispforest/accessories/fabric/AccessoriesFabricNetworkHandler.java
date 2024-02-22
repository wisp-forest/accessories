package io.wispforest.accessories.fabric;

import io.wispforest.accessories.networking.AccessoriesNetworkHandler;
import io.wispforest.accessories.networking.AccessoriesPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class AccessoriesFabricNetworkHandler extends AccessoriesNetworkHandler {

    public static final AccessoriesFabricNetworkHandler INSTANCE = new AccessoriesFabricNetworkHandler();

    private final Map<ResourceLocation, PacketType<AccessoriesFabricPacket<?>>> packetTypes = new HashMap<>();

    @Override
    public void init() {
        this.s2cBuilders.forEach((location, builder) -> builder.registerPacket(this::registerS2CDeferred));

        this.c2sBuilders.forEach((location, builder) -> builder.registerPacket(this::registerC2S));
    }

    @Environment(EnvType.CLIENT)
    public void initClient() {
        this.s2cBuilders.forEach((location, builder) -> builder.registerPacket(this::registerS2C));

        this.c2sBuilders.forEach((location, builder) -> builder.registerPacket(this::registerC2S));
    }

    protected <M extends AccessoriesPacket> void registerC2S(Class<M> messageType, Supplier<M> supplier) {
        ServerPlayNetworking.registerGlobalReceiver(getOrCreate(messageType, supplier), (packet, player, sender) -> packet.innerPacket().handle(player));
    }

    protected <M extends AccessoriesPacket> void registerS2CDeferred(Class<M> messageType, Supplier<M> supplier) {
        getOrCreate(messageType, supplier);
    }

    @Environment(EnvType.CLIENT)
    protected <M extends AccessoriesPacket> void registerS2C(Class<M> messageType, Supplier<M> supplier) {
        //TODO: CLASS LOADING ISSUE!
        ClientPlayNetworking.registerGlobalReceiver(getOrCreate(messageType, supplier), (packet, player, sender) -> packet.innerPacket().handle(player));
    }

    private <M extends AccessoriesPacket> PacketType<AccessoriesFabricPacket<?>> getOrCreate(Class<M> messageType, Supplier<M> supplier){
        return packetTypes.computeIfAbsent(
                getId(messageType),
                location -> PacketType.create(location, buf -> new AccessoriesFabricPacket<>(AccessoriesPacket.read(supplier, buf)))
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

    @Override
    public <M extends AccessoriesPacket> void sendToTrackingAndSelf(Entity entity, Supplier<M> packet) {
        if(entity.level().isClientSide) return;

        for (var player : PlayerLookup.tracking(entity)) sendToPlayer(player, packet.get());

        if(entity instanceof ServerPlayer serverPlayer) sendToPlayer(serverPlayer, packet.get());
    }

    @Nullable
    public <M extends AccessoriesPacket> PacketType<AccessoriesFabricPacket<?>> get(Class<M> mClass){
        return this.packetTypes.get(getId(mClass));
    }

    private record AccessoriesFabricPacket<P extends AccessoriesPacket>(P innerPacket) implements FabricPacket {
        @Override
        public void write(FriendlyByteBuf buf) {
            this.innerPacket.write(buf);
        }

        @Override
        public PacketType<?> getType() {
            var clazz = this.innerPacket.getClass();

            var packetType = INSTANCE.get(clazz);

            if(packetType == null) throw new IllegalStateException("Unable to get the FabricPacket Type for the following class! [Name: " + clazz + "]");

            return packetType;
        }
    }
}
