package io.wispforest.accessories.networking;

import com.mojang.logging.LogUtils;
import io.wispforest.accessories.Accessories;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

import java.util.function.Supplier;

public abstract class AccessoriesPacket implements CustomPacketPayload {

    private static final Logger LOGGER = LogUtils.getLogger();

    protected boolean emptyPacket;

    public AccessoriesPacket(){
        this(true);
    }

    public AccessoriesPacket(boolean emptyPacket){
        this.emptyPacket = emptyPacket;
    }

    public static <A extends AccessoriesPacket> A read(Supplier<A> supplier, FriendlyByteBuf buf){
        var packet = supplier.get();

        packet.emptyPacket = false;

        packet.read(buf);

        return packet;
    }

    @Override
    public ResourceLocation id() {
        return AccessoriesNetworkHandler.getId(this.getClass());
    }

    public abstract void write(FriendlyByteBuf buf);

    protected abstract void read(FriendlyByteBuf buf);

    public void handle(Player player){
        if(emptyPacket) {
            throw new IllegalStateException("Unable to handle Packet due to the required read call not happening before handle! [Class: " + this.getClass().getName() + "]");
        }
    }
}
