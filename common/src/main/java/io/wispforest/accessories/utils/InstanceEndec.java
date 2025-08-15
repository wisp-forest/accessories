package io.wispforest.accessories.utils;

import io.wispforest.endec.SerializationContext;
import io.wispforest.endec.util.MapCarrier;

public interface InstanceEndec {
    void encode(MapCarrier encoder, SerializationContext ctx);

    void decode(MapCarrier decoder, SerializationContext ctx);
}
