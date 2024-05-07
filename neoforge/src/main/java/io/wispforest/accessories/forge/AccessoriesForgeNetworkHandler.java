package io.wispforest.accessories.forge;

import com.mojang.logging.LogUtils;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.networking.AccessoriesNetworkHandler;
import io.wispforest.accessories.networking.AccessoriesPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import org.slf4j.Logger;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class AccessoriesForgeNetworkHandler extends AccessoriesNetworkHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final AccessoriesForgeNetworkHandler INSTANCE = new AccessoriesForgeNetworkHandler();

    private final SimpleChannel channel = NetworkRegistry.newSimpleChannel(Accessories.of("main"), () -> "1", NetworkRegistry.acceptMissingOr("1"), NetworkRegistry.acceptMissingOr("1"));

    private int i = 0;

    public void initializeNetworking() {
        this.register();

        this.init();
    }

    @Override
    public void init() {
        for (ResourceLocation location : List.copyOf(this.s2cBuilders.keySet())) {
            if(!this.c2sBuilders.containsKey(location)) continue;

            var builder = this.s2cBuilders.get(location);

            builder.registerPacket(this::registerBoth);

            this.s2cBuilders.remove(location);
            this.c2sBuilders.remove(location);
        }

        this.s2cBuilders.forEach((location, builder) -> builder.registerPacket(this::registerS2C));
        this.c2sBuilders.forEach((location, builder) -> builder.registerPacket(this::registerC2S));
    }

    protected <M extends AccessoriesPacket> void registerC2S(Class<M> messageType, Supplier<M> supplier) {
        var location = getId(messageType);

        channel.registerMessage(i,
                messageType,
                AccessoriesPacket::write,
                (buf) -> AccessoriesPacket.read(supplier, buf),
                server(location, AccessoriesPacket::handle)
        );

        i++;
    }

    protected <M extends AccessoriesPacket> void registerS2C(Class<M> messageType, Supplier<M> supplier) {
        var location = getId(messageType);

        channel.registerMessage(i,
                messageType,
                AccessoriesPacket::write,
                (buf) -> AccessoriesPacket.read(supplier, buf),
                client(location, AccessoriesPacket::handle)
        );

        i++;
    }

    protected <M extends AccessoriesPacket> void registerBoth(Class<M> messageType, Supplier<M> supplier) {
        var location = getId(messageType);

        channel.registerMessage(i,
                messageType,
                AccessoriesPacket::write,
                (buf) -> AccessoriesPacket.read(supplier, buf),
                (m, contextSupplier) -> {
                    if(contextSupplier.get().getDirection().getReceptionSide() == LogicalSide.CLIENT) {
                        client(location, AccessoriesPacket::handle).accept(m, contextSupplier);
                    } else {
                        server(location, AccessoriesPacket::handle).accept(m, contextSupplier);
                    }
                }
        );

        i++;
    }

    private static <T> BiConsumer<T, Supplier<NetworkEvent.Context>> server(ResourceLocation id, BiConsumer<T, Player> handler) {
        return (packet, context) -> {
            var player = context.get().getSender();

            if(player == null) {
                LOGGER.warn("Player was found to be empty, packet wont be handled! [Location: {}]", id);

                return;
            }

            context.get().enqueueWork(() -> handler.accept(packet, context.get().getSender()));

            context.get().setPacketHandled(true);
        };
    }

    private static <T> BiConsumer<T, Supplier<NetworkEvent.Context>> client(ResourceLocation id, BiConsumer<T, Player> handler) {
        return (packet, context) -> {
            var client = Minecraft.getInstance();

            var player = client.player;

            if(player == null) {
                LOGGER.warn("Player was found to be empty, packet wont be handled! [Location: {}]", id);

                return;
            }

            client.execute(() -> handler.accept(packet, player));

            context.get().setPacketHandled(true);
        };
    }

    @Override
    public <M extends AccessoriesPacket> void sendToServer(M packet) {
        this.channel.send(PacketDistributor.SERVER.noArg(), packet);
    }

    @Override
    public <M extends AccessoriesPacket> void sendToPlayer(ServerPlayer player, M packet) {
        this.channel.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    @Override
    public <M extends AccessoriesPacket> void sendToTrackingAndSelf(Entity entity, Supplier<M> packet) {
        this.channel.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), packet.get());
    }
}
