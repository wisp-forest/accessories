package io.wispforest.accessories.utils;

import io.wispforest.endec.*;
import net.fabricmc.fabric.api.util.TriState;

import java.util.function.Supplier;

public class EndecUtils {

    public static final Endec<TriState> TRI_STATE_ENDEC = Endec.BOOLEAN.nullableOf().xmap(TriState::of, TriState::getBoxed);

    public static <T> Endec<T> unit(T t) {
        return unit(() -> t);
    }

    public static <T> Endec<T> unit(Supplier<T> supplier) {
        return new Endec<>() {
            @Override public void encode(SerializationContext ctx, Serializer<?> serializer, T value) {}
            @Override public T decode(SerializationContext ctx, Deserializer<?> deserializer) { return supplier.get(); }
        };
    }

    public static <T> StructEndec<T> structUnit(T t) {
        return structUnit(() -> t);
    }

    public static <T> StructEndec<T> structUnit(Supplier<T> supplier) {
        return new StructEndec<>() {
            @Override public void encodeStruct(SerializationContext ctx, Serializer<?> serializer, Serializer.Struct struct, T value) {}
            @Override public T decodeStruct(SerializationContext ctx, Deserializer<?> deserializer, Deserializer.Struct struct) { return supplier.get(); }
        };
    }
}
