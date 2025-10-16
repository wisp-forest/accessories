package io.wispforest.accessories.networking.client;

import io.wispforest.accessories.impl.option.AccessoriesPlayerOptionsHolder;
import io.wispforest.accessories.utils.EndecUtils;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import net.minecraft.world.entity.player.Player;

public record SyncPlayerOptions(AccessoriesPlayerOptionsHolder options) {
    public static final StructEndec<SyncPlayerOptions> ENDEC = StructEndecBuilder.of(
            EndecUtils.createMapCarrierEndec(AccessoriesPlayerOptionsHolder::new).fieldOf("options", SyncPlayerOptions::options),
            SyncPlayerOptions::new
    );

    //@Environment(EnvType.CLIENT)
    public static void handlePacket(SyncPlayerOptions packet, Player player) {
        EndecUtils.readDataFrom(AccessoriesPlayerOptionsHolder.getOptions(player), packet.options());
    }
}
