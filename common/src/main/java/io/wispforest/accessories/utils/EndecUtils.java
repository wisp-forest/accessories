package io.wispforest.accessories.utils;

import com.google.common.base.Suppliers;
import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import io.wispforest.accessories.endec.NbtMapCarrier;
import io.wispforest.accessories.mixin.StateHolderAccessor;
import io.wispforest.accessories.mixin.owo.TagValueInputAccessor;
import io.wispforest.accessories.mixin.owo.TagValueOutputAccessor;
import io.wispforest.endec.*;
import io.wispforest.endec.format.gson.GsonMapCarrier;
import io.wispforest.endec.impl.KeyedEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.endec.impl.StructField;
import io.wispforest.endec.util.MapCarrierDecodable;
import io.wispforest.endec.util.MapCarrierEncodable;
import io.wispforest.owo.mixin.serialization.ForwardingDynamicOpsAccessor;
import io.wispforest.owo.mixin.serialization.RegistryOpsAccessor;
import io.wispforest.owo.serialization.CodecUtils;
import io.wispforest.owo.serialization.RegistriesAttribute;
import io.wispforest.owo.serialization.format.ContextHolder;
import io.wispforest.owo.serialization.format.nbt.NbtEndec;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.EndTag;
import net.minecraft.resources.DelegatingOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.*;

