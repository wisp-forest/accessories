package io.wispforest.accessories.utils;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.ServiceLoader;

public class ServiceLoaderUtils {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static <T> T load(Class<T> clazz) {
        final T loadedService = ServiceLoader.load(clazz)
            .findFirst()
            .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));

        LOGGER.debug("Loaded {} for service {}", loadedService, clazz);

        return loadedService;
    }
}
