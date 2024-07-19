package io.wispforest.accessories.compat;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.client.TargetType;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        public boolean allowSlotScrolling = true;

        @ConfigEntry.Gui.CollapsibleObject()
        public HighlightOptions highlightOptions = new HighlightOptions();

        public static class HighlightOptions {
            public boolean highlightHovered = true;

            @ConfigEntry.Gui.CollapsibleObject()
            public UnselectedOptions unselectedOptions = new UnselectedOptions();

            public static class UnselectedOptions {
                public boolean renderUnselected = true;
                @ConfigEntry.Gui.CollapsibleObject(startExpanded = true)
                public DarkenOptions darkenOptions = new DarkenOptions();

                public static class DarkenOptions {
                    public boolean darkenUnselected = true;
                    public float darkenedBrightness = 0.5f;
                    public float darkenedOpacity = 1f;
                }
            }
        }

        public List<RenderSlotTarget> disabledDefaultRenders = new ArrayList<>();
    }

    public List<SlotAmountModifier> modifiers = new ArrayList<>();

    public static class SlotAmountModifier {
        public String slotType;
        public int amount = 0;
    }

    public static class RenderSlotTarget {
        public String slotType = "";
        public TargetType targetType = TargetType.ALL;
    }
}
