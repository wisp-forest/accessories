package io.wispforest.accessories.networking.server;

import com.mojang.logging.LogUtils;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.networking.AccessoriesPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

public class NukeAccessories extends AccessoriesPacket {

    private static final Logger LOGGER = LogUtils.getLogger();

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

        // Only players in creative should be able to nuke their accessories
        if (!player.getAbilities().instabuild) {
            LOGGER.info("A given player sent a NukeAccessories packet not as a Creative Player: [Player: {}]", player.getName());

            return;
        }

        var cap = player.accessoriesCapability();

        if (cap != null) {
            cap.reset(false);

            player.containerMenu.broadcastChanges();
        }
    }
}