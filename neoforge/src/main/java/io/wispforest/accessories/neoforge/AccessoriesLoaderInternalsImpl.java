package io.wispforest.accessories.neoforge;

import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

public class AccessoriesLoaderInternalsImpl {

    public static boolean isDevelopmentEnv() {
        return !FMLLoader.isProduction();
    }

    public static boolean isModLoaded(String mod) {
        return FMLLoader.getLoadingModList().getModFileById(mod) != null;
    }

    public static Path getConfigPath() {
        return FMLLoader.getGamePath().resolve(FMLPaths.CONFIGDIR.relative());
    }
}
