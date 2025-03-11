package io.wispforest.accessories;

import dev.architectury.injectables.annotations.ExpectPlatform;

import java.nio.file.Path;

public class AccessoriesLoaderInternals {

    @ExpectPlatform
    public static boolean isDevelopmentEnv() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean isModLoaded(String mod) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static Path getConfigPath() {
        throw new AssertionError();
    }
}
