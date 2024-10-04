package io.wispforest.accessories.fabric;

import net.fabricmc.loader.api.FabricLoader;

public class AccessoriesLoaderInternalsImpl {

    public static boolean isDevelopmentEnv() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    public static boolean isModLoaded(String mod) {
        return FabricLoader.getInstance().isModLoaded(mod);
    }
}
