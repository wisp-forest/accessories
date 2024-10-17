package io.wispforest.tclayer;

import dev.emi.trinkets.api.TrinketConstants;
import dev.emi.trinkets.api.TrinketsAttributeModifiersComponent;
import io.wispforest.accessories.mixin.owo.ConfigWrapperAccessor;
import io.wispforest.tclayer.compat.config.SlotIdRedirect;
import io.wispforest.tclayer.compat.config.TCLayerConfig;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public class TCLayer implements ModInitializer {

    public final static TCLayerConfig CONFIG = TCLayerConfig.createAndLoad();

    @Override
    public void onInitialize() {
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, ResourceLocation.fromNamespaceAndPath(TrinketConstants.MOD_ID, "attribute_modifiers"), TrinketsAttributeModifiersComponent.TYPE);
    }
}
