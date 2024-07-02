package io.wispforest.accessories.fabric;

import io.wispforest.accessories.endec.CodecUtils;
import io.wispforest.accessories.networking.*;
import io.wispforest.accessories.networking.base.BaseNetworkHandler;
import io.wispforest.accessories.networking.base.HandledPacketPayload;
import io.wispforest.accessories.networking.base.NetworkBuilderRegister;
import io.wispforest.accessories.networking.base.PacketBuilderConsumer;
import io.wispforest.endec.Endec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class AccessoriesFabricNetworkHandler extends BaseNetworkHandler {

    public static final AccessoriesFabricNetworkHandler INSTANCE = new AccessoriesFabricNetworkHandler(AccessoriesPackets::register);

    protected AccessoriesFabricNetworkHandler(Consumer<NetworkBuilderRegister> builder) {
        super(builder);
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
        var type = PayloadTypeRegistry.playC2S().register(getId(messageType), CodecUtils.packetCodec(endec)).type();

        ServerPlayNetworking.registerGlobalReceiver(type, (packet, context) -> packet.handle(context.player()));
    }

    protected <M extends HandledPacketPayload> void registerS2CDeferred(Class<M> messageType, Endec<M> endec) {
        PayloadTypeRegistry.playS2C().register(BaseNetworkHandler.getId(messageType), CodecUtils.packetCodec(endec));
    }

    @Override
    @Environment(EnvType.CLIENT)
    public <M extends HandledPacketPayload> void sendToServer(M packet) {
        ClientPlayNetworking.send(packet);
    }

    @Override
    public <M extends HandledPacketPayload> void sendToPlayer(ServerPlayer player, M packet) {
        ServerPlayNetworking.send(player, packet);
    }

    @Override
    public <M extends HandledPacketPayload> void sendToTrackingAndSelf(Entity entity, Supplier<M> packet) {
        if(entity.level().isClientSide) return;

        for (var player : PlayerLookup.tracking(entity)) sendToPlayer(player, packet.get());

        if(entity instanceof ServerPlayer serverPlayer) sendToPlayer(serverPlayer, packet.get());
    }

    public interface RegistrationFunc {
        <M extends HandledPacketPayload> void consume(Class<M> messageType, Endec<M> endec);
    }
}
