package io.wispforest.accessories;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

public class AccessoriesInternalsClient {

    @ExpectPlatform
    public static <M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> void registerMenuConstructor(MenuType<? extends M> type, MenuScreens.ScreenConstructor<M, U> factory) {
        throw new AssertionError();
    }
}
