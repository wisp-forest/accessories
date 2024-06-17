package io.wispforest.accessories.networking;

import com.mojang.logging.LogUtils;
import io.wispforest.accessories.Accessories;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

import java.util.function.Supplier;

public interface AccessoriesPacket extends CustomPacketPayload {

    Logger LOGGER = LogUtils.getLogger();

    @Override
    default Type<? extends CustomPacketPayload> type() {
        return AccessoriesNetworkHandler.getId(this.getClass());
    }

    default void handle(Player player){
        //NOOP
    }
}
