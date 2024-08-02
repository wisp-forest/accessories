package io.wispforest.accessories.neoforge;

import com.mojang.logging.LogUtils;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.endec.CodecUtils;
import io.wispforest.accessories.networking.base.BaseNetworkHandler;
import io.wispforest.accessories.networking.base.HandledPacketPayload;
import io.wispforest.accessories.networking.AccessoriesPackets;
import io.wispforest.accessories.networking.base.NetworkBuilderRegister;
import io.wispforest.endec.Endec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AccessoriesForgeNetworkHandler extends BaseNetworkHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final AccessoriesForgeNetworkHandler INSTANCE = new AccessoriesForgeNetworkHandler(AccessoriesPackets::register);

    @Nullable
    private PayloadRegistrar registrar = null;

    protected AccessoriesForgeNetworkHandler(Consumer<NetworkBuilderRegister> builder) {
        super(Accessories.of("main"), builder);
    }

    public void initializeNetworking(final RegisterPayloadHandlersEvent event) {
        this.registrar = event.registrar(Accessories.MODID);

        this.init();
    }

    @Override
    public void init() {
        super.init();

        for (var type : List.copyOf(this.s2cBuilders.keySet())) {
            if(!this.c2sBuilders.containsKey(type)) continue;

            this.s2cBuilders.get(type).registerPacket(this::registerBoth);

            this.s2cBuilders.remove(type);
            this.c2sBuilders.remove(type);
        }

        this.s2cBuilders.forEach((location, builder) -> builder.registerPacket(this::registerS2C));
        this.c2sBuilders.forEach((location, builder) -> builder.registerPacket(this::registerC2S));
    }

    protected <M extends HandledPacketPayload> void registerC2S(Class<M> messageType, Endec<M> endec) {
        var id = getId(messageType);

        this.registrar.playToServer(id, CodecUtils.packetCodec(endec), (arg, iPayloadContext) -> {
            var player = iPayloadContext.player();

            iPayloadContext.enqueueWork(() -> arg.handle(player));
        });
    }

    protected <M extends HandledPacketPayload> void registerS2C(Class<M> messageType, Endec<M> endec) {
        var id = getId(messageType);

        this.registrar.playToClient(id, CodecUtils.packetCodec(endec), (arg, iPayloadContext) -> {
            var player = iPayloadContext.player();

            iPayloadContext.enqueueWork(() -> arg.handle(player));
        });
    }

    protected <M extends HandledPacketPayload> void registerBoth(Class<M> messageType, Endec<M> endec) {
        var id = getId(messageType);

        this.registrar.playBidirectional(id, CodecUtils.packetCodec(endec), (arg, iPayloadContext) -> {
            var player = iPayloadContext.player();

            iPayloadContext.enqueueWork(() -> arg.handle(player));
        });
    }

    @Override
    public <M extends HandledPacketPayload> void sendToServer(M packet) {
        PacketDistributor.sendToServer(packet);
    }

    @Override
    public <M extends HandledPacketPayload> void sendToPlayer(ServerPlayer player, M packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    @Override
    public <M extends HandledPacketPayload> void sendToTrackingAndSelf(Entity entity, Supplier<M> packet) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, packet.get());
    }
}
