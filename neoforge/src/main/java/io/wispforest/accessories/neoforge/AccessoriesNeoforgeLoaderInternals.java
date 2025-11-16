package io.wispforest.accessories.neoforge;

import io.wispforest.accessories.AccessoriesLoaderInternals;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

public class AccessoriesNeoforgeLoaderInternals extends AccessoriesLoaderInternals {

    public boolean isDevelopmentEnv() {
        return !FMLLoader.getCurrent().isProduction();
    }

    public boolean isModLoaded(String mod) {
        return FMLLoader.getCurrent().getLoadingModList().getModFileById(mod) != null;
    }

    public Path getConfigPath() {
        return FMLLoader.getCurrent().getGameDir().resolve(FMLPaths.CONFIGDIR.relative());
    }
}
