package io.wispforest.accessories.networking.base;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.endec.MinecraftEndecs;
import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.ReflectiveEndecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class BaseNetworkHandler {

    protected final Map<Type<?>, PacketBuilder<?>> c2sBuilders = new LinkedHashMap<>();
    protected final Map<Type<?>, PacketBuilder<?>> s2cBuilders = new LinkedHashMap<>();

    private final ReflectiveEndecBuilder endecBuilder;

    protected BaseNetworkHandler(Consumer<NetworkBuilderRegister> builder) {
        this.endecBuilder = MinecraftEndecs.withExtra(new ReflectiveEndecBuilder());

        builder.accept(createRegister());
    }

    private boolean freezeRegistration = false;

    protected void init() {
        this.freezeRegistration = true;
    }

    @Environment(EnvType.CLIENT)
    public abstract <M extends HandledPacketPayload> void sendToServer(M packet);

    public abstract <M extends HandledPacketPayload> void sendToPlayer(ServerPlayer player, M packet);

    public <M extends HandledPacketPayload> void sendToAllPlayers(MinecraftServer server, M packet){
        for (var player : server.getPlayerList().getPlayers()) sendToPlayer(player, packet);
    }

    public <M extends HandledPacketPayload> void sendToTrackingAndSelf(Entity entity, M packet) {
       sendToTrackingAndSelf(entity, (Supplier<M>) () -> packet);
    }

    public abstract <M extends HandledPacketPayload> void sendToTrackingAndSelf(Entity entity, Supplier<M> packet);

    //--

    protected final NetworkBuilderRegister createRegister() {
        return new NetworkBuilderRegister() {
            @Override
            public ReflectiveEndecBuilder builder() {
                return BaseNetworkHandler.this.endecBuilder;
            }

            @Override
            public <M extends HandledPacketPayload> void registerBuilderC2S(Class<M> messageType, Endec<M> endec){
                checkIfFrozen("C2S", messageType);

                var builder = PacketBuilder.of(messageType, endec);

                if(BaseNetworkHandler.this.c2sBuilders.containsKey(builder.id())) {
                    throw new IllegalStateException("Unable to register the given C2S packet as it already exists within the handler Map! [Class: " + messageType.getSimpleName() + "]");
                }

                BaseNetworkHandler.this.c2sBuilders.put(builder.id(), builder);
            }

            @Override
            public <M extends HandledPacketPayload> void registerBuilderS2C(Class<M> messageType, Endec<M> endec){
                checkIfFrozen("S2C", messageType);

                var builder = PacketBuilder.of(messageType, endec);

                if(BaseNetworkHandler.this.c2sBuilders.containsKey(builder.id())) {
                    throw new IllegalStateException("Unable to register the given S2C packet as it already exists within the handler Map! [Class: " + messageType.getSimpleName() + "]");
                }

                BaseNetworkHandler.this.s2cBuilders.put(builder.id(), builder);
            }

            @Override
            public <M extends HandledPacketPayload> void registerBuilderBiDi(Class<M> messageType, Endec<M> endec){
                checkIfFrozen("Bi-Directional", messageType);

                var builder = PacketBuilder.of(messageType, endec);

                if(BaseNetworkHandler.this.c2sBuilders.containsKey(builder.id()) || BaseNetworkHandler.this.s2cBuilders.containsKey(builder.id()) ) {
                    throw new IllegalStateException("Unable to register the given Bi-Directional packet as it already exists within the handler Map! [Class: " + messageType.getSimpleName() + "]");
                }

                BaseNetworkHandler.this.c2sBuilders.put(builder.id(), builder);
                BaseNetworkHandler.this.s2cBuilders.put(builder.id(), builder);
            }
        };
    }

    private void checkIfFrozen(String direction, Class<?> packetType) {
        if(!freezeRegistration) return;

        throw new IllegalStateException("Unable to register the given " + direction + " builder as network registration has occurred! [Class: " + packetType.getSimpleName() + "]");
    }

    protected record PacketBuilder<M extends HandledPacketPayload>(Type<M> id, Class<M> clazz, Endec<M> endec) {
        public static <M extends HandledPacketPayload> PacketBuilder<M> of(Class<M> clazz, Endec<M> endec){
            return new PacketBuilder<>(getId(clazz), clazz, endec);
        }

        public void registerPacket(PacketBuilderConsumer registerFunc){
            registerFunc.accept(clazz(), endec());
        }
    }

    public static <M extends HandledPacketPayload> Type<M> getId(Class<M> mClass){
        return new Type<>(Accessories.of(mClass.getName().toLowerCase()));
    }


}