package io.wispforest.accessories.compat;

import io.wispforest.accessories.Accessories;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.ArrayList;
import java.util.List;

@Config(name = Accessories.MODID)
public class AccessoriesConfig implements ConfigData {

    @ConfigEntry.Gui.CollapsibleObject
    public ClientData clientData = new ClientData();

    public static class ClientData {
        public boolean showGroupTabs = true;
        public boolean showLineRendering = true;

        public int inventoryButtonXOffset = 67;
        public int inventoryButtonYOffset = 8;

        public int creativeInventoryButtonXOffset = 319;
        public int creativeInventoryButtonYOffset = 122;
    }

    public List<SlotAmountModifier> modifiers = new ArrayList<>();

    public static class SlotAmountModifier {
        public String slotType;
        public int amount = 0;
    }
}