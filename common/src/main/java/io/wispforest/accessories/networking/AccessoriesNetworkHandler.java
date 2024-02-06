package io.wispforest.accessories.networking;

import io.netty.buffer.Unpooled;
import io.wispforest.accessories.AccessoriesAccess;
import io.wispforest.accessories.networking.client.*;
import io.wispforest.accessories.networking.server.ScreenOpen;
import io.wispforest.accessories.networking.server.MenuScroll;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.function.Supplier;

public abstract class AccessoriesNetworkHandler {

    public static Supplier<MinecraftServer> server = () -> null;

    public static FriendlyByteBuf createBuf() {
        return new FriendlyByteBuf(Unpooled.buffer());
    }

    public final void register() {
        registerS2CDeferred(SyncContainer.class, SyncContainer::new);
        registerS2CDeferred(SyncContainerData.class, SyncContainerData::new);
        registerS2CDeferred(SyncData.class, SyncData::new);
        registerS2CDeferred(MenuScroll.class, MenuScroll::new);

        registerS2CDeferred(SyncCosmeticsMenuToggle.class, SyncCosmeticsMenuToggle::new);
        registerS2CDeferred(SyncLinesMenuToggle.class, SyncLinesMenuToggle::new);

        registerC2S(ScreenOpen.class, ScreenOpen::new);
        registerC2S(MenuScroll.class, MenuScroll::new);
    }

    @Environment(EnvType.CLIENT)
    public final void registerClient(){
        registerS2C(SyncContainer.class, SyncContainer::new);
        registerS2C(SyncContainerData.class, SyncContainerData::new);
        registerS2C(SyncData.class, SyncData::new);
        registerS2C(MenuScroll.class, MenuScroll::new);

        registerS2C(SyncCosmeticsMenuToggle.class, SyncCosmeticsMenuToggle::new);
        registerS2C(SyncLinesMenuToggle.class, SyncLinesMenuToggle::new);
    }

    protected abstract <M extends AccessoriesPacket> void registerC2S(Class<M> messageType, Supplier<M> supplier);

    protected abstract <M extends AccessoriesPacket> void registerS2CDeferred(Class<M> messageType, Supplier<M> supplier);

    @Environment(EnvType.CLIENT)
    protected abstract <M extends AccessoriesPacket> void registerS2C(Class<M> messageType, Supplier<M> supplier);

    @Environment(EnvType.CLIENT)
    public abstract <M extends AccessoriesPacket> void sendToServer(M packet);

    public abstract <M extends AccessoriesPacket> void sendToPlayer(ServerPlayer player, M packet);

    public <M extends AccessoriesPacket> void sendToAllPlayers(M packet){
        for (var player : server.get().getPlayerList().getPlayers()) sendToPlayer(player, packet);
    }

    public <M extends AccessoriesPacket> void sendToTrackingAndSelf(Entity entity, M packet) {
       sendToTrackingAndSelf(entity, (Supplier<M>) () -> packet);
    }

    public <M extends AccessoriesPacket> void sendToTrackingAndSelf(Entity entity, Supplier<M> packet) {
        if(entity.level().isClientSide) return;

        var players = AccessoriesAccess.getInternal().getTracking(entity);

        for (var player : players) sendToPlayer(player, packet.get());

        if(entity instanceof ServerPlayer serverPlayer) sendToPlayer(serverPlayer, packet.get());
    }
}
