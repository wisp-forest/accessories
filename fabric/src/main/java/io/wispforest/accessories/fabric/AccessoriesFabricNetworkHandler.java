package io.wispforest.accessories.fabric;

import io.wispforest.accessories.endec.CodecUtils;
import io.wispforest.accessories.networking.AccessoriesNetworkHandler;
import io.wispforest.accessories.networking.AccessoriesPacket;
import io.wispforest.endec.Endec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.function.Supplier;

public class AccessoriesFabricNetworkHandler extends AccessoriesNetworkHandler {

    public static final AccessoriesFabricNetworkHandler INSTANCE = new AccessoriesFabricNetworkHandler();

    @Override
    public void init() {
        this.s2cBuilders.forEach((location, builder) -> builder.registerPacket(this::registerS2CDeferred));
        this.c2sBuilders.forEach((location, builder) -> builder.registerPacket(this::registerC2S));
    }

    @Environment(EnvType.CLIENT)
    public void initClient(RegistrationFunc registerS2C) {
        this.s2cBuilders.forEach((location, builder) -> builder.registerPacket(registerS2C::consume));
        // this.c2sBuilders.forEach((location, builder) -> builder.registerPacket(this::registerC2S));
    }

    protected <M extends AccessoriesPacket> void registerC2S(Class<M> messageType, Endec<M> endec) {
        var type = getId(messageType);

        PayloadTypeRegistry.playC2S().register(type, CodecUtils.packetCodec(endec));

        ServerPlayNetworking.registerGlobalReceiver(type, (packet, context) -> packet.handle(context.player()));
    }

    protected <M extends AccessoriesPacket> void registerS2CDeferred(Class<M> messageType, Endec<M> endec) {
        var type = AccessoriesNetworkHandler.getId(messageType);

        PayloadTypeRegistry.playS2C().register(type, CodecUtils.packetCodec(endec));
    }

    @Override
    @Environment(EnvType.CLIENT)
    public <M extends AccessoriesPacket> void sendToServer(M packet) {
        ClientPlayNetworking.send(packet);
    }

    @Override
    public <M extends AccessoriesPacket> void sendToPlayer(ServerPlayer player, M packet) {
        ServerPlayNetworking.send(player, packet);
    }

    @Override
    public <M extends AccessoriesPacket> void sendToTrackingAndSelf(Entity entity, Supplier<M> packet) {
        if(entity.level().isClientSide) return;

        for (var player : PlayerLookup.tracking(entity)) sendToPlayer(player, packet.get());

        if(entity instanceof ServerPlayer serverPlayer) sendToPlayer(serverPlayer, packet.get());
    }

    public interface RegistrationFunc {
        <M extends AccessoriesPacket> void consume(Class<M> messageType, Endec<M> endec);
    }
}
