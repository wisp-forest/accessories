package io.wispforest.accessories.fabric;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public class AccessoriesLoaderInternalsImpl {

    public static boolean isDevelopmentEnv() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    public static boolean isModLoaded(String mod) {
        return FabricLoader.getInstance().isModLoaded(mod);
    }

    public static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir();
    }
}
