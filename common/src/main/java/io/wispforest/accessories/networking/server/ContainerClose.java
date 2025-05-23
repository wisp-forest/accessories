package io.wispforest.accessories.networking.server;

import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public record ContainerClose() {
    public static final StructEndec<ContainerClose> ENDEC = Endec.unit(ContainerClose::new);

    public static void handlePacket(ContainerClose packet, Player player) {
        ((ServerPlayer)player).doCloseContainer();
    }
}
