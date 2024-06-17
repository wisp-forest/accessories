package io.wispforest.accessories.networking;

import io.netty.buffer.Unpooled;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.networking.client.*;
import io.wispforest.accessories.networking.holder.SyncHolderChange;
import io.wispforest.accessories.networking.server.NukeAccessories;
import io.wispforest.accessories.networking.server.ScreenOpen;
import io.wispforest.accessories.networking.server.MenuScroll;
import io.wispforest.accessories.networking.server.SyncCosmeticToggle;
import io.wispforest.endec.Endec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public abstract class AccessoriesNetworkHandler {

    public static Supplier<MinecraftServer> server = () -> null;

    protected final Map<CustomPacketPayload.Type<?>, NetworkPacketBuilder<?>> c2sBuilders = new HashMap<>();
    protected final Map<CustomPacketPayload.Type<?>, NetworkPacketBuilder<?>> s2cBuilders = new HashMap<>();

    public static FriendlyByteBuf createBuf() {
        return new FriendlyByteBuf(Unpooled.buffer());
    }

    public abstract void init();

    public final void register() {
        registerBuilderC2S(ScreenOpen.class, ScreenOpen.ENDEC);
        registerBuilderC2S(NukeAccessories.class, NukeAccessories.ENDEC);
        registerBuilderC2S(SyncCosmeticToggle.class, SyncCosmeticToggle.ENDEC);

        registerBuilderS2C(SyncEntireContainer.class, SyncEntireContainer.ENDEC);
        registerBuilderS2C(SyncContainerData.class, SyncContainerData.ENDEC);
        registerBuilderS2C(SyncData.class, SyncData.ENDEC);

        registerBuilderBiDi(MenuScroll.class, MenuScroll.ENDEC);
        registerBuilderBiDi(SyncHolderChange.class, SyncHolderChange.ENDEC);
    }

    protected <M extends AccessoriesPacket> void registerBuilderC2S(Class<M> messageType, Endec<M> endec){
        var builder = NetworkPacketBuilder.of(messageType, endec);

        this.c2sBuilders.put(builder.id(), builder);
    }

    protected <M extends AccessoriesPacket> void registerBuilderS2C(Class<M> messageType, Endec<M> endec){
        var builder = NetworkPacketBuilder.of(messageType, endec);

        this.s2cBuilders.put(builder.id(), builder);
    }

    protected <M extends AccessoriesPacket> void registerBuilderBiDi(Class<M> messageType, Endec<M> endec){
        var builder = NetworkPacketBuilder.of(messageType, endec);

        this.c2sBuilders.put(builder.id(), builder);
        this.s2cBuilders.put(builder.id(), builder);
    }

    @Environment(EnvType.CLIENT)
    public abstract <M extends AccessoriesPacket> void sendToServer(M packet);

    public abstract <M extends AccessoriesPacket> void sendToPlayer(ServerPlayer player, M packet);

    public <M extends AccessoriesPacket> void sendToAllPlayers(M packet){
        for (var player : server.get().getPlayerList().getPlayers()) sendToPlayer(player, packet);
    }

    public <M extends AccessoriesPacket> void sendToTrackingAndSelf(Entity entity, M packet) {
       sendToTrackingAndSelf(entity, (Supplier<M>) () -> packet);
    }

    public abstract <M extends AccessoriesPacket> void sendToTrackingAndSelf(Entity entity, Supplier<M> packet);

    public record NetworkPacketBuilder<M extends AccessoriesPacket>(CustomPacketPayload.Type<M> id, Class<M> clazz, Endec<M> endec) {
        public static <M extends AccessoriesPacket> NetworkPacketBuilder<M> of(Class<M> clazz, Endec<M> endec){
            return new NetworkPacketBuilder<>(getId(clazz), clazz, endec);
        }

        public void registerPacket(BiConsumer<Class<M>, Endec<M>> registerFunc){
            registerFunc.accept(clazz(), endec());
        }
    }

    public static <M extends AccessoriesPacket> CustomPacketPayload.Type<M> getId(Class<M> mClass){
        return new CustomPacketPayload.Type<>(Accessories.of(mClass.getName().toLowerCase()));
    }
}