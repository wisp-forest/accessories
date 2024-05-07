package io.wispforest.accessories.networking;

import io.netty.buffer.Unpooled;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.networking.client.*;
import io.wispforest.accessories.networking.holder.SyncHolderChange;
import io.wispforest.accessories.networking.server.NukeAccessories;
import io.wispforest.accessories.networking.server.ScreenOpen;
import io.wispforest.accessories.networking.server.MenuScroll;
import io.wispforest.accessories.networking.server.SyncCosmeticToggle;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.FriendlyByteBuf;
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

    protected final Map<ResourceLocation, NetworkPacketBuilder<?>> c2sBuilders = new HashMap<>();
    protected final Map<ResourceLocation, NetworkPacketBuilder<?>> s2cBuilders = new HashMap<>();

    public static FriendlyByteBuf createBuf() {
        return new FriendlyByteBuf(Unpooled.buffer());
    }

    public abstract void init();

    public final void register() {
        registerBuilderC2S(ScreenOpen.class, ScreenOpen::new);
        registerBuilderC2S(MenuScroll.class, MenuScroll::new);
        registerBuilderC2S(NukeAccessories.class, NukeAccessories::new);
        registerBuilderC2S(SyncCosmeticToggle.class, SyncCosmeticToggle::new);

        registerBuilderS2C(SyncEntireContainer.class, SyncEntireContainer::new);
        registerBuilderS2C(SyncContainerData.class, SyncContainerData::new);
        registerBuilderS2C(SyncData.class, SyncData::new);
        registerBuilderS2C(MenuScroll.class, MenuScroll::new);

        registerBuilderS2C(SyncHolderChange.class, SyncHolderChange::new);
        registerBuilderC2S(SyncHolderChange.class, SyncHolderChange::new);
    }

    protected <M extends AccessoriesPacket> void registerBuilderC2S(Class<M> messageType, Supplier<M> supplier){
        var builder = NetworkPacketBuilder.of(messageType, supplier);

        this.c2sBuilders.put(builder.location(), builder);
    }

    protected <M extends AccessoriesPacket> void registerBuilderS2C(Class<M> messageType, Supplier<M> supplier){
        var builder = NetworkPacketBuilder.of(messageType, supplier);

        this.s2cBuilders.put(builder.location(), builder);
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

    public record NetworkPacketBuilder<M extends AccessoriesPacket>(ResourceLocation location, Class<M> clazz, Supplier<M> supplier) {
        public static <M extends AccessoriesPacket> NetworkPacketBuilder<M> of(Class<M> clazz, Supplier<M> supplier){
            return new NetworkPacketBuilder<>(getId(clazz), clazz, supplier);
        }

        public void registerPacket(BiConsumer<Class<M>, Supplier<M>> registerFunc){
            registerFunc.accept(clazz(), supplier());
        }
    }

    public static <M extends AccessoriesPacket> ResourceLocation getId(Class<M> mClass){
        return new ResourceLocation(Accessories.MODID, mClass.getName().toLowerCase());
    }
}