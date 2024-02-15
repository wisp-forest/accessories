package io.wispforest.accessories.neoforge;

import com.mojang.logging.LogUtils;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.networking.AccessoriesNetworkHandler;
import io.wispforest.accessories.networking.AccessoriesPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.handling.IPlayPayloadHandler;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class AccessoriesForgeNetworkHandler extends AccessoriesNetworkHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final AccessoriesForgeNetworkHandler INSTANCE = new AccessoriesForgeNetworkHandler();

    @Nullable
    private IPayloadRegistrar registrar = null;

    @SubscribeEvent
    public void initializeNetworking(final RegisterPayloadHandlerEvent event) {
        this.registrar = event.registrar(Accessories.MODID)
                .versioned("1.0.0");
//                .optional();

        this.register();

        this.init();
    }

    @Override
    public void init() {
        for (ResourceLocation location : List.copyOf(s2cBuilders.keySet())) {
            if(!c2sBuilders.containsKey(location)) continue;

            var builder = s2cBuilders.get(location);

            builder.registerPacket(this::registerBoth);

            s2cBuilders.remove(location);
            c2sBuilders.remove(location);
        }

        s2cBuilders.forEach((location, builder) -> builder.registerPacket(this::registerS2C));
        c2sBuilders.forEach((location, builder) -> builder.registerPacket(this::registerC2S));
    }

    protected <M extends AccessoriesPacket> void registerC2S(Class<M> messageType, Supplier<M> supplier) {
        var location = getId(messageType);

        registrar.play(
                location,
                buf -> new AccessoriesForgePacket<>(supplier.get().readPacket(buf)),
                builder -> {
                    builder.server(
                            (arg, iPayloadContext) -> {
                                var player = iPayloadContext.player();

                                if(player.isEmpty()) {
                                    LOGGER.warn("Player was found to be empty, packet wont be handled! [Location: {}]", arg.id());

                                    return;
                                }

                                iPayloadContext.workHandler().execute(() -> {
                                    arg.innerPacket().handle(player.get());
                                });
                            });
                }
        );
    }

    protected <M extends AccessoriesPacket> void registerS2C(Class<M> messageType, Supplier<M> supplier) {
        var location = getId(messageType);

        registrar.play(
                location,
                buf -> AccessoriesForgePacket.of(supplier, buf),
                builder -> {
                    builder.client(
                            (arg, iPayloadContext) -> {
                                iPayloadContext.workHandler().execute(() -> {
                                    //TODO: FIX CLASSLOADING ISSUE IF PRESENT
                                    var player = iPayloadContext.player().or(() -> Optional.ofNullable(Minecraft.getInstance().player));

                                    if(player.isEmpty()) {
                                        LOGGER.warn("Player was found to be empty, packet wont be handled! [Location: {}]", arg.id());

                                        return;
                                    }

                                    arg.innerPacket().handle(player.get());
                                });
                            });
                }
        );
    }

    protected <M extends AccessoriesPacket> void registerBoth(Class<M> messageType, Supplier<M> supplier) {
        var location = getId(messageType);

        registrar.play(
                location,
                buf -> AccessoriesForgePacket.of(supplier, buf),
                builder -> {
                    IPlayPayloadHandler<AccessoriesForgePacket<M>> handler = (arg, iPayloadContext) -> {
                        var player = iPayloadContext.player();

                        if(player.isEmpty()) {
                            LOGGER.warn("Player was found to be empty, packet wont be handled! [Location: {}]", arg.id());

                            return;
                        }

                        iPayloadContext.workHandler().execute(() -> {
                            arg.innerPacket().handle(player.get());
                        });
                    };

                    builder.client(handler)
                            .server(handler);
                }
        );
    }

    @Override
    public <M extends AccessoriesPacket> void sendToServer(M packet) {
        PacketDistributor.SERVER.with(null).send(new AccessoriesForgePacket<>(packet));
    }

    @Override
    public <M extends AccessoriesPacket> void sendToPlayer(ServerPlayer player, M packet) {
        PacketDistributor.PLAYER.with(player).send(new AccessoriesForgePacket<>(packet));
    }

    @Override
    public <M extends AccessoriesPacket> void sendToTrackingAndSelf(Entity entity, Supplier<M> packet) {
        PacketDistributor.TRACKING_ENTITY_AND_SELF.with(entity).send(new AccessoriesForgePacket<>(packet.get()));
    }
}
