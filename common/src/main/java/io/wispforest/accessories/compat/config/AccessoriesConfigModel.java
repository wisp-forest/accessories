package io.wispforest.accessories.compat.config;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.impl.PlayerEquipControl;
import io.wispforest.owo.config.Option;
import io.wispforest.owo.config.annotation.*;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

@Config(name = Accessories.MODID, wrapperName = "AccessoriesConfig")
public class AccessoriesConfigModel {

    @RestartRequired
    @Sync(Option.SyncMode.OVERRIDE_CLIENT)
    public boolean useExperimentalCaching = false;

    @Nest
    @Expanded
    public ContentFocusedOptions contentOptions = new ContentFocusedOptions();
    
    public static class ContentFocusedOptions {
        @RestartRequired
        @Sync(Option.SyncMode.OVERRIDE_CLIENT)
        public List<String> validGliderSlots = new ArrayList<>(List.of("cape", "back"));

        public boolean allowGliderEquip = false;

        @RestartRequired
        @Sync(Option.SyncMode.OVERRIDE_CLIENT)
        public List<String> validTotemSlots = new ArrayList<>(List.of("charm"));
        
        public boolean allowTotemEquip = false;

        @RestartRequired
        @Sync(Option.SyncMode.OVERRIDE_CLIENT)
        public List<String> validBannerSlots = new ArrayList<>(List.of("hat", "cape"));

        public boolean allowBannerEquip = true;
    }
    
    @Nest
    @Expanded
    public GeneralClientOptions clientOptions = new GeneralClientOptions();

    public static class GeneralClientOptions {

        @Hook
        public PlayerEquipControl equipControl = PlayerEquipControl.MUST_NOT_CROUCH;

        public boolean forceNullRenderReplacement = false;

        public boolean disableEmptySlotScreenError = false;

        public boolean showCosmeticAccessories = true;

        public List<RenderSlotTarget> disabledDefaultRenders = new ArrayList<>();
    }

    @Nest
    @Expanded
    public ScreenOptions screenOptions = new ScreenOptions();

    public static class ScreenOptions {

        public boolean keybindIgnoresOtherTargets = false;

        public boolean backButtonClosesScreen = false;

        public ScreenType selectedScreenType = ScreenType.NONE;

        @Hook
        public boolean showUnusedSlots = false;

        public boolean allowSlotScrolling = true;

        // Screen Injected Button offsets

        @SectionHeader("button_offsets")
        @Hook
        public List<MenuButtonInjection> menuButtonInjections = new ArrayList<>(
                List.of(
                        new MenuButtonInjection(ResourceLocation.withDefaultNamespace("creative_player_inventory"), 96, 6),
                        new MenuButtonInjection(ResourceLocation.withDefaultNamespace("player_inventory"), 66, 8),
                        new MenuButtonInjection(ResourceLocation.withDefaultNamespace("horse_inventory"), 69, 18)
                )
        );

        // Experimental Screen

        @SectionHeader("experimental")
        public boolean isDarkMode = false;
        public boolean showEquippedStackSlotType = true;

        public boolean entityLooksAtMouseCursor = false;

        public boolean allowSideBarCraftingGrid = true;

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
