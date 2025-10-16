package io.wispforest.accessories.data.api;

import com.google.common.base.Objects;
import com.google.common.collect.BiMap;
import com.google.common.collect.ForwardingIterator;
import com.google.common.collect.ForwardingSet;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

///
/// Am implementation of Guavas [BiMap] that requires that it conforms to java's [SequencedMap].
///
public interface SequencedBiMap<K, V> extends BiMap<K, V>, SequencedMap<K, V> {

    ///
    /// Creates an implementation of [SequencedBiMap] that acts as a wrapper around a given
    /// [SequencedMap] instance using a [SequencedMapFactory]. Such means that it uses two maps
    /// and handles adding to both maps.
    ///
    static <K, V> SequencedBiMap<K, V> of(SequencedMapFactory factory) {
        return new SequencedBiMapImpl<>(factory);
    }

    static <K, V> SequencedBiMap<K, V> of(SequencedMapFactory factory, Map<K, V> map) {
        var bimap = SequencedBiMap.<K, V>of(factory);

        bimap.putAll(map);

        return bimap;
    }

    //--

    @Override
    V put(K key, V value);

    @Override
    V forcePut(K key, V value);

    @Override
    void putAll(Map<? extends K, ? extends V> map);

    @Override
    SequencedBiMap<V, K> inverse();

    //--

    V forcePutLast(K k, V v);

    V forcePutFirst(K k, V v);

    //--

    @Override
    SequencedSet<Entry<K, V>> sequencedEntrySet();

    @Override
    SequencedCollection<V> sequencedValues();

    @Override
    SequencedSet<K> sequencedKeySet();

    @Override
    V putLast(K k, V v);

    @Override
    V putFirst(K k, V v);

    @Override
    Entry<K, V> pollLastEntry();

    @Override
    Entry<K, V> pollFirstEntry();

    @Override
    Entry<K, V> lastEntry();

    @Override
    Entry<K, V> firstEntry();

    @Override
    SequencedBiMap<K, V> reversed();

    interface SequencedMapFactory {
        <K, V> SequencedMap<K, V> create(int size);
    }
}

final class SequencedBiMapImpl<K, V> implements SequencedBiMap<K, V> {
    private final SequencedMap<K, V> baseMap;
    private final SequencedMap<V, K> inverseMap;
    private final SequencedBiMapImpl<V, K> inverseView;

    SequencedBiMapImpl(SequencedMap<K, V> baseMap, SequencedMap<V, K> inverseMap, SequencedBiMapImpl<V, K> inverseView) {
        this.baseMap = baseMap;
        this.inverseMap = inverseMap;
        this.inverseView = inverseView;
    }

    SequencedBiMapImpl(SequencedMap<K, V> baseMap, SequencedMap<V, K> inverseMap) {
        this.baseMap = baseMap;
        this.inverseMap = inverseMap;
        this.inverseView = new SequencedBiMapImpl<>(inverseMap, baseMap, this);
    }

    SequencedBiMapImpl(SequencedMapFactory factory) {
        this(factory.create(0), factory.create(0));
    }

    @Override
    public int size() {
        return this.baseMap.size();
    }

    @Override
    public boolean isEmpty() {
        return this.baseMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return this.baseMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return this.inverseMap.containsKey(value);
    }

    @Override
    public V get(Object key) {
        return this.baseMap.get(key);
    }

