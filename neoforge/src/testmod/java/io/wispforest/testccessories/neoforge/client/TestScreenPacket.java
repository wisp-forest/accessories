package io.wispforest.testccessories.neoforge.client;

import io.wispforest.endec.StructEndec;
import io.wispforest.testccessories.neoforge.TestMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;

public record TestScreenPacket() {

    public static final StructEndec<TestScreenPacket> ENDEC = StructEndec.unit(new TestScreenPacket());

    private static final MenuProvider INSTANCE = new SimpleMenuProvider(TestMenu::new, Component.literal("TEST"));

    public static void handlePacket(TestScreenPacket packet, Player player) {
        if(player instanceof ServerPlayer serverPlayer) serverPlayer.openMenu(INSTANCE);
    }
}
