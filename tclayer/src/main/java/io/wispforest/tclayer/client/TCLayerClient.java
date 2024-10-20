package io.wispforest.tclayer.client;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.compat.config.client.ExtendedConfigScreen;
import io.wispforest.accessories.compat.config.client.components.StructListOptionContainer;
import io.wispforest.owo.config.ui.ConfigScreenProviders;
import io.wispforest.owo.config.ui.OptionComponentFactory;
import io.wispforest.owo.util.NumberReflection;
import io.wispforest.owo.util.ReflectionUtils;
import io.wispforest.tclayer.TCLayer;
import net.fabricmc.api.ClientModInitializer;

import java.util.List;

public class TCLayerClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ConfigScreenProviders.register(
                "tclayer",
                ExtendedConfigScreen.buildFunc(
                        TCLayer.CONFIG,
                        (config, factoryRegister) -> {
                            factoryRegister.registerFactory(
                                    option -> {
                                        var field = option.backingField().field();
                                        if (field.getType() != List.class) return false;

                                        var listType = ReflectionUtils.getTypeArgument(field.getGenericType(), 0);
                                        if (listType == null) return false;

                                        return String.class != listType && !NumberReflection.isNumberType(listType);
                                    },
                                    (uiModel, option) -> {
                                        var layout = new StructListOptionContainer<>(uiModel, option);
                                        return new OptionComponentFactory.Result<>(layout, layout);
                                    });
                        }));
    }
}
