package io.wispforest.accessories.compat;

import io.wispforest.accessories.Accessories;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = Accessories.MODID)
public class AccessoriesConfig implements ConfigData {

    @ConfigEntry.Gui.CollapsibleObject
    public ClientData clientData = new ClientData();

    public static class ClientData {
        public boolean showGroupTabs = true;
        public boolean showLineRendering = true;

        public int inventoryButtonXOffset = 66;
        public int inventoryButtonYOffset = 9;

        public int creativeInventoryButtonXOffset = 66;
        public int creativeInventoryButtonYOffset = 9;
    }
}
