package io.wispforest.accessories.utils;

import io.wispforest.accessories.endec.NbtMapCarrier;
import io.wispforest.endec.*;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.endec.util.MapCarrier;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.nbt.CompoundTag;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class EndecUtils {

    public static final Endec<TriState> TRI_STATE_ENDEC = Endec.BOOLEAN.nullableOf().xmap(TriState::of, TriState::getBoxed);

    public static final Endec<Vector3f> VECTOR_3_F_ENDEC = EndecUtils.vectorEndec("Vector3f", Endec.FLOAT, Vector3f::new, Vector3f::x, Vector3f::y, Vector3f::z);

    public static final Endec<Quaternionf> QUATERNIONF_COMPONENTS = EndecUtils.vectorEndec("QuaternionfComponents", Endec.FLOAT, Quaternionf::new, Quaternionf::x, Quaternionf::y, Quaternionf::z, Quaternionf::w);

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

    public static <C, V> Endec<V> vectorEndec(String name, Endec<C> componentEndec, StructEndecBuilder.Function3<C, C, C, V> constructor, Function<V, C> xGetter, Function<V, C> yGetter, Function<V, C> zGetter) {
        return componentEndec.listOf().validate(ints -> {
            if (ints.size() != 3) throw new IllegalStateException(name + " array must have three elements");
        }).xmap(
                components -> constructor.apply(components.get(0), components.get(1), components.get(2)),
                vector -> List.of(xGetter.apply(vector), yGetter.apply(vector), zGetter.apply(vector))
        );
    }

    public static <C, V> Endec<V> vectorEndec(String name, Endec<C> componentEndec, StructEndecBuilder.Function4<C, C, C, C, V> constructor, Function<V, C> xGetter, Function<V, C> yGetter, Function<V, C> zGetter, Function<V, C> wGetter) {
        return componentEndec.listOf().validate(ints -> {
            if (ints.size() != 4) throw new IllegalStateException(name + " array must have four elements");
        }).xmap(
                components -> constructor.apply(components.get(0), components.get(1), components.get(2), components.get(3)),
                vector -> List.of(xGetter.apply(vector), yGetter.apply(vector), zGetter.apply(vector), wGetter.apply(vector))
        );
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
}
