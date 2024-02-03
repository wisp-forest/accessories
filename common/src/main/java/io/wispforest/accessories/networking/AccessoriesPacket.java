package io.wispforest.accessories.networking;

import com.mojang.logging.LogUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

public abstract class AccessoriesPacket {

    private static final Logger LOGGER = LogUtils.getLogger();

    private boolean emptyPacket = true;

    public AccessoriesPacket(){}

    public abstract void write(FriendlyByteBuf buf);

    protected abstract void read(FriendlyByteBuf buf);

    public final <A extends AccessoriesPacket> A readPacket(FriendlyByteBuf buf){
        this.emptyPacket = false;

        read(buf);

        return (A) this;
    }

    public void handle(Player player){
        if(emptyPacket) {
            throw new IllegalStateException("Unable to handle Packet due to the required read call not happening before handle! [Class: " + this.getClass().getName() + "]");
        }
    }
}
