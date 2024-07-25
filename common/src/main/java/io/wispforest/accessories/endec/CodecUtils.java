package io.wispforest.accessories.endec;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;
import io.wispforest.accessories.endec.format.edm.EdmOps;
import io.wispforest.accessories.mixin.DelegatingOpsAccessor;
import io.wispforest.accessories.mixin.RegistryOpsAccessor;
import io.wispforest.endec.*;
import io.wispforest.endec.format.bytebuf.ByteBufDeserializer;
import io.wispforest.endec.format.bytebuf.ByteBufSerializer;
import io.wispforest.endec.format.edm.*;
import net.minecraft.Util;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.DelegatingOps;
import net.minecraft.resources.RegistryOps;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CodecUtils {

    /**
     * Create a new endec serializing the same data as {@code codec}
     * <p>
     * This method is implemented by converting all data to be (de-)serialized
     * to the Endec Data Model data format (hereto-forth to be referred to as EDM)
     * which has both an endec ({@link EdmEndec}) and DynamicOps implementation ({@link EdmOps}).
     * Since EDM encodes structure using a self-described format's native structural types,
     * <b>this means that for JSON and NBT, the created endec's serialized representation is identical
     * to that of {@code codec}</b>. In general, for non-self-described formats, the serialized
     * representation is a byte array
     * <p>
     * When decoding, an EDM element is read from the deserializer and then parsed using {@code codec}
     * <p>
     * When encoding, the value is encoded using {@code codec} to an EDM element which is then
     * written into the serializer
     */
    public static <T> Endec<T> ofCodec(Codec<T> codec) {
        return Endec.of(
                (ctx, serializer, value) -> {
                    EdmEndec.INSTANCE.encode(ctx, serializer, Util.getOrThrow(codec.encodeStart(createEdmOps(ctx), value), IllegalStateException::new));
                },
                (ctx, deserializer) -> {
                    return Util.getOrThrow(codec.parse(createEdmOps(ctx), EdmEndec.INSTANCE.decode(ctx, deserializer)), IllegalStateException::new);
                }
        );
    }

    //--

    /**
     * Create a codec serializing the same data as this endec, assuming
     * that the serialized format posses the {@code assumedAttributes}
     * <p>
     * This method is implemented by converting between a given DynamicOps'
     * datatype and EDM (see {@link #ofCodec(Codec)}) and then encoding/decoding
     * from/to an EDM element using the {@link EdmSerializer} and {@link EdmDeserializer}
     * <p>
     * The serialized representation of a codec created through this method is generally
     * identical to that of a codec manually created to describe the same data
     */
    public static <T> Codec<T> ofEndec(Endec<T> endec, SerializationContext assumedContext) {
        return new Codec<>() {
            @Override
            public <D> DataResult<Pair<T, D>> decode(DynamicOps<D> ops, D input) {
                return captureThrows(() -> new Pair<>(endec.decode(createContext(ops, assumedContext), LenientEdmDeserializer.of(ops.convertTo(EdmOps.withoutContext(), input))), input));
            }

            @Override
            @SuppressWarnings("unchecked")
            public <D> DataResult<D> encode(T input, DynamicOps<D> ops, D prefix) {
                return captureThrows(() -> EdmOps.withoutContext().convertTo(ops, endec.encodeFully(createContext(ops, assumedContext), EdmSerializer::of, input)));
            }
        };
    }

    public static <T> Codec<T> ofEndec(Endec<T> endec) {
        return ofEndec(endec, SerializationContext.empty());
    }

    public static <T> MapCodec<T> ofStruct(StructEndec<T> structEndec, SerializationContext assumedContext) {
        return new MapCodec<>() {
            @Override
            public <T1> Stream<T1> keys(DynamicOps<T1> ops) {
                throw new UnsupportedOperationException("MapCodec generated from StructEndec cannot report keys");
            }

            @Override
            public <T1> DataResult<T> decode(DynamicOps<T1> ops, MapLike<T1> input) {
                return captureThrows(() -> {
                    var map = new HashMap<String, EdmElement<?>>();
                    input.entries().forEach(pair -> {
                        map.put(
                                Util.getOrThrow(ops.getStringValue(pair.getFirst()), s -> new IllegalStateException("Unable to parse key: " + s)),
                                ops.convertTo(EdmOps.withoutContext(), pair.getSecond())
                        );
                    });

                    var context = createContext(ops, assumedContext);

                    return structEndec.decode(context, LenientEdmDeserializer.of(EdmElement.wrapMap(map)));
                });
            }

            @Override
            public <T1> RecordBuilder<T1> encode(T input, DynamicOps<T1> ops, RecordBuilder<T1> prefix) {
                try {
                    var context = createContext(ops, assumedContext);

                    var element = structEndec.encodeFully(context, EdmSerializer::of, input).<Map<String, EdmElement<?>>>cast();

                    var result = prefix;
                    for (var entry : element.entrySet()) {
                        result = result.add(entry.getKey(), EdmOps.withoutContext().convertTo(ops, entry.getValue()));
                    }

                    return result;
                } catch (Exception e) {
                    return prefix.withErrorsFrom(DataResult.error(e::getMessage, input));
                }
            }
        };
    }

    public static <T> MapCodec<T> ofStruct(StructEndec<T> structEndec) {
        return ofStruct(structEndec, SerializationContext.empty());
    }

    private static SerializationContext createContext(DynamicOps<?> ops, SerializationContext assumedContext) {
        var rootOps = ops;
        while (rootOps instanceof DelegatingOps<?>) rootOps = ((DelegatingOpsAccessor<?>) rootOps).delegate();

        var context = rootOps instanceof EdmOps edmOps
                ? edmOps.capturedContext().and(assumedContext)
                : assumedContext;

//        if (ops instanceof RegistryOps<?> registryOps) {
//            context = context.withAttributes(RegistriesAttribute.infoGetterOnly(((RegistryOpsAccessor) registryOps).lookupProvider()));
//        }

        return context;
    }

    private static DynamicOps<EdmElement<?>> createEdmOps(SerializationContext ctx) {
        DynamicOps<EdmElement<?>> ops = EdmOps.withContext(ctx);

//        if (ctx.hasAttribute(RegistriesAttribute.REGISTRIES)) {
//            ops = RegistryOps.create(ops, ctx.getAttributeValue(RegistriesAttribute.REGISTRIES).infoGetter());
//        }

        return ops;
    }

    private static <T> DataResult<T> captureThrows(Supplier<T> action) {
        try {
            return DataResult.success(action.get());
        } catch (Exception e) {
            return DataResult.error(e::getMessage);
        }
    }
}
