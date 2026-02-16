package io.wispforest.accessories.compat.config;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.data.AccessoriesBaseData;
import io.wispforest.accessories.impl.PlayerEquipControl;
import io.wispforest.accessories.impl.option.AccessoriesPlayerOptionsHolder;
import io.wispforest.owo.config.Option;
import io.wispforest.owo.config.annotation.*;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Config(name = Accessories.MODID, wrapperName = "AccessoriesConfig")
public class AccessoriesConfigModel {

    @Nest
    public ContentFocusedOptions contentOptions = new ContentFocusedOptions();
    
    public static class ContentFocusedOptions {
        @Sync(Option.SyncMode.OVERRIDE_CLIENT)
        @Hook
        public List<String> validGliderSlots = new ArrayList<>(List.of(AccessoriesBaseData.CAPE_SLOT, AccessoriesBaseData.BACK_SLOT));

        @Sync(Option.SyncMode.OVERRIDE_CLIENT)
        @Hook
        public boolean allowGliderEquip = false;

        @Sync(Option.SyncMode.OVERRIDE_CLIENT)
        @Hook
        public List<String> validTotemSlots = new ArrayList<>(List.of(AccessoriesBaseData.CHARM_SLOT));

        @Sync(Option.SyncMode.OVERRIDE_CLIENT)
        @Hook
        public boolean allowTotemEquip = false;

        @Sync(Option.SyncMode.OVERRIDE_CLIENT)
        @Hook
        public List<String> validBannerSlots = new ArrayList<>(List.of(AccessoriesBaseData.CAPE_SLOT, AccessoriesBaseData.HAT_SLOT));

        @Sync(Option.SyncMode.OVERRIDE_CLIENT)
        @Hook
        public boolean allowBannerEquip = true;
    }
    
    @Nest
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
    public ScreenOptions screenOptions = new ScreenOptions();

    public static class ScreenOptions {

        public boolean prioritizeCreativeScreen = false;

        @SectionHeader("button_offsets")
        @Hook
        public List<MenuButtonInjection> menuButtonInjections = new ArrayList<>(
            List.of(
                new MenuButtonInjection(ResourceLocation.withDefaultNamespace("creative_player_inventory"), 96, 6),
                new MenuButtonInjection(ResourceLocation.withDefaultNamespace("player_inventory"), 66, 8),
                new MenuButtonInjection(ResourceLocation.withDefaultNamespace("horse_inventory"), 69, 18)
            )
        );

        @SectionHeader("accessories_screen")
        @Nullable
        public AccessoriesPlayerOptionsHolder defaultValues = null;

        public boolean keybindIgnoresOtherTargets = false;

        public boolean backButtonClosesScreen = false;

//        public ScreenType selectedScreenType = ScreenType.NONE;

        @ExcludeFromScreen
        @Hook
        public boolean showUnusedSlots = false;

        public boolean allowSlotScrolling = true;

        @ExcludeFromScreen
        @Hook
        public boolean isDarkMode = false;

        @ExcludeFromScreen
        public boolean showEquippedStackSlotType = true;

        @ExcludeFromScreen
        public boolean entityLooksAtMouseCursor = false;

        @Hook
        public boolean alwaysShowCraftingGrid = false;

        @Hook
        public TooltipInfoType equipCheckTooltipType = TooltipInfoType.BASIC;

        @Hook
        public boolean showSlotDarkeningEffect = true;

        // Screen Injected Button offsets

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
