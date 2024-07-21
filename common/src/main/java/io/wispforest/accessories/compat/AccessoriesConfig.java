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

        public int inventoryButtonXOffset = 66;
        public int inventoryButtonYOffset = 9;

        public int creativeInventoryButtonXOffset = 96;
        public int creativeInventoryButtonYOffset = 7;

        public boolean forceNullRenderReplacement = false;

        public boolean disableEmptySlotScreenError = false;

        @ConfigEntry.Gui.CollapsibleObject()
        public HoverOptions hoverOptions = new HoverOptions();

        public static class HoverOptions {

            @ConfigEntry.Gui.CollapsibleObject()
            public HoveredOptions hoveredOptions = new HoveredOptions();

            public static class HoveredOptions {
                public boolean brightenHovered = true;
                public float hoveredBrightness = 1.5f;
                public float hoveredOpacity = 1f;
                public boolean cycleBrightness = true;
                public float cycleSpeed = 0.1f;
                public float minBrightness = 0.5f;
                public float minOpacity = 1f;

                public boolean line = false;
                public boolean clickbait = false;
            }

            @ConfigEntry.Gui.CollapsibleObject()
            public UnHoveredOptions unHoveredOptions = new UnHoveredOptions();

            public static class UnHoveredOptions {
                public boolean renderUnHovered = true;
                public boolean onlyWhileHovered = true;

                public boolean darkenUnHovered = true;
                public float darkenedBrightness = 0.5f;
                public float darkenedOpacity = 1f;
            }
        }
    }

    public List<SlotAmountModifier> modifiers = new ArrayList<>();

    public static class SlotAmountModifier {
        public String slotType;
        public int amount = 0;
    }
}
