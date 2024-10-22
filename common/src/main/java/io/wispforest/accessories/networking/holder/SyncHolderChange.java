package io.wispforest.accessories.networking.holder;

import io.wispforest.accessories.client.gui.AccessoriesScreenBase;
import io.wispforest.accessories.impl.AccessoriesHolderImpl;
import io.wispforest.accessories.networking.AccessoriesNetworking;
import io.wispforest.endec.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.function.Function;

public record SyncHolderChange(HolderProperty<?> property, Object data) {

    public static final StructEndec<SyncHolderChange> ENDEC = new StructEndec<>() {
        @Override
        public void encodeStruct(SerializationContext ctx, Serializer<?> serializer, Serializer.Struct struct, SyncHolderChange value) {
            struct.field("property", ctx, HolderProperty.ENDEC, value.property());
            struct.field("value", ctx, (Endec) value.property().endec(), value.data());
        }

        @Override
        public SyncHolderChange decodeStruct(SerializationContext ctx, Deserializer<?> deserializer, Deserializer.Struct struct) {
            var prop = struct.field("property", ctx, HolderProperty.ENDEC);

            return new SyncHolderChange(prop, struct.field("value", ctx, prop.endec()));
        }
    };

    public static <T> SyncHolderChange of(HolderProperty<T> property, T data) {
        return new SyncHolderChange(property, data);
    }

    public static <T> SyncHolderChange of(HolderProperty<T> property, Player player, Function<T, T> operation) {
        return new SyncHolderChange(property, operation.apply(property.getter().apply(AccessoriesHolderImpl.getHolder(player))));
    }

    public static void handlePacket(SyncHolderChange packet, Player player) {
        packet.property().setData(player, packet.data());

        if(player.level().isClientSide()) {
            handleClient(packet, player);
        } else {
            AccessoriesNetworking.sendToPlayer((ServerPlayer) player, SyncHolderChange.of((HolderProperty<Object>) packet.property(), (Object) packet.property().getter().apply(AccessoriesHolderImpl.getHolder(player))));
        }
    }

    @Environment(EnvType.CLIENT)
    public static void handleClient(SyncHolderChange packet, Player player) {
        if(Minecraft.getInstance().screen instanceof AccessoriesScreenBase accessoriesScreen) {
            accessoriesScreen.onHolderChange(packet.property().name());
        }
    }
}