package io.wispforest.accessories.neoforge;

import io.wispforest.accessories.AccessoriesAccess;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

public class ExampleExpectPlatformImpl {
    /**
     * This is our actual method to {@link AccessoriesAccess#getConfigDirectory()}.
     */
    public static Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }
}
