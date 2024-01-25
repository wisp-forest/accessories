package io.wispforest.accessories.networking;

import io.netty.buffer.Unpooled;
import io.wispforest.accessories.AccessoriesAccess;
import io.wispforest.accessories.networking.client.SyncContainers;
import io.wispforest.accessories.networking.client.SyncData;
import io.wispforest.accessories.networking.server.ScreenOpen;
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
        registerS2C(SyncContainers.class, SyncContainers::new);
        registerS2C(SyncData.class, SyncData::new);

        registerC2S(ScreenOpen.class, ScreenOpen::new);
    }

    protected abstract <M extends AccessoriesPacket> void registerC2S(Class<M> messageType, Supplier<M> supplier);

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
