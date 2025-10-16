package io.wispforest.accessories.api.client.screen;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public interface ScreenReopener<M extends AbstractContainerMenu, S extends AbstractContainerScreen<M>> {

    ScreenReopener<AbstractContainerMenu, AbstractContainerScreen<AbstractContainerMenu>> PLAYER_INVENTORY = (player, targetEntity, prevScreen) -> false;

    ScreenReopener<AbstractContainerMenu, AbstractContainerScreen<AbstractContainerMenu>> CUSTOM_INVENTORY = (player, targetEntity, prevScreen) -> ScreenOpener.CUSTOM_INVENTORY.openScreen(player, targetEntity);

    boolean reopenScreen(Player player, LivingEntity targetEntity, S prevScreen);
}
