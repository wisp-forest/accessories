package io.wispforest.accessories.neoforge;

import com.mojang.logging.LogUtils;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.endec.CodecUtils;
import io.wispforest.accessories.networking.AccessoriesNetworkHandler;
import io.wispforest.accessories.networking.AccessoriesPacket;
import io.wispforest.endec.Endec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.function.Supplier;

public class AccessoriesForgeNetworkHandler extends AccessoriesNetworkHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final AccessoriesForgeNetworkHandler INSTANCE = new AccessoriesForgeNetworkHandler();

    @Nullable
    private PayloadRegistrar registrar = null;

    @SubscribeEvent
    public void initializeNetworking(final RegisterPayloadHandlersEvent event) {
        this.registrar = event.registrar(Accessories.MODID);

        this.register();

        this.init();
    }

    @Override
    public void init() {
        for (var type : List.copyOf(this.s2cBuilders.keySet())) {
            if(!this.c2sBuilders.containsKey(type)) continue;

            var builder = this.s2cBuilders.get(type);

            builder.registerPacket(this::registerBoth);

            this.s2cBuilders.remove(type);
            this.c2sBuilders.remove(type);
        }

        this.s2cBuilders.forEach((location, builder) -> builder.registerPacket(this::registerS2C));
        this.c2sBuilders.forEach((location, builder) -> builder.registerPacket(this::registerC2S));
    }

    protected <M extends AccessoriesPacket> void registerC2S(Class<M> messageType, Endec<M> endec) {
        var id = getId(messageType);

        registrar.playToServer(id, CodecUtils.packetCodec(endec), (arg, iPayloadContext) -> {
            var player = iPayloadContext.player();

            iPayloadContext.enqueueWork(() -> arg.handle(player));
        });
    }

    protected <M extends AccessoriesPacket> void registerS2C(Class<M> messageType, Endec<M> endec) {
        var id = getId(messageType);

        registrar.playToClient(id, CodecUtils.packetCodec(endec), (arg, iPayloadContext) -> {
            var player = iPayloadContext.player();

            iPayloadContext.enqueueWork(() -> arg.handle(player));
        });
    }

    protected <M extends AccessoriesPacket> void registerBoth(Class<M> messageType, Endec<M> endec) {
        var id = getId(messageType);

        registrar.playBidirectional(id, CodecUtils.packetCodec(endec), (arg, iPayloadContext) -> {
            var player = iPayloadContext.player();

            iPayloadContext.enqueueWork(() -> arg.handle(player));
        });
    }

    @Override
    public <M extends AccessoriesPacket> void sendToServer(M packet) {
        PacketDistributor.sendToServer(packet);
    }

    @Override
    public <M extends AccessoriesPacket> void sendToPlayer(ServerPlayer player, M packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    @Override
    public <M extends AccessoriesPacket> void sendToTrackingAndSelf(Entity entity, Supplier<M> packet) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, packet.get());
    }
}
