package io.wispforest.accessories.api.client.screen;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.Nullable;

public interface ScreenBasedTargetGetter<M extends AbstractContainerMenu, S extends AbstractContainerScreen<M>> {

    ScreenBasedTargetGetter<AbstractContainerMenu, AbstractContainerScreen<AbstractContainerMenu>> PLAYER_DEFAULTED_TARGET = screen -> null;

    @Nullable LivingEntity getTarget(S screen);
}
