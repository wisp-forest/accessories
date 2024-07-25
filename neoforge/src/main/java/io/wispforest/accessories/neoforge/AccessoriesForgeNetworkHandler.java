package io.wispforest.accessories.neoforge;

import com.mojang.logging.LogUtils;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.endec.CodecUtils;
import io.wispforest.accessories.networking.base.BaseNetworkHandler;
import io.wispforest.accessories.networking.base.HandledPacketPayload;
import io.wispforest.accessories.networking.AccessoriesPackets;
import io.wispforest.accessories.networking.base.NetworkBuilderRegister;
import io.wispforest.accessories.networking.base.Type;
import io.wispforest.endec.Endec;
import io.wispforest.endec.format.bytebuf.ByteBufDeserializer;
import io.wispforest.endec.format.bytebuf.ByteBufSerializer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.slf4j.Logger;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AccessoriesForgeNetworkHandler extends BaseNetworkHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final AccessoriesForgeNetworkHandler INSTANCE = new AccessoriesForgeNetworkHandler(AccessoriesPackets::register);

    private final SimpleChannel channel = NetworkRegistry.newSimpleChannel(Accessories.of("main"), () -> "1", NetworkRegistry.acceptMissingOr("1"), NetworkRegistry.acceptMissingOr("1"));

    private int i = 0;

    protected AccessoriesForgeNetworkHandler(Consumer<NetworkBuilderRegister> builder) {
        super(builder);
    }

    public void initializeNetworking() {
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

        channel.registerMessage(i,
                messageType,
                (packet, buf) -> endec.encodeFully(() -> ByteBufSerializer.of(buf), packet),
                (buf) -> endec.decodeFully(ByteBufDeserializer::of, buf),
                server(id));

        i++;
    }

    protected <M extends HandledPacketPayload> void registerS2C(Class<M> messageType, Endec<M> endec) {
        var id = getId(messageType);

        channel.registerMessage(i,
                messageType,
                (packet, buf) -> endec.encodeFully(() -> ByteBufSerializer.of(buf), packet),
                (buf) -> endec.decodeFully(ByteBufDeserializer::of, buf),
                (m, contextSupplier) -> client(id).accept(m, contextSupplier));

        i++;
    }

    protected <M extends HandledPacketPayload> void registerBoth(Class<M> messageType, Endec<M> endec) {
        var id = getId(messageType);

        channel.registerMessage(i,
                messageType,
                (packet, buf) -> endec.encodeFully(() -> ByteBufSerializer.of(buf), packet),
                (buf) -> endec.decodeFully(ByteBufDeserializer::of, buf),
                (m, contextSupplier) -> {
                    if(contextSupplier.get().getDirection().getReceptionSide() == LogicalSide.CLIENT) {
                        client(id).accept(m, contextSupplier);
                    } else {
                        server(id).accept(m, contextSupplier);
                    }
                });

        i++;
    }

    //--

    private static <T extends HandledPacketPayload> BiConsumer<T, Supplier<NetworkEvent.Context>> server(Type<T> id) {
        return (packet, context) -> {
            var player = context.get().getSender();

            if(player == null) {
                LOGGER.warn("Player was found to be empty, packet wont be handled! [Location: {}]", id);

                return;
            }

            context.get().enqueueWork(() -> packet.handle(context.get().getSender()));

            context.get().setPacketHandled(true);
        };
    }

    private static <T extends HandledPacketPayload> BiConsumer<T, Supplier<NetworkEvent.Context>> client(Type<T> id) {
        return (packet, context) -> innerClientCall(packet, context, id).run();
    }

    @OnlyIn(Dist.CLIENT)
    private static <T extends HandledPacketPayload> Runnable innerClientCall(T packet, Supplier<NetworkEvent.Context> context, Type<T> id) {
        return () -> {
            var client = Minecraft.getInstance();

            client.execute(() -> {
                var player = client.player;

                if(player == null) {
                    LOGGER.warn("Player was found to be empty, packet wont be handled! [Location: {}]", id);

                    return;
                }

                packet.handle(player);
            });

            context.get().setPacketHandled(true);
        };
    }

    //--

    @Override
    public <M extends HandledPacketPayload> void sendToServer(M packet) {
        this.channel.sendToServer(packet);
    }

    @Override
    public <M extends HandledPacketPayload> void sendToPlayer(ServerPlayer player, M packet) {
        this.channel.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    @Override
    public <M extends HandledPacketPayload> void sendToTrackingAndSelf(Entity entity, Supplier<M> packet) {
        this.channel.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), packet.get());
    }

    //--

    public <M extends HandledPacketPayload> void sendWithDistributor(PacketDistributor.PacketTarget target, M message){
        this.channel.send(target, message);
    }

    public <M extends HandledPacketPayload> void sendWithConnection(Connection manager, NetworkDirection direction, M message) {
        this.channel.sendTo(message, manager, direction);
    }
}
