package io.wispforest.accessories.neoforge;


import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

public class AccessoriesInternalsClientImpl {

    public static <M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> void registerMenuConstructor(MenuType<? extends M> type, MenuScreens.ScreenConstructor<M, U> factory) {
        MenuScreens.register(type, factory);
    }
}
