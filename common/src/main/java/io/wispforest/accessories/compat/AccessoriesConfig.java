package io.wispforest.accessories.compat;

import io.wispforest.accessories.Accessories;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.ArrayList;
import java.util.List;

@Config(name = Accessories.MODID)
public class AccessoriesConfig implements ConfigData {

    @ConfigEntry.Gui.CollapsibleObject(startExpanded = true)
    public ClientData clientData = new ClientData();

    public static class ClientData {
        public boolean showGroupTabs = true;

        public boolean showUniqueRendering = false;
        public boolean showLineRendering = true;

        public int inventoryButtonXOffset = 66;
        public int inventoryButtonYOffset = 9;

        public int creativeInventoryButtonXOffset = 96;
        public int creativeInventoryButtonYOffset = 7;

        public boolean forceNullRenderReplacement = false;

        public boolean disableEmptySlotScreenError = false;
    }

    public List<SlotAmountModifier> modifiers = new ArrayList<>();

    public static class SlotAmountModifier {
        public String slotType;
        public int amount = 0;
    }
}