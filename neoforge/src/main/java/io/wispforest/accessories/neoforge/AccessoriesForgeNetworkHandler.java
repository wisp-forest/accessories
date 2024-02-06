package io.wispforest.accessories.neoforge;

import com.mojang.logging.LogUtils;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.networking.AccessoriesNetworkHandler;
import io.wispforest.accessories.networking.AccessoriesPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.function.Supplier;

public class AccessoriesForgeNetworkHandler extends AccessoriesNetworkHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final AccessoriesForgeNetworkHandler INSTANCE = new AccessoriesForgeNetworkHandler();

    @Nullable
    private IPayloadRegistrar registrar = null;

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlerEvent event) {
        AccessoriesForgeNetworkHandler.INSTANCE.registrar = event.registrar(Accessories.MODID)
                .versioned("1.0.0")
                .optional();

        AccessoriesForgeNetworkHandler.INSTANCE.register();
        AccessoriesForgeNetworkHandler.INSTANCE.registerClient();
    }

    @Override
    protected <M extends AccessoriesPacket> void registerC2S(Class<M> messageType, Supplier<M> supplier) {
        var location = getId(messageType);

        registrar.common(
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

    @Override
    protected <M extends AccessoriesPacket> void registerS2CDeferred(Class<M> messageType, Supplier<M> supplier) {
        // Unused due to how neoforge handles packets
    }

    @Override
    protected <M extends AccessoriesPacket> void registerS2C(Class<M> messageType, Supplier<M> supplier) {
        var location = getId(messageType);

        registrar.common(
                location,
                buf -> new AccessoriesForgePacket<>(supplier.get().readPacket(buf)),
                builder -> {
                    builder.client(
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

    public <M extends AccessoriesPacket> ResourceLocation getId(Class<M> mClass){
        return new ResourceLocation(Accessories.MODID, mClass.getName().toLowerCase());
    }
}
