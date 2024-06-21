package io.wispforest.accessories.endec;

import io.wispforest.endec.SerializationContext;
import io.wispforest.endec.format.edm.EdmMap;
import io.wispforest.endec.impl.KeyedEndec;
import io.wispforest.endec.util.MapCarrier;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class EdmUtils {
    public static EdmMap newMap() {
        return EdmMap.wrapMap(new HashMap<>()).asMap();
    }

    private static final class LenientEdmMap implements MapCarrier {

        private final EdmMap edmMap;

        public LenientEdmMap(EdmMap edmMap) {
            this.edmMap = edmMap;
        }

        @Override
        public <T> T getWithErrors(SerializationContext ctx, @NotNull KeyedEndec<T> key) {
            return MapCarrier.super.getWithErrors(ctx, key);
        }

        @Override
        public <T> void put(@NotNull KeyedEndec<T> key, @NotNull T value) {
            MapCarrier.super.put(key, value);
        }

        @Override
        public <T> void delete(@NotNull KeyedEndec<T> key) {
            MapCarrier.super.delete(key);
        }

        @Override
        public <T> boolean has(@NotNull KeyedEndec<T> key) {
            return MapCarrier.super.has(key);
        }
    }
}
