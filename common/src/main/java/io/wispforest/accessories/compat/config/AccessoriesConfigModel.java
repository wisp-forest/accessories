package io.wispforest.accessories.compat.config;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.compat.config.client.Structured;
import io.wispforest.accessories.impl.PlayerEquipControl;
import io.wispforest.owo.config.annotation.*;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.List;

@Config(name = Accessories.MODID, wrapperName = "AccessoriesConfig")
public class AccessoriesConfigModel {

    @Nest
    @Expanded
    public GeneralClientOptions clientOptions = new GeneralClientOptions();

    public static class GeneralClientOptions {

        @Hook
        public PlayerEquipControl equipControl = PlayerEquipControl.MUST_NOT_CROUCH;

        public boolean forceNullRenderReplacement = false;

        public boolean disableEmptySlotScreenError = false;

        public List<RenderSlotTarget> disabledDefaultRenders = new ArrayList<>();
    }

    @Nest
    @Expanded
    public ScreenOptions screenOptions = new ScreenOptions();

    public static class ScreenOptions {

        public ScreenType selectedScreenType = ScreenType.NONE;

        @Hook
        public boolean showUnusedSlots = false;

        public boolean allowSlotScrolling = true;

        // Screen Injected Button offsets

        @SectionHeader("button_offsets")
        @Structured(sideBySide = true) public Vector2i inventoryButtonOffset = new Vector2i(62, 8);
        @Structured(sideBySide = true) public Vector2i creativeInventoryButtonOffset = new Vector2i(96, 6);

        // Experimental Screen

        @SectionHeader("experimental")
        public boolean isDarkMode = false;
        public boolean showEquippedStackSlotType = true;

        // Legacy Screen

        @SectionHeader("legacy")
        public boolean showGroupTabs = true;

        @SectionHeader("hover")
        @Nest public HoveredOptions hoveredOptions = new HoveredOptions();
        @Nest public UnHoveredOptions unHoveredOptions = new UnHoveredOptions();
    }

    public static class HoveredOptions {
        public boolean brightenHovered = true;
        public boolean cycleBrightness = true;

        public boolean line = false;
        public boolean clickbait = false;
    }

    public static class UnHoveredOptions {
        public boolean renderUnHovered = true;

        public boolean darkenUnHovered = true;
        public float darkenedBrightness = 0.5f;
        public float darkenedOpacity = 1f;
    }

    public List<SlotAmountModifier> modifiers = new ArrayList<>();
}
