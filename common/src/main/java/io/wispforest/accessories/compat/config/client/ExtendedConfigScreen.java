package io.wispforest.accessories.compat.config.client;

import io.wispforest.owo.config.ConfigWrapper;
import io.wispforest.owo.config.Option;
import io.wispforest.owo.config.ui.ConfigScreen;
import io.wispforest.owo.config.ui.OptionComponentFactory;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

public final class ExtendedConfigScreen extends ConfigScreen {
    private ExtendedConfigScreen(ConfigWrapper<?> config, @Nullable Screen parent, BiConsumer<ConfigWrapper<?>, FactoryRegister> consumer) {
        super(ConfigScreen.DEFAULT_MODEL_ID, config, parent);

        consumer.accept(config, this.extraFactories::put);
    }

    public static Function<Screen, Screen> buildFunc(ConfigWrapper<?> config, BiConsumer<ConfigWrapper<?>, FactoryRegister> consumer) {
        return screen -> new ExtendedConfigScreen(config, screen, consumer);
    }

    public interface FactoryRegister {
        void registerFactory(Predicate<Option<?>> predicate, OptionComponentFactory<?> factory);
    }
}
