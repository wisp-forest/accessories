package io.wispforest.accessories.utils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CollectionUtils {
    public static <K, V> Collector<Map.Entry<K, V>, ?, LinkedHashMap<K, V>> toLinkedMap() {
        return Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (obj1, obj2) -> obj1, LinkedHashMap::new);
    }

    public static <T, K> Collector<T, ?, LinkedHashMap<K, T>> toLinkedMap(Function<? super T, ? extends K> keyMapper) {
        return Collectors.toMap(keyMapper, t -> t, (obj1, obj2) -> obj1, LinkedHashMap::new);
    }
}
