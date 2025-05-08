package io.wispforest.accessories.pond;

import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;

public interface ReplaceableJsonResourceReloadListener {

    void accessories$allowReplacementLoading(boolean value);

    boolean accessories$allowReplacementLoading();

    static void toggleValue(SimpleJsonResourceReloadListener listener) {
        if (listener instanceof ReplaceableJsonResourceReloadListener value) {
            value.accessories$allowReplacementLoading(!value.accessories$allowReplacementLoading());
        }
    }
}
