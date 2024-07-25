package io.wispforest.accessories.forge;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.client.gui.AccessoriesScreen;
import net.minecraft.client.gui.screens.MenuScreens;

public class AccessoriesInternalsClientImpl {

    public static void registerToMenuTypes(){
        MenuScreens.register(Accessories.ACCESSORIES_MENU_TYPE, AccessoriesScreen::new);
    }
}
