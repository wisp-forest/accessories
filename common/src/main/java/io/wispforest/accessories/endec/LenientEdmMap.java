package io.wispforest.accessories.endec;

import io.wispforest.endec.Endec;
import io.wispforest.endec.SerializationAttributes;
import io.wispforest.endec.SerializationContext;
import io.wispforest.endec.format.edm.*;
import io.wispforest.endec.impl.KeyedEndec;
import io.wispforest.endec.util.MapCarrier;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public final class LenientEdmMap implements MapCarrier {

    public static final Endec<LenientEdmMap> ENDEC = EdmEndec.MAP.xmap(LenientEdmMap::of, LenientEdmMap::edmMap);

    private final EdmMap edmMap;
    private final Map<String, EdmElement<?>> mapView;

    public LenientEdmMap(EdmMap edmMap, Map<String, EdmElement<?>> mapView) {
        this.edmMap = edmMap;
        this.mapView = mapView;
    }

    public static LenientEdmMap of(EdmMap edmMap) {
        var mutableView = new HashMap<>(edmMap.value());

        return new LenientEdmMap(EdmElement.consumeMap(mutableView).asMap(), mutableView);
    }

    @Override
    public <T> T getWithErrors(SerializationContext ctx, @NotNull KeyedEndec<T> key) {
        if (!this.has(key)) return key.defaultValue();
        return key.endec().decodeFully(ctx.withAttributes(SerializationAttributes.HUMAN_READABLE), LenientEdmDeserializer::of, this.mapView.get(key.key()));
    }

    @Override
    public <T> void put(SerializationContext ctx, @NotNull KeyedEndec<T> key, @NotNull T value) {
        this.mapView.put(key.key(), key.endec().encodeFully(ctx.withAttributes(SerializationAttributes.HUMAN_READABLE), EdmSerializer::of, value));
    }

    @Override
    public <T> void delete(@NotNull KeyedEndec<T> key) {
        this.mapView.remove(key.key());
    }

    @Override
    public <T> boolean has(@NotNull KeyedEndec<T> key) {
        return this.mapView.containsKey(key.key());
    }

    public EdmMap edmMap() {
        return edmMap;
    }
}
