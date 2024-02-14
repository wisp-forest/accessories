package io.wispforest.accessories.data;

import net.minecraft.server.packs.resources.PreparableReloadListener;

import java.util.function.Consumer;

public interface DataLoadingModifications {
    void beforeRegistration(Consumer<PreparableReloadListener> registrationMethod);

    @interface DataLoadingModificationsCapable{}
}
