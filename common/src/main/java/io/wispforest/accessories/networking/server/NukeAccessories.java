package io.wispforest.accessories.networking.server;

import io.wispforest.accessories.AccessoriesAccess;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.networking.AccessoriesPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.ContainerHelper;
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

        AccessoriesAccess.getCapability(player).ifPresent(AccessoriesCapability::clear);
    }
}