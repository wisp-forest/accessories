package io.wispforest.accessories.networking.server;

import io.wispforest.accessories.client.AccessoriesMenu;
import io.wispforest.accessories.networking.AccessoriesPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;

public class ScreenOpen extends AccessoriesPacket {

    public ScreenOpen(){
        super(false);
    }

    @Override
    public void write(FriendlyByteBuf buf) {}

    @Override
    protected void read(FriendlyByteBuf buf) {}

    @Override
    public void handle(Player player) {
        player.openMenu(new SimpleMenuProvider((i, inventory, player1) -> new AccessoriesMenu(i, inventory, true, player1), Component.empty()));
    }
}