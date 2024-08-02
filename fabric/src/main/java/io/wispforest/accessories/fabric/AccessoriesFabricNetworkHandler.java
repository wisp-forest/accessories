package io.wispforest.accessories.fabric;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.networking.*;
import io.wispforest.accessories.networking.base.*;
import io.wispforest.endec.Endec;
import io.wispforest.endec.format.bytebuf.ByteBufDeserializer;
import io.wispforest.endec.format.bytebuf.ByteBufSerializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AccessoriesFabricNetworkHandler extends BaseNetworkHandler {

    public static final AccessoriesFabricNetworkHandler INSTANCE = new AccessoriesFabricNetworkHandler(AccessoriesPackets::register);

    private final Map<Type<?>, PacketType<AccessoriesFabricPacket<?>>> packetTypes = new HashMap<>();

    protected AccessoriesFabricNetworkHandler(Consumer<NetworkBuilderRegister> builder) {
        super(Accessories.of("main"), builder);
    }

    @Override
    public void init() {
        super.init();

        this.s2cBuilders.forEach((location, builder) -> builder.registerPacket(this::registerS2CDeferred));
        this.c2sBuilders.forEach((location, builder) -> builder.registerPacket(this::registerC2S));
    }

    @Environment(EnvType.CLIENT)
    public void initClient(PacketBuilderConsumer registerS2C) {
        this.s2cBuilders.forEach((location, builder) -> builder.registerPacket(registerS2C::accept));
    }

    protected <M extends HandledPacketPayload> void registerC2S(Class<M> messageType, Endec<M> endec) {
        ServerPlayNetworking.registerGlobalReceiver(getOrCreateType(messageType, endec), (packet, player, sender) -> packet.innerPacket().handle(player));
    }

    protected <M extends HandledPacketPayload> void registerS2CDeferred(Class<M> messageType, Endec<M> endec) {
        getOrCreateType(messageType, endec);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public <M extends HandledPacketPayload> void sendToServer(M packet) {
        ClientPlayNetworking.send(createFabricPacket(packet, true));
    }

    @Override
    public <M extends HandledPacketPayload> void sendToPlayer(ServerPlayer player, M packet) {
        ServerPlayNetworking.send(player, createFabricPacket(packet, false));
    }

    @Override
    public <M extends HandledPacketPayload> void sendToTrackingAndSelf(Entity entity, Supplier<M> packet) {
        if(entity.level().isClientSide) return;

        for (var player : PlayerLookup.tracking(entity)) sendToPlayer(player, packet.get());

        if(entity instanceof ServerPlayer serverPlayer) sendToPlayer(serverPlayer, packet.get());
    }

    @Nullable
    public <M extends HandledPacketPayload> PacketType<AccessoriesFabricPacket<?>> get(Class<M> mClass){
        return this.packetTypes.get(getId(mClass));
    }

    private <P extends HandledPacketPayload> AccessoriesFabricPacket<P> createFabricPacket(P packet, boolean isClient) {
        return new AccessoriesFabricPacket<>(packet, getEndec((Class<P>) packet.getClass(), isClient));
    }

    public <M extends HandledPacketPayload> PacketType<AccessoriesFabricPacket<?>> getOrCreateType(Class<M> messageType, Endec<M> endec){
        return packetTypes.computeIfAbsent(
                getId(messageType),
                location -> PacketType.create(location.location(), buf -> {
                    var innerData = endec.decodeFully(ByteBufDeserializer::of, buf);

                    return new AccessoriesFabricPacket<>(innerData, endec);
                })
        );
    }

    public <P extends HandledPacketPayload> Endec<P> getEndec(Class<P> clazz, boolean isClient) {
        return (Endec<P>) ((isClient) ? this.c2sBuilders : this.s2cBuilders)
                .get(this.getId(clazz)).endec();
    }

    public record AccessoriesFabricPacket<P extends HandledPacketPayload>(P innerPacket, Endec<P> endec) implements FabricPacket {
        @Override
        public void write(FriendlyByteBuf buf) {
            endec.encodeFully(() -> ByteBufSerializer.of(buf), innerPacket);
        }

        @Override
        public PacketType<?> getType() {
            var clazz = this.innerPacket.getClass();

            var packetType = INSTANCE.get(clazz);

            if(packetType == null) throw new IllegalStateException("Unable to get the FabricPacket Type for the following class! [Name: " + clazz + "]");

            return packetType;
        }
    }

    public interface RegistrationFunc {
        <M extends HandledPacketPayload> void consume(Class<M> messageType, Endec<M> endec);
    }
}
