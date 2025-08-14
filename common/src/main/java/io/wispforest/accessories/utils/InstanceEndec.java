package io.wispforest.accessories.utils;

import io.wispforest.endec.SerializationContext;
import io.wispforest.endec.util.MapCarrierDecodable;
import io.wispforest.endec.util.MapCarrierEncodable;

public interface InstanceEndec {
    void encode(MapCarrierEncodable encoder, SerializationContext ctx);

    void decode(MapCarrierDecodable decoder, SerializationContext ctx);
}
