package io.wispforest.accessories.networking.client;

import io.wispforest.accessories.AccessoriesAccess;
import io.wispforest.accessories.client.gui.AccessoriesScreen;
import io.wispforest.accessories.networking.AccessoriesPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

public class SyncCosmeticsMenuToggle extends AccessoriesPacket {

    private boolean isCosmeticsToggled;

    public SyncCosmeticsMenuToggle(){}

    public SyncCosmeticsMenuToggle(boolean isCosmeticsToggled){
        this.isCosmeticsToggled = isCosmeticsToggled;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(this.isCosmeticsToggled);
    }

    @Override
    protected void read(FriendlyByteBuf buf) {
        this.isCosmeticsToggled = buf.readBoolean();
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void handle(Player player) {
        super.handle(player);

        AccessoriesAccess.modifyHolder(player, holder -> holder.cosmeticsShown(this.isCosmeticsToggled));

        if(Minecraft.getInstance().screen instanceof AccessoriesScreen accessoriesScreen) {
            accessoriesScreen.updateCosmeticToggleButton();
        }
    }
}
