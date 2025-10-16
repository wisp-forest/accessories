package io.wispforest.accessories.neoforge;

import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

public class AccessoriesLoaderInternalsImpl {

    public static boolean isDevelopmentEnv() {
        return !FMLLoader.getCurrent().isProduction();
    }

    public static boolean isModLoaded(String mod) {
        return FMLLoader.getCurrent().getLoadingModList().getModFileById(mod) != null;
    }

    public static Path getConfigPath() {
        return FMLLoader.getCurrent().getGameDir().resolve(FMLPaths.CONFIGDIR.relative());
    }
}
