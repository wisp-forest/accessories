package io.wispforest.accessories.networking.client;

import io.wispforest.accessories.impl.option.AccessoriesPlayerOptionsHolder;
import io.wispforest.accessories.utils.InstanceEndec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.player.Player;

public record SyncPlayerOptions(AccessoriesPlayerOptionsHolder options) {
    public static final StructEndec<SyncPlayerOptions> ENDEC = StructEndecBuilder.of(
            InstanceEndec.constructed(AccessoriesPlayerOptionsHolder::new).fieldOf("options", SyncPlayerOptions::options),
            SyncPlayerOptions::new
    );

    @Environment(EnvType.CLIENT)
    public static void handlePacket(SyncPlayerOptions packet, Player player) {
        AccessoriesPlayerOptionsHolder.getOptions(player).readFrom(packet.options());
    }
}