import java.lang.Math;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class EndecUtils {

    public static final Endec<Vector2i> VECTOR_2_I_ENDEC = StructEndecBuilder.of(
            Endec.LONG.fieldOf("x", vec2i -> (long) vec2i.x),
            Endec.LONG.fieldOf("y", vec2i -> (long) vec2i.y),
            (x, y) -> new Vector2i((int) (long) x, (int) (long) y)
    );

    public static final StructEndec<Vector3f> VECTOR_3_F_ENDEC = EndecUtils.vectorEndec("Vector3f", Endec.FLOAT, Vector3f::new, Vector3f::x, Vector3f::y, Vector3f::z);

    public static final StructEndec<Quaternionf> QUATERNIONF_COMPONENTS = EndecUtils.vectorEndec("QuaternionfComponents", Endec.FLOAT, Quaternionf::new, Quaternionf::x, Quaternionf::y, Quaternionf::z, Quaternionf::w);

    public static final StructEndec<AxisAngle4f> AXISANGLE4F = StructEndecBuilder.of(
            Endec.FLOAT.xmap(degrees -> (float) Math.toRadians(degrees), (radians) -> (float) Math.toDegrees(radians)).fieldOf("angle", axisAngle4f -> axisAngle4f.angle),
            VECTOR_3_F_ENDEC.fieldOf("axis", axisAngle4f -> new Vector3f(axisAngle4f.x, axisAngle4f.y, axisAngle4f.z)),
            AxisAngle4f::new
    );

    public static final Endec<Matrix4f> MATRIX4F = Endec.FLOAT.listOf()
            .validate(floats -> {
                if (floats.size() != 16) throw new IllegalStateException("Matrix entries must have 16 elements");
            }).xmap(floats -> {
                var matrix4f = new Matrix4f();

                for (int i = 0; i < floats.size(); i++) {
                    matrix4f.setRowColumn(i >> 2, i & 3, floats.get(i));
                }

                return matrix4f.determineProperties();
            }, matrix4f -> {
                var floats = new FloatArrayList(16);

                for (int i = 0; i < 16; i++) {
                    floats.add(matrix4f.getRowColumn(i >> 2, i & 3));
                }

                return floats;
            });

    public static StructEndec<BlockState> blockStateEndec(String typeKey) {
        return CodecUtils.toStructEndec(
                ((MapCodec.MapCodecCodec<BlockState>) BuiltInRegistries.BLOCK.byNameCodec().dispatch(
                        "id",
                        stateHolder -> ((StateHolderAccessor<Block, BlockState>) stateHolder).accessories$owner(),
                        block -> {
                            BlockState stateHolder = block.defaultBlockState();

                            if (stateHolder.getValues().isEmpty()) return MapCodec.unit(stateHolder);

                            return (((StateHolderAccessor<Block, BlockState>) stateHolder).accessories$propertiesCodec())
                                    .codec()
                                    .lenientOptionalFieldOf("properties")
                                    .xmap(optional -> optional.orElse(stateHolder), Optional::of);
                        }
                )).codec()
        );
    }

    public static <C, V> StructEndec<V> vectorEndec(String name, Endec<C> componentEndec, StructEndecBuilder.Function3<C, C, C, V> constructor, Function<V, C> xGetter, Function<V, C> yGetter, Function<V, C> zGetter) {
        return vectorEndec(name, componentEndec, constructor, xGetter, yGetter, zGetter, null);
    }

    public static <C, V> StructEndec<V> vectorEndec(String name, Endec<C> componentEndec, StructEndecBuilder.Function3<C, C, C, V> constructor, Function<V, C> xGetter, Function<V, C> yGetter, Function<V, C> zGetter, @Nullable C defaultValue) {
        var networkEndec = structifyEndec(componentEndec.listOf().validate(ints -> {
            if (ints.size() != 3) throw new IllegalStateException(name + " array must have three elements");
        }).xmap(
                components -> constructor.apply(components.get(0), components.get(1), components.get(2)),
                vector -> List.of(xGetter.apply(vector), yGetter.apply(vector), zGetter.apply(vector))
        ));

        var baseEndec = StructEndecBuilder.of(
                (defaultValue != null) ? componentEndec.optionalFieldOf("x", xGetter, defaultValue) : componentEndec.fieldOf("x", xGetter),
                (defaultValue != null) ? componentEndec.optionalFieldOf("y", xGetter, defaultValue) : componentEndec.fieldOf("y", yGetter),
                (defaultValue != null) ? componentEndec.optionalFieldOf("z", xGetter, defaultValue) : componentEndec.fieldOf("z", zGetter),
                constructor
        );

        return new AttributeStructEndecBuilder<>(baseEndec, SerializationAttributes.HUMAN_READABLE).orElse(networkEndec);
    }

    public static <C, V> StructEndec<V> vectorEndec(String name, Endec<C> componentEndec, StructEndecBuilder.Function4<C, C, C, C, V> constructor, Function<V, C> xGetter, Function<V, C> yGetter, Function<V, C> zGetter, Function<V, C> wGetter) {
        var networkEndec = structifyEndec(componentEndec.listOf().validate(ints -> {
            if (ints.size() != 4) throw new IllegalStateException(name + " array must have four elements");
        }).xmap(
                components -> constructor.apply(components.get(0), components.get(1), components.get(2), components.get(3)),
                vector -> List.of(xGetter.apply(vector), yGetter.apply(vector), zGetter.apply(vector), wGetter.apply(vector))
        ));

        var baseEndec = StructEndecBuilder.of(
                componentEndec.fieldOf("x", xGetter),
                componentEndec.fieldOf("y", yGetter),
                componentEndec.fieldOf("z", zGetter),
                componentEndec.fieldOf("w", wGetter),
                constructor
        );

        return new AttributeStructEndecBuilder<>(baseEndec, SerializationAttributes.HUMAN_READABLE).orElse(networkEndec);
    }

    public static void dfuKeysCarrier(MapCarrierDecodable carrier, Map<String, String> changedKeys) {
        CompoundTag compoundTag;

        if (carrier instanceof NbtMapCarrier nbtMapCarrier) {
            compoundTag = nbtMapCarrier.compoundTag();
        } else if (carrier instanceof CompoundTag carrierTag) {
            compoundTag = carrierTag;
        } else {
            compoundTag = null;
        }

        if(compoundTag != null) {
            changedKeys.forEach((prevKey, newKey) -> {
                if (compoundTag.contains(prevKey)) compoundTag.put(newKey, compoundTag.get(prevKey));
            });
        }
    }

    public static <E extends Enum<E> & StringRepresentable> Endec<E> forEnumStringRepresentable(Class<E> enumClass) {
        return Endec.ifAttr(
                SerializationAttributes.HUMAN_READABLE,
                Endec.STRING.xmap(name -> Arrays.stream(enumClass.getEnumConstants()).filter(e -> e.getSerializedName().equals(name)).findFirst().get(), StringRepresentable::getSerializedName)
        ).orElse(
                Endec.VAR_INT.xmap(ordinal -> enumClass.getEnumConstants()[ordinal], Enum::ordinal)
        );
    }

    public static <T> StructEndec<T> structifyEndec(Endec<T> endec) {
        return structifyEndec("v", endec);
    }

    public static <T> StructEndec<T> structifyEndec(String fieldName, Endec<T> endec) {
        return wrappedEndec(fieldName, endec).xmap(MutableObject::getValue, MutableObject::new);
    }

    public static <K, V> StructEndec<Pair<K, V>> ofPair(String keyName, String valueName, Endec<K> keyEndec, Endec<V> valueEndec) {
        return StructEndecBuilder.of(
                keyEndec.fieldOf(keyName, Pair::left),
                valueEndec.fieldOf(valueName, Pair::second),
                Pair::of
        );
    }

    public static <T> StructEndec<MutableObject<T>> wrappedEndec(String fieldName, Endec<T> endec) {
        return StructEndecBuilder.of(endec.fieldOf(fieldName, MutableObject::getValue), MutableObject::new);
    }

    public static <C extends Collection<T>, T> Endec<C> collectionOf(Endec<T> endec, Supplier<C> supplier) {
        return endec.listOf().xmap(ts -> {
            var collection = supplier.get();

            collection.addAll(ts);

            return collection;
        }, ArrayList::new);
    }

    public static <E extends Enum<E>> Endec<E> forEnum(Class<E> enumClass) {
        return Endec.ifAttr(
                SerializationAttributes.HUMAN_READABLE,
                Endec.STRING.xmap(name -> {
                    return Arrays.stream(enumClass.getEnumConstants())
                            .filter(e -> e.name().toLowerCase(Locale.ROOT).equals(name.toLowerCase(Locale.ROOT)))
                            .findFirst()
                            .orElseThrow();
                }, Enum::name)
        ).orElse(
                Endec.VAR_INT.xmap(ordinal -> enumClass.getEnumConstants()[ordinal], Enum::ordinal)
        );
    }

    public static final class LazyStructEndec<T> implements StructEndec<T> {
        private final Supplier<StructEndec<T>> supplier;

        public LazyStructEndec(Supplier<StructEndec<T>> supplier) {
            this.supplier = Suppliers.memoize(supplier::get);
        }

        @Override
        public void encodeStruct(SerializationContext ctx, Serializer<?> serializer, Serializer.Struct struct, T value) {
            supplier.get().encodeStruct(ctx, serializer, struct, value);
        }

        @Override
        public T decodeStruct(SerializationContext ctx, Deserializer<?> deserializer, Deserializer.Struct struct) {
            return supplier.get().decodeStruct(ctx, deserializer, struct);
        }


        @Override
        public String toString() {
            return "LazyStructEndec[" +
                    "supplier=" + supplier + ']';
        }
    }

    public static <S, T> StructField<S, T> optionalFieldOf(Endec<T> endec, String name, Function<S, T> getter, Supplier<@Nullable T> defaultValue, Predicate<T> isEmpty) {
        return new StructField<>(name, endec.optionalOf().xmap(optional -> optional.orElseGet(defaultValue), t -> {
            if (t == null || isEmpty.test(t)) return Optional.empty();

            return Optional.of(t);
        }), getter, defaultValue);
    }

    public static <T extends InstanceEndec> Endec<T> createMapCarrierEndec(Supplier<T> supplier) {
        return NbtEndec.COMPOUND.xmapWithContext(
            (ctx, compound) -> Util.make(supplier.get(), t -> t.decode(new NbtMapCarrier(compound), ctx)),
            (ctx, t) -> Util.make(NbtMapCarrier.of(), map -> t.encode(map, ctx)).compoundTag());
    }

    public static <T extends InstanceEndec> void readDataFrom(T to, T from) {
        var carrier = new GsonMapCarrier(new JsonObject());

        from.encode(carrier, SerializationContext.empty());
        to.decode(carrier, SerializationContext.empty());
    }

    public static MapCarrierDecodable createCarrierDecoder(ValueInput input) {
        if (input instanceof MapCarrierDecodable decodable) return decodable;

        if (input instanceof TagValueInputAccessor tagInputAccessor) {
            var assumedContext = createContext(tagInputAccessor.accessories$context().ops(), SerializationContext.empty());

            return new MapCarrierDecodable() {
                @Override
                public <T> T getWithErrors(SerializationContext ctx, @NotNull KeyedEndec<T> key) {
                    ctx = ctx.and(assumedContext);

                    return tagInputAccessor.accessories$input()
                        .get(ctx, key);
                }

                @Override
                public <T> T get(SerializationContext ctx, @NotNull KeyedEndec<T> key) {
                    try {
                        return this.getWithErrors(ctx, key);
                    } catch (Exception e) {
                        var tag = tagInputAccessor.accessories$input()
                            .get(key.key());

                        if (tag == null) tag = EndTag.INSTANCE;

                        tagInputAccessor.accessories$problemReporter()
                            .report(new TagValueInput.DecodeFromFieldFailedProblem(key.key(), tag, (DataResult.Error<?>) DataResult.error(e::getMessage)));

                        return key.defaultValue();
                    }
                }

                @Override
                public <T> boolean has(@NotNull KeyedEndec<T> key) {
                    return tagInputAccessor.accessories$input().has(key);
                }
            };
        }

        return new MapCarrierDecodable() {
            @Override
            public <T> T getWithErrors(SerializationContext ctx, @NotNull KeyedEndec<T> key) {
                return input.read(key.key(), CodecUtils.toCodec(key.endec(), ctx))
                    .orElseGet(key::defaultValue);
            }

            @Override
            public <T> boolean has(@NotNull KeyedEndec<T> key) {
                return input.child(key.key()).isPresent();
            }
        };
    }

    public static MapCarrierEncodable createCarrierEncoder(ValueOutput output) {
        if (output instanceof MapCarrierEncodable encodable) return encodable;

        if (output instanceof TagValueOutputAccessor tagOutputAccessor) {
            var assumedContext = createContext(tagOutputAccessor.accessories$ops(), SerializationContext.empty());

            return new MapCarrierEncodable() {
                @Override
                public <T> void put(SerializationContext ctx, @NotNull KeyedEndec<T> key, @NotNull T value) {
                    try {
                        ctx = ctx.and(assumedContext);

                        tagOutputAccessor.accessories$output()
                            .put(ctx, key, value);
                    } catch (Exception e) {
                        tagOutputAccessor.accessories$problemReporter()
                            .report(new TagValueOutput.EncodeToFieldFailedProblem(key.key(), value, (DataResult.Error<?>) DataResult.error(e::getMessage)));
                    }
                }

                @Override
                public <T> void delete(@NotNull KeyedEndec<T> key) {
                    tagOutputAccessor.accessories$output()
                        .delete(key);
                }
            };
        }

        return new MapCarrierEncodable() {
            @Override
            public <T> void put(SerializationContext ctx, @NotNull KeyedEndec<T> key, @NotNull T value) {
                output.store(key.key(), CodecUtils.toCodec(key.endec(), ctx), value);
            }

            @Override
            public <T> void delete(@NotNull KeyedEndec<T> key) {
                output.discard(key.key());
            }
        };
    }

    //--

    private static SerializationContext createContext(DynamicOps<?> ops, SerializationContext assumedContext) {
        var rootOps = ops;
        var context = rootOps instanceof ContextHolder holder
            ? holder.capturedContext().and(assumedContext)
            : null;

        while (rootOps instanceof DelegatingOps<?>) {
            rootOps = ((ForwardingDynamicOpsAccessor<?>) rootOps).owo$delegate();

            if (context == null && rootOps instanceof ContextHolder holder) {
                context = holder.capturedContext().and(assumedContext);
            }
        }

        if (context == null) context = assumedContext;

        if (ops instanceof RegistryOps<?> registryOps) {
            context = context.withAttributes(RegistriesAttribute.tryFromCachedInfoGetter(((RegistryOpsAccessor) registryOps).owo$infoGetter()));
        }

        return context;
    }
}
