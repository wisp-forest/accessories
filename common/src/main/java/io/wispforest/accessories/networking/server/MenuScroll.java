package io.wispforest.accessories.networking.server;

import io.wispforest.accessories.menu.variants.AccessoriesMenu;
import io.wispforest.accessories.networking.AccessoriesNetworking;
import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public record MenuScroll(int index, boolean smooth) {

    public static final StructEndec<MenuScroll> ENDEC = StructEndecBuilder.of(
            Endec.VAR_INT.fieldOf("index", MenuScroll::index),
            Endec.BOOLEAN.fieldOf("smooth", MenuScroll::smooth),
            MenuScroll::new
    );

    public static void handlePacket(MenuScroll packet, Player player) {
        if(player.containerMenu instanceof AccessoriesMenu menu && menu.scrollTo(packet.index(), packet.smooth()) && player instanceof ServerPlayer serverPlayer){
            AccessoriesNetworking.sendToPlayer(serverPlayer, new MenuScroll(packet.index(), packet.smooth()));
        }
    }
}
