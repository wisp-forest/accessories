package io.wispforest.accessories.neoforge;

import net.neoforged.fml.loading.FMLLoader;

public class AccessoriesLoaderInternalsImpl {

    public static boolean isDevelopmentEnv() {
        return !FMLLoader.isProduction();
    }

    public static boolean isModLoaded(String mod) {
        return FMLLoader.getLoadingModList().getModFileById(mod) != null;
    }
}
