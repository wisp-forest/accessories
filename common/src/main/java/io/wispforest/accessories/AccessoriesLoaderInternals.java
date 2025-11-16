package io.wispforest.accessories;

import io.wispforest.accessories.utils.ServiceLoaderUtils;

import java.nio.file.Path;

public abstract class AccessoriesLoaderInternals {

    public static final AccessoriesLoaderInternals INSTANCE = ServiceLoaderUtils.load(AccessoriesLoaderInternals.class);

    public abstract boolean isDevelopmentEnv();

    public abstract boolean isModLoaded(String mod);

    public abstract Path getConfigPath();
}
