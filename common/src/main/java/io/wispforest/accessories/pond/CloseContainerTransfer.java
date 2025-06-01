package io.wispforest.accessories.pond;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.Screen;

@Environment(EnvType.CLIENT)
public interface CloseContainerTransfer {
    void accessories$setScreenTransfer(Screen screen);
}
