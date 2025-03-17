package io.wispforest.accessories.networking.server;

import com.mojang.logging.LogUtils;
import io.wispforest.endec.StructEndec;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

public record NukeAccessories() {

    public static final StructEndec<NukeAccessories> ENDEC = StructEndec.unit(new NukeAccessories());

    private static final Logger LOGGER = LogUtils.getLogger();

    public static void handlePacket(NukeAccessories packet, Player player) {
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