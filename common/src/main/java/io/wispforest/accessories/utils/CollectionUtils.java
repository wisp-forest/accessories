package io.wispforest.accessories.utils;

import it.unimi.dsi.fastutil.Hash;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CollectionUtils {
    public static <K, V> Collector<Map.Entry<K, V>, ?, LinkedHashMap<K, V>> linkedMapCollector() {
        return Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (obj1, obj2) -> obj1, LinkedHashMap::new);
    }

    public static <T, K> Collector<T, ?, LinkedHashMap<K, T>> linkedMapKeyCollector(Function<? super T, ? extends K> keyMapper) {
        return Collectors.toMap(keyMapper, Function.identity(), (obj1, obj2) -> obj1, LinkedHashMap::new);
    }

    public static <T, V> Collector<T, ?, LinkedHashMap<T, V>> linkedMapValueCollector(Function<? super T, ? extends V> valueMapper) {
        return Collectors.toMap(Function.identity(), valueMapper, (obj1, obj2) -> obj1, LinkedHashMap::new);
    }

    private static final Hash.Strategy<Object> IDENTITY_STRATEGY = new Hash.Strategy<Object>() {
        @Override
        public int hashCode(Object o) {
            return System.identityHashCode(o);
        }

        @Override
        public boolean equals(Object a, Object b) {
            return a == b;
        }
    };

    public static <T> Hash.Strategy<T> identityHash() {
        return (Hash.Strategy<T>) IDENTITY_STRATEGY;
    }

    public static <T, R> Stream<R> mapWithIndex(Stream<T> stream, IndexMapper<T, R> mapper) {
        var index = new MutableInt(0);

        if (stream.isParallel()) {
            throw new IllegalStateException("Unable to mapWithIndex on parallel map due to the unordered nature of operation!");
        }

        return stream.map(t -> mapper.map(index.getAndAdd(1), t));
    }

    public static <T, R> Stream<R> flatMapWithIndex(Stream<T> stream, IndexMapper<T, Stream<R>> mapper) {
        var index = new MutableInt(0);

        if (stream.isParallel()) {
            throw new IllegalStateException("Unable to mapWithIndex on parallel map due to the unordered nature of operation!");
        }

        return stream.flatMap(t -> mapper.map(index.getAndAdd(1), t));
    }


    public interface IndexMapper<T, R> {
        R map(int index, T t);
    }
}
