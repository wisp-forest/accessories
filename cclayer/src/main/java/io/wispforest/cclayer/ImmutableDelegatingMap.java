package io.wispforest.cclayer;


import io.wispforest.accessories.api.slot.SlotType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.type.ISlotType;
import top.theillusivec4.curios.compat.CuriosWrappingUtils;
import top.theillusivec4.curios.compat.WrappedSlotType;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public final class ImmutableDelegatingMap<K, V, I> implements Map<K, V> {

    public final Class<K> keyClass;
    public final Class<V> valueClass;

    public final Map<K, I> map;

    public final UnaryOperator<K> toKeyNamespace;
    public final UnaryOperator<K> fromKeyNamespace;

    public final Function<I, V> toValueMapFunc;
    public final Function<V, @Nullable I> fromValueMapFunc;


    private ImmutableDelegatingMap(Class<K> keyClass,
                                   Class<V> valueClass,
                                   Map<K, I> map,
                                   UnaryOperator<K> toKeyNamespace,
                                   UnaryOperator<K> fromKeyNamespace,
                                   Function<I, V> toValueMapFunc,
                                   Function<V, @Nullable I> fromValueMapFunc
    ) {
        this.keyClass = keyClass;
        this.valueClass = valueClass;

        this.map = map;

        this.toKeyNamespace = toKeyNamespace;
        this.fromKeyNamespace = fromKeyNamespace;

        this.toValueMapFunc = toValueMapFunc;
        this.fromValueMapFunc = fromValueMapFunc;
    }

    public static <I, V> Map<String, V> of(Class<V> valueClass, Map<String, I> map, UnaryOperator<String> toKeyNamespace, UnaryOperator<String> fromKeyNamespace, Function<I, V> toValueMapFunc, Function<V, @Nullable I> fromValueMapFunc) {
        return new ImmutableDelegatingMap<>(String.class, valueClass,map, toKeyNamespace, fromKeyNamespace, toValueMapFunc, fromValueMapFunc);
    }

    public static Map<String, ISlotType> slotType(Map<String, io.wispforest.accessories.api.slot.SlotType> map) {
        return new ImmutableDelegatingMap<>(String.class, ISlotType.class, map,
                CuriosWrappingUtils::accessoriesToCurios,
                CuriosWrappingUtils::curiosToAccessories,
                WrappedSlotType::new,
                curiosSlot -> curiosSlot instanceof WrappedSlotType wrappedSlotType ? wrappedSlotType.innerSlotType() : null);
    }

    public static Map<String, ResourceLocation> slotIcon(Map<String, io.wispforest.accessories.api.slot.SlotType> map) {
        return new ImmutableDelegatingMap<>(String.class, ResourceLocation.class, map,
                CuriosWrappingUtils::accessoriesToCurios,
                CuriosWrappingUtils::curiosToAccessories,
                SlotType::icon,
                location -> null);
    }

    public static Map<String, Integer> slotBaseSize(Map<String, io.wispforest.accessories.api.slot.SlotType> map) {
        return new ImmutableDelegatingMap<>(String.class, Integer.class, map,
                CuriosWrappingUtils::accessoriesToCurios,
                CuriosWrappingUtils::curiosToAccessories,
                SlotType::amount,
                location -> null);
    }

    @Override
    public int size() {
        return this.map.size();
    }

    @Override
    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        if(!(keyClass.isInstance(key))) return false;

        return this.map.containsKey(this.fromKeyNamespace.apply((K) key));
    }

    @Override
    public boolean containsValue(Object value) {
        if(!(valueClass.isInstance(value))) return false;

        var possibleValue = this.fromValueMapFunc.apply((V) value);

        return possibleValue != null && this.map.containsValue(possibleValue);
    }

    @Override
    @Nullable
    public V get(Object key) {
        if(!(keyClass.isInstance(key))) return null;

        var entry = this.map.get(this.fromKeyNamespace.apply((K) key));

        if(entry == null) return null;

        return this.toValueMapFunc.apply(entry);
    }

    @Override
    public @NotNull Set<K> keySet() {
        return this.map.keySet().stream()
                .map(this.toKeyNamespace)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public @NotNull Collection<V> values() {
        return this.map.values().stream()
                .map(this.toValueMapFunc)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public @NotNull Set<Entry<K, V>> entrySet() {
        return this.map.entrySet().stream()
                .map(kiEntry ->
                        Map.entry(
                                this.toKeyNamespace.apply(kiEntry.getKey()),
                                this.toValueMapFunc.apply(kiEntry.getValue())))
                .collect(Collectors.toUnmodifiableSet());
    }

    //--

    @Override public @Nullable V put(K key, V value) { return null; }
    @Override public V remove(Object key) { return null; }
    @Override public void putAll(@NotNull Map<? extends K, ? extends V> m) {}
    @Override public void clear() {}
}