    //--

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        map.forEach(this::put);
    }

    @Override public V put(K key, V value) { return putInBothMaps(key, value, false, MapPutOperation.PUT); }
    @Override public V putLast(K key, V value) { return putInBothMaps(key, value, false, MapPutOperation.PUT_LAST); }
    @Override public V putFirst(K key, V value) { return putInBothMaps(key, value, false, MapPutOperation.PUT_FIRST); }

    @Override public V forcePut(K key, V value) { return putInBothMaps(key, value, true, MapPutOperation.PUT); }
    @Override public V forcePutLast(K key, V value) { return putInBothMaps(key, value, true, MapPutOperation.PUT_LAST); }
    @Override public V forcePutFirst(K key, V value) { return putInBothMaps(key, value, true, MapPutOperation.PUT_FIRST); }

    private V putInBothMaps(K key, V value, boolean force, MapPutOperation operation) {
        boolean containedKey = containsKey(key);

        if (containedKey && Objects.equal(value, get(key))) return value;

        if (force) {
            inverse().remove(value);
        } else if (containsValue(value)) {
            throw new IllegalArgumentException("value already present: " + value);
        }

        V oldValue = operation.put(this.baseMap, key, value);

        if (containedKey) this.inverseMap.remove(oldValue);

        operation.put(this.inverseMap, value, key);

        return oldValue;
    }

    private interface MapPutOperation {
        MapPutOperation PUT = SequencedMap::put;
        MapPutOperation PUT_LAST = SequencedMap::putLast;
        MapPutOperation PUT_FIRST = SequencedMap::putFirst;

        <K, V> V put(SequencedMap<K, V> map, K k, V v);
    }

    //--

    @Override
    public V remove(Object key) {
        V value = null;

        if (this.baseMap.containsKey(key)) {
            value = this.baseMap.remove(key);

            this.inverseMap.remove(value);
        }

        return value;
    }

    @Override
    public void clear() {
        this.baseMap.clear();
        this.inverseMap.clear();
    }

    @Override
    public @NotNull SequencedSet<K> keySet() {
        return new CustomForwardingSet<>(this, map -> map.baseMap.sequencedKeySet(), key -> {
            if (!containsKey(key)) return false;

            remove(key);

            return true;
        });
    }

    @Override
    public SequencedSet<V> values() {
        return new CustomForwardingSet<>(this, map -> map.inverseView.sequencedKeySet(), key -> {
            var value = get(key);

            if (!containsValue(value)) return false;

            return remove(key, value);
        });
    }

    @Override
    public @NotNull SequencedSet<Entry<K, V>> entrySet() {
        return new CustomForwardingSet<>(this, map -> map.baseMap.sequencedEntrySet(), key -> {
            var value = get(key);

            if (!containsValue(value)) return false;

            return remove(key, value);
        });
    }

    @Override public SequencedBiMap<V, K> inverse() { return inverseView; }

    @Override public SequencedSet<Entry<K, V>> sequencedEntrySet() { return entrySet(); }
    @Override public SequencedCollection<V> sequencedValues() { return values(); }
    @Override public SequencedSet<K> sequencedKeySet() { return keySet(); }

    @Override public Entry<K, V> pollLastEntry() { return wrapEntry(this.baseMap.pollLastEntry()); }
    @Override public Entry<K, V> pollFirstEntry() { return wrapEntry(this.baseMap.pollFirstEntry()); }
    @Override public Entry<K, V> lastEntry() { return wrapEntry(this.baseMap.lastEntry()); }
    @Override public Entry<K, V> firstEntry() { return wrapEntry(this.baseMap.firstEntry()); }

    private Entry<K, V> wrapEntry(Entry<K, V> entry) {
        return new Entry<K, V>() {
            @Override public K getKey() { return entry.getKey(); }
            @Override public V getValue() { return entry.getValue(); }
            @Override public V setValue(V value) { return SequencedBiMapImpl.this.put(this.getKey(), value); }
            @Override public boolean equals(Object obj) { return entry.equals(obj); }
            @Override public int hashCode() { return entry.hashCode(); }
        };
    }

    @Override
    public SequencedBiMap<K, V> reversed() {
        return new SequencedBiMapImpl<>(this.baseMap.reversed(), this.inverseMap.reversed());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Map<?, ?> m) || m.size() != size()) return false;
        return this.baseMap.equals(m);
    }

    @Override
    public int hashCode() {
        return this.baseMap.hashCode();
    }

    @Override
    public String toString() {
        return this.baseMap.toString();
    }
}

class CustomForwardingSet<T, K, V> extends ForwardingSet<T> implements SequencedSet<T> {

    private final SequencedBiMap<K, V> biMap;
    private final Function<SequencedBiMapImpl<K, V>, SequencedSet<T>> delegateGetter;
    private final Function<T, Boolean> removeFunc;

    private final SequencedSet<T> delegate;

    CustomForwardingSet(SequencedBiMapImpl<K, V> biMap, Function<SequencedBiMapImpl<K, V>, SequencedSet<T>> delegateGetter, Function<T, Boolean> removeFunc) {
        this.biMap = biMap;
        this.delegateGetter = delegateGetter;
        this.removeFunc = removeFunc;

        this.delegate = delegateGetter.apply(biMap);
    }

    @Override
    protected SequencedSet<T> delegate() {
        return delegate;
    }

    @Override
    public Iterator<T> iterator() {
        var itr = super.iterator();

        return new ForwardingIterator<T>() {
            private T value;

            @Override protected Iterator<T> delegate() { return itr; }
            @Override public T next() { return this.value = super.next(); }

            @Override
            public void remove() {
                CustomForwardingSet.this.remove(this.value);

                this.value = null;
            }
        };
    }

    //--

    @Override
    public void clear() {
        this.biMap.clear();
    }

    @Override
    public boolean isEmpty() {
        return this.biMap.isEmpty();
    }

    @Override
    public int size() {
        return this.biMap.size();
    }

    @Override
    public boolean remove(Object object) {
        return this.removeFunc.apply((T) object);
    }

    @Override
    public SequencedSet<T> reversed() {
        return new CustomForwardingSet<>((SequencedBiMapImpl<K, V>) this.biMap.reversed(), this.delegateGetter, this.removeFunc);
    }

    //--

    @Override
    public boolean removeAll(Collection<?> collection) {
        return removeIf(collection::contains);
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        return removeIf(Predicate.not(collection::contains));
    }

    @Override
    public boolean removeIf(@NotNull Predicate<? super T> filter) {
        boolean modified = false;

        var it = iterator();

        while (it.hasNext()) {
            if (filter.test(it.next())) {
                it.remove();

                modified = true;
            }
        }

        return modified;
    }

    //--

    @Override
    public boolean add(T element) {
        throw new IllegalStateException("'add' is not support for this set!");
    }

    @Override
    public boolean addAll(Collection<? extends T> collection) {
        throw new IllegalStateException("'addAll' is not support for this set!");
    }

    @Override
    public void addFirst(T t) {
        throw new IllegalStateException("'addFirst' is not support for this set!");
    }

    @Override
    public void addLast(T t) {
        throw new IllegalStateException("'addLast' is not support for this set!");
    }
}