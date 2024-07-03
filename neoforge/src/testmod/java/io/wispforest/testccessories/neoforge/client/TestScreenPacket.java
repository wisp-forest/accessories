package io.wispforest.testccessories.neoforge.client;

import io.wispforest.accessories.networking.base.HandledPacketPayload;
import io.wispforest.endec.Endec;
import io.wispforest.testccessories.neoforge.TestMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;

public class TestScreenPacket implements HandledPacketPayload {

    public static final Endec<TestScreenPacket> ENDEC = Endec.of((ctx, serializer, value) -> {}, (ctx, serializer) -> new TestScreenPacket());

    private static final MenuProvider INSTANCE = new SimpleMenuProvider(TestMenu::new, Component.literal("TEST"));

    @Override
    public void handle(Player player) {
        if(player instanceof ServerPlayer serverPlayer) serverPlayer.openMenu(INSTANCE);
    }
}
