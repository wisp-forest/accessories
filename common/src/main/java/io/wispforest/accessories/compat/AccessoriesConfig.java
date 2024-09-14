package io.wispforest.accessories.compat;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.PlayerEquipControl;
import io.wispforest.accessories.api.client.TargetType;
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

        public ScreenType selectedScreenType = ScreenType.NONE;

        public int inventoryButtonXOffset = 62;
        public int inventoryButtonYOffset = 8;

        public int creativeInventoryButtonXOffset = 96;
        public int creativeInventoryButtonYOffset = 6;

        public boolean forceNullRenderReplacement = false;

        public boolean disableEmptySlotScreenError = false;

        public boolean allowSlotScrolling = true;

        public PlayerEquipControl equipControl = PlayerEquipControl.MUST_NOT_CROUCH;

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

    public enum ScreenType {
        NONE(-1),
        ORIGINAL(1),
        EXPERIMENTAL_V1(2);

        private final int screenIndex;

        ScreenType(int screenIndex) {
            this.screenIndex = screenIndex;
        }

        public boolean isValid() {
            return this.screenIndex >= 1;
        }
    }
}
