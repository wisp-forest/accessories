package io.wispforest.accessories;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class DataLoaderBase {

    public static final Logger LOGGER = LogUtils.getLogger();

    public static DataLoaderBase INSTANCE = new DataLoaderBase();

    public void registerListeners() {
        // NO-OP
        LOGGER.info("Registering Listeners!");
    }
}
