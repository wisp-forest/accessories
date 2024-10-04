package io.wispforest.accessories;

import dev.architectury.injectables.annotations.ExpectPlatform;

public class AccessoriesLoaderInternals {

    @ExpectPlatform
    public static boolean isDevelopmentEnv() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean isModLoaded(String mod) {
        throw new AssertionError();
    }
}
