package io.wispforest.accessories.fabric;

import io.wispforest.accessories.AccessoriesLoaderInternals;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public class AccessoriesFabricLoaderInternals extends AccessoriesLoaderInternals {

    public boolean isDevelopmentEnv() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    public boolean isModLoaded(String mod) {
        return FabricLoader.getInstance().isModLoaded(mod);
    }

    public Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir();
    }
}
