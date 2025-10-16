package io.wispforest.accessories.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public class ValidatingForwardingMap<K, V> implements Map<K, V> {

    private final Map<K, V> innerMap;

    private final Class<K> clazzK;
    private final Class<V> clazzV;

    private final Predicate<K> keyValidator;
    private final Function<V, K> keyGetter;

    public ValidatingForwardingMap(Map<K, V> innerMap, Class<K> clazzK, Class<V> clazzV, Predicate<K> keyValidator, Function<V, K> keyGetter) {
        this.innerMap = innerMap;
        this.clazzK = clazzK;
        this.clazzV = clazzV;
        this.keyValidator = keyValidator;
        this.keyGetter = keyGetter;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public int size() {
        return keySet().size();
    }

    //--

    @Override
    public boolean containsKey(Object key) {
        return clazzK.isInstance(key) && keyValidator.test((K) key) && innerMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return clazzV.isInstance(value) && keyValidator.test(keyGetter.apply((V) value)) && innerMap.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return (clazzK.isInstance(key) && keyValidator.test((K) key)) ? innerMap.get(key) : null;
    }

    @Override
    public boolean equals(Object o) {
        return hashCode() == o.hashCode();
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    private Set<K> keySet = null;

    @Override
    public @NotNull Set<K> keySet() {
        if (keySet == null) {
            keySet = new WrappingSet<>(innerMap.keySet(), o -> clazzK.isInstance(o) ? (K) o : null, keyValidator);
        }

        return keySet;
    }

    private Collection<V> values = null;

    @Override
    public @NotNull Collection<V> values() {
        if (values == null) {
            values = new WrappingCollection<>(innerMap.values(), o -> clazzV.isInstance(o) ? (V) o : null, v -> keyValidator.test(keyGetter.apply(v)));
        }

        return values;
    }

    private Set<Entry<K, V>> entrySet = null;

    @Override
    public @NotNull Set<Entry<K, V>> entrySet() {
        if (entrySet == null) {
            entrySet = WrappingSet.of(innerMap.entrySet(), clazzK, keyValidator);
        }

        return entrySet;
    }

    //--

    @Override public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) { throwUnsupported("replaceAll"); }
    @Override public @Nullable V putIfAbsent(K key, V value) { return throwUnsupported("putIfAbsent"); }
    @Override public boolean remove(Object key, Object value) { return throwUnsupported("remove"); }
    @Override public boolean replace(K key, V oldValue, V newValue) { return throwUnsupported("replace"); }
    @Override public @Nullable V replace(K key, V value) { return throwUnsupported("replace"); }
    @Override public V computeIfAbsent(K key, @NotNull Function<? super K, ? extends V> mappingFunction) { return throwUnsupported("computeIfAbsent"); }
    @Override public V computeIfPresent(K key, @NotNull BiFunction<? super K, ? super V, ? extends V> remappingFunction) { return throwUnsupported("computeIfPresent"); }
    @Override public V compute(K key, @NotNull BiFunction<? super K, ? super @Nullable V, ? extends V> remappingFunction) { return throwUnsupported("compute"); }
    @Override public V merge(K key, @NotNull V value, @NotNull BiFunction<? super V, ? super V, ? extends V> remappingFunction) { return throwUnsupported("merge");}
    @Override public V put(K key, V value) { return throwUnsupported("put"); }
    @Override public void putAll(Map<? extends K, ? extends V> map) { throwUnsupported("putAll"); }
    @Override public void clear() { throwUnsupported("clear"); }
    @Override public V remove(Object key) { return throwUnsupported("remove"); }

    private static <T> T throwUnsupported(String methodName) {
        throw new UnsupportedOperationException("Unable to use '" + methodName + "' as such is not support as its Immutable!");
    }
}

class WrappingSet<K, S extends Set<K>> extends WrappingCollection<K, S> implements Set<K> {

    WrappingSet(S innerSet, Function<Object, K> unpacker, Predicate<K> keyValidator) {
        super(innerSet, unpacker, keyValidator);
    }

    public static <K, V> Set<Map.Entry<K, V>> of(Set<Map.Entry<K, V>> innerSet, Class<K> classT, Predicate<K> keyValidator) {
        return new WrappingSet<>(innerSet, o -> {
            return (o instanceof Map.Entry<?, ?> entry)
                ? (Map.Entry<K, V>) entry
                : null;
        }, k -> {
            var key = k.getKey();

            return classT.isInstance(key) && keyValidator.test(key);
        });
    }
}

class WrappingCollection<K, C extends Collection<K>> implements Collection<K> {

    private final C innerSet;
    private final Function<Object, K> unpacker;
    private final Predicate<K> keyValidator;

    WrappingCollection(C innerSet, Function<Object, K> unpacker, Predicate<K> keyValidator) {
        this.innerSet = innerSet;
        this.unpacker = unpacker;
        this.keyValidator = keyValidator;
    }

    @Override
    public int size() {
        return (int) innerSet.stream().filter(keyValidator).count();
    }

    @Override
    public boolean isEmpty() {
        return size() != 0;
    }

    @Override
    public boolean contains(Object o) {
        var k = unpacker.apply(o);

        return k != null && keyValidator.test(k) && innerSet.contains(o);
    }

    @Override
    public @NotNull Iterator<K> iterator() {
        return innerSet.stream().filter(keyValidator).iterator();
    }

    @Override
    public @NotNull Object[] toArray() {
        return innerSet.stream().filter(keyValidator).toArray();
    }

    @Override
    public @NotNull <T> T[] toArray(@NotNull T[] a) {
        return innerSet.stream().filter(keyValidator).toArray(value -> (T[]) Array.newInstance(a.getClass().getComponentType(), value));
    }

    @Override public boolean add(K k) { return throwUnsupported("add"); }
    @Override public boolean remove(Object o) { return throwUnsupported("remove"); }
    @Override public boolean containsAll(@NotNull Collection<?> c) { return throwUnsupported("containsAll"); }
    @Override public boolean addAll(@NotNull Collection<? extends K> c) { return throwUnsupported("addAll"); }
    @Override public boolean retainAll(@NotNull Collection<?> c) { return throwUnsupported("retainAll"); }
    @Override public boolean removeAll(@NotNull Collection<?> c) { return throwUnsupported("removeAll"); }
    @Override public void clear() { throwUnsupported("clear"); }

    private static <T> T throwUnsupported(String methodName) {
        throw new UnsupportedOperationException("Unable to use '" + methodName + "' as such is not support as its Immutable!");
    }
}
