package io.wispforest.accessories.pond;

import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import net.minecraft.util.context.ContextKey;

public interface AccessoriesRenderStateAPImpl extends AccessoriesRenderStateAPI {

    Reference2ObjectMap<ContextKey<?>, Object> contextKeyToContextData();

    @Override
    default <T> T getStateData(ContextKey<T> key) {
        return (T) this.contextKeyToContextData().get(key);
    }

    @Override
    default <T> void setStateData(ContextKey<T> key, T data) {
        var map = this.contextKeyToContextData();

        if (data == null) {
            map.remove(key);
        } else {
            map.put(key, data);
        }
    }

    @Override
    default <T> boolean hasStateData(ContextKey<T> key) {
        return this.contextKeyToContextData().containsKey(key);
    }

    @Override
    default void clearExtraData() {
        this.contextKeyToContextData().clear();
    }
}
