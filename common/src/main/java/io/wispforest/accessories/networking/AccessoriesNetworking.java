package io.wispforest.accessories.networking;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.mixin.ServerChunkLoadingManagerAccessor;
import io.wispforest.accessories.networking.client.*;
import io.wispforest.accessories.networking.holder.SyncOptionChange;
import io.wispforest.accessories.networking.server.MenuScroll;
import io.wispforest.accessories.networking.server.NukeAccessories;
import io.wispforest.accessories.networking.server.ScreenOpen;
import io.wispforest.accessories.networking.server.SyncCosmeticToggle;
import io.wispforest.owo.network.ClientAccess;
import io.wispforest.owo.network.OwoNetChannel;
import io.wispforest.owo.network.ServerAccess;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class AccessoriesNetworking {

    public static final OwoNetChannel CHANNEL = OwoNetChannel.create(Accessories.of("main"));

    public static void init() {
        CHANNEL.registerServerbound(ScreenOpen.class, ScreenOpen.ENDEC, serverHandler(ScreenOpen::handlePacket));
        CHANNEL.registerServerbound(NukeAccessories.class, NukeAccessories.ENDEC, serverHandler(NukeAccessories::handlePacket));
        CHANNEL.registerServerbound(SyncCosmeticToggle.class, SyncCosmeticToggle.ENDEC, serverHandler(SyncCosmeticToggle::handlePacket));

        CHANNEL.registerServerbound(MenuScroll.class, MenuScroll.ENDEC, serverHandler(MenuScroll::handlePacket));
        CHANNEL.registerServerbound(SyncOptionChange.class, SyncOptionChange.ENDEC, serverHandler(SyncOptionChange::handlePacket));

        //--

        CHANNEL.registerClientboundDeferred(SyncEntireContainer.class, SyncEntireContainer.ENDEC);
        CHANNEL.registerClientboundDeferred(SyncContainerData.class, SyncContainerData.ENDEC);
        CHANNEL.registerClientboundDeferred(SyncData.class, SyncData.ENDEC);
        CHANNEL.registerClientboundDeferred(SyncPlayerOptions.class, SyncPlayerOptions.ENDEC);
        CHANNEL.registerClientboundDeferred(AccessoryBreak.class, AccessoryBreak.ENDEC);
        CHANNEL.registerClientboundDeferred(InvalidateEntityCache.class, InvalidateEntityCache.ENDEC);
        CHANNEL.registerClientboundDeferred(ScreenVariantPing.class, ScreenVariantPing.ENDEC);

        CHANNEL.registerClientboundDeferred(MenuScroll.class, MenuScroll.ENDEC);
        CHANNEL.registerClientboundDeferred(SyncOptionChange.class, SyncOptionChange.ENDEC);
    }

    @Environment(EnvType.CLIENT)
    public static void initClient() {
        CHANNEL.registerClientbound(SyncEntireContainer.class, SyncEntireContainer.ENDEC, clientHandler(SyncEntireContainer::handlePacket));
        CHANNEL.registerClientbound(SyncContainerData.class, SyncContainerData.ENDEC, clientHandler(SyncContainerData::handlePacket));
        CHANNEL.registerClientbound(SyncData.class, SyncData.ENDEC, clientHandler(SyncData::handlePacket));
        CHANNEL.registerClientbound(SyncPlayerOptions.class, SyncPlayerOptions.ENDEC, clientHandler(SyncPlayerOptions::handlePacket));
        CHANNEL.registerClientbound(AccessoryBreak.class, AccessoryBreak.ENDEC, clientHandler(AccessoryBreak::handlePacket));
        CHANNEL.registerClientbound(InvalidateEntityCache.class, InvalidateEntityCache.ENDEC, clientHandler(InvalidateEntityCache::handlePacket));
        CHANNEL.registerClientbound(ScreenVariantPing.class, ScreenVariantPing.ENDEC, clientHandler(ScreenVariantPing::handlePacket));

        CHANNEL.registerClientbound(MenuScroll.class, MenuScroll.ENDEC, clientHandler(MenuScroll::handlePacket));
        CHANNEL.registerClientbound(SyncOptionChange.class, SyncOptionChange.ENDEC, clientHandler(SyncOptionChange::handlePacket));
    }

    @Environment(EnvType.CLIENT)
    public static <R extends Record> OwoNetChannel.ChannelHandler<R, ClientAccess> clientHandler(BiConsumer<R, Player> consumer) {
        return (r, access) -> consumer.accept(r, access.player());
    }

    public static <R extends Record> OwoNetChannel.ChannelHandler<R, ServerAccess> serverHandler(BiConsumer<R, Player> consumer) {
        return (r, access) -> consumer.accept(r, access.player());
    }

    @Environment(EnvType.CLIENT)
    public static <R extends Record> void sendToServer(R packet) {
        CHANNEL.clientHandle().send(packet);
    }

    public static <R extends Record> void sendToPlayer(ServerPlayer player, R packet) {
        CHANNEL.serverHandle(player).send(packet);
    }

    public static <R extends Record> void sendToAllPlayers(MinecraftServer server, R packet){
        for (var player : server.getPlayerList().getPlayers()) sendToPlayer(player, packet);
    }

    public static <R extends Record> void sendToTrackingAndSelf(Entity entity, R packet) {
        var targets = new ArrayList<ServerPlayer>();

        if (entity.level().getChunkSource() instanceof ServerChunkCache chunkCache) {
            var chunkLoadingManager = chunkCache.chunkMap;
            var tracker = ((ServerChunkLoadingManagerAccessor) chunkLoadingManager).accessories$getEntityMap().get(entity.getId());

            // return an immutable collection to guard against accidental removals.
            if (tracker != null) {
                targets.addAll(
                        tracker.accessories$getSeenBy().stream()
                                .map(ServerPlayerConnection::getPlayer)
                                .collect(Collectors.toUnmodifiableSet())
                );
            }
        }

        if (entity instanceof ServerPlayer serverPlayer) targets.add(serverPlayer);

        CHANNEL.serverHandle(targets).send(packet);
    }

}
