package io.wispforest.accessories.networking.server;

import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.networking.AccessoriesPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

public class NukeAccessories extends AccessoriesPacket {

    public NukeAccessories(){
        super(false);
    }

    @Override
    public void write(FriendlyByteBuf buf) {}

    @Override
    protected void read(FriendlyByteBuf buf) {}

    @Override
    public void handle(Player player) {
        super.handle(player);

        var cap = AccessoriesCapability.get(player);

        if(cap != null) cap.clear();
    }
}