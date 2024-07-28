package io.wispforest.accessories;

import com.mojang.logging.LogUtils;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Optional;

public class DataLoaderBase {

    public static final Logger LOGGER = LogUtils.getLogger();

    public static DataLoaderBase INSTANCE = new DataLoaderBase();

    public void registerListeners() {
        // NO-OP
        LOGGER.info("Registering Listeners!");
    }

    protected Optional<PreparableReloadListener> getIdentifiedSlotLoader(){
        return Optional.empty();
    }

    protected Optional<PreparableReloadListener> getIdentifiedEntitySlotLoader(){
        return Optional.empty();
    }
}
