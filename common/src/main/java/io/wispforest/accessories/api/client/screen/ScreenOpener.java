package io.wispforest.accessories.api.client.screen;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public interface ScreenOpener {

    ScreenOpener CUSTOM_INVENTORY = (player, targetEntity) -> {
        if (player.getVehicle() instanceof HasCustomInventoryScreen){
            ((LocalPlayer) player).connection.send(new ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.OPEN_INVENTORY));

            return true;
        }

        return false;
    };

    boolean openScreen(Player player, LivingEntity targetEntity);
}
