package io.wispforest.accessories.networking.holder;

import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.client.gui.AccessoriesScreen;
import io.wispforest.accessories.networking.BaseAccessoryPacket;
import io.wispforest.accessories.networking.base.HandledPacketPayload;
import io.wispforest.endec.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.function.Function;

public record SyncHolderChange(HolderProperty<?> property, Object data) implements BaseAccessoryPacket {

    public static final Endec<SyncHolderChange> ENDEC = new StructEndec<SyncHolderChange>() {
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
        return new SyncHolderChange(property, operation.apply(property.getter().apply(player.accessoriesHolder())));
    }

    @Override
    public void handle(Player player) {
        this.property.setData(player, this.data);

        if(player.level().isClientSide()) {
            handleClient(player);
        } else {
            AccessoriesInternals.getNetworkHandler().sendToPlayer((ServerPlayer) player, SyncHolderChange.of((HolderProperty<Object>) this.property, (Object) this.property.getter().apply(player.accessoriesHolder())));
        }
    }

    @Environment(EnvType.CLIENT)
    public void handleClient(Player player) {
        if(Minecraft.getInstance().screen instanceof AccessoriesScreen accessoriesScreen) {
            accessoriesScreen.updateButtons(this.property.name());
        }
    }
}