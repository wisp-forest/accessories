package io.wispforest.accessories.utils;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.wispforest.accessories.endec.NbtMapCarrier;
import io.wispforest.accessories.mixin.StateHolderAccessor;
import io.wispforest.owo.serialization.CodecUtils;
import io.wispforest.owo.serialization.format.nbt.NbtEndec;
import io.wispforest.endec.*;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.endec.util.MapCarrier;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateHolder;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import org.joml.*;

import java.lang.Math;
import java.util.*;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public class EndecUtils {

    public static final Endec<ListTag> NBT_LIST = NbtEndec.ELEMENT.xmap(ListTag.class::cast, listTag -> listTag);

    public static final Endec<TriState> TRI_STATE_ENDEC = Endec.BOOLEAN.nullableOf().xmap(TriState::of, TriState::getBoxed);

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

    public static void dfuKeysCarrier(MapCarrier carrier, Map<String, String> changedKeys) {
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

    public static <T> StructEndec<MutableObject<T>> wrappedEndec(String fieldName, Endec<T> endec) {
        return StructEndecBuilder.of(endec.fieldOf(fieldName, MutableObject::getValue), MutableObject::new);
    }

    public static <K, V, M extends Map<K, V>> Endec<M> map(IntFunction<M> mapConstructor, Function<K, String> keyToString, Function<String, K> stringToKey, Endec<V> valueEndec) {
        return Endec.of((ctx, serializer, map) -> {
            try (var mapState = serializer.map(ctx, valueEndec, map.size())) {
                map.forEach((k, v) -> mapState.entry(keyToString.apply(k), v));
            }
        }, (ctx, deserializer) -> {
            var mapState = deserializer.map(ctx, valueEndec);

            var map = mapConstructor.apply(mapState.estimatedSize());

            mapState.forEachRemaining(entry -> map.put(stringToKey.apply(entry.getKey()), entry.getValue()));

            return map;

        });
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
}
