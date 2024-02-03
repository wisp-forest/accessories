package io.wispforest.accessories.networking.server;

import io.wispforest.accessories.AccessoriesAccess;
import io.wispforest.accessories.client.AccessoriesMenu;
import io.wispforest.accessories.networking.AccessoriesPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class MenuScroll extends AccessoriesPacket {

    private int index;
    private boolean smooth;

    public MenuScroll(){
        super();
    }

    public MenuScroll(int index, boolean smooth){
        this.index = index;
        this.smooth = smooth;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.index);
        buf.writeBoolean(this.smooth);
    }

    @Override
    protected void read(FriendlyByteBuf buf) {
        this.index = buf.readVarInt();
        this.smooth = buf.readBoolean();
    }

    @Override
    public void handle(Player player) {
        super.handle(player);

        if(player.containerMenu instanceof AccessoriesMenu menu && menu.scrollTo(this.index, this.smooth) && player instanceof ServerPlayer serverPlayer){
            AccessoriesAccess.getNetworkHandler().sendToPlayer(serverPlayer, new MenuScroll(this.index, this.smooth));
        }
    }
}
