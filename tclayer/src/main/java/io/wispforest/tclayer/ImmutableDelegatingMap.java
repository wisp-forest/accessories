package io.wispforest.tclayer;


import dev.emi.trinkets.api.SlotGroup;
import dev.emi.trinkets.api.SlotType;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.compat.*;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.data.SlotTypeLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public final class ImmutableDelegatingMap<K, V, I> implements Map<K, V> {

    public final String debugNaming;

    public final Class<K> keyClass;
    public final Class<V> valueClass;

    public final Map<K, I> map;

    public final UnaryOperator<K> toKeyNamespace;
    public final UnaryOperator<K> fromKeyNamespace;

    public final BiFunction<K, I, V> toValueMapFunc;
    public final Function<V, @Nullable I> fromValueMapFunc;


    private ImmutableDelegatingMap(
            String debugNaming,
            Class<K> keyClass,
            Class<V> valueClass,
            Map<K, I> map,
            UnaryOperator<K> toKeyNamespace,
            UnaryOperator<K> fromKeyNamespace,
            Function<I, V> toValueMapFunc,
            Function<V, @Nullable I> fromValueMapFunc
    ) {
        this(debugNaming, keyClass, valueClass, map, toKeyNamespace, fromKeyNamespace, (K k, I i) -> toValueMapFunc.apply(i), fromValueMapFunc);
    }

    private ImmutableDelegatingMap(
            String debugNaming,
            Class<K> keyClass,
            Class<V> valueClass,
            Map<K, I> map,
            UnaryOperator<K> toKeyNamespace,
            UnaryOperator<K> fromKeyNamespace,
            BiFunction<K, I, V> toValueMapFunc,
            Function<V, @Nullable I> fromValueMapFunc
    ) {
        this.debugNaming = debugNaming;

        this.keyClass = keyClass;
        this.valueClass = valueClass;

        this.map = map;

        this.toKeyNamespace = toKeyNamespace;
        this.fromKeyNamespace = fromKeyNamespace;

        this.toValueMapFunc = toValueMapFunc;
        this.fromValueMapFunc = fromValueMapFunc;
    }

//    public static <I, V> Map<String, V> of(Class<V> valueClass, Map<String, I> map, UnaryOperator<String> toKeyNamespace, UnaryOperator<String> fromKeyNamespace, Function<I, V> toValueMapFunc, Function<V, @Nullable I> fromValueMapFunc) {
//        return new ImmutableDelegatingMap<>(String.class, valueClass,map, toKeyNamespace, fromKeyNamespace, toValueMapFunc, fromValueMapFunc);
//    }

    public static Map<String, SlotType> slotType(Map<String, io.wispforest.accessories.api.slot.SlotType> map, String group) {
        return new ImmutableDelegatingMap<>(
                "slot_types",
                String.class, SlotType.class, map,
                WrappingTrinketsUtils::accessoriesToTrinkets_Slot,
                string -> WrappingTrinketsUtils.trinketsToAccessories_Slot(Optional.empty(), string),
                slotType -> new WrappedSlotType(slotType, group),
                trinketSlot -> trinketSlot instanceof WrappedSlotType wrappedSlotType ? wrappedSlotType.slotType : null);
    }

    public static Map<String, SlotGroup> slotGroups(Map<String, Map<String, io.wispforest.accessories.api.slot.SlotType>> map, boolean isClientSide) {
        return new ImmutableDelegatingMap<>(
                "slot_group",
                String.class, SlotGroup.class, map,
                WrappingTrinketsUtils::accessoriesToTrinkets_Group,
                WrappingTrinketsUtils::trinketsToAccessories_Group,
                (group, slotData) -> new WrappedSlotGroup(group, slotData, isClientSide),
                trinketSlot -> trinketSlot instanceof WrappedSlotGroup wrappedSlotType ? wrappedSlotType.innerSlots() : null);
    }

    public static Map<String, Map<String, TrinketInventory>> trinketComponentView(Map<String, Map<String, io.wispforest.accessories.api.slot.SlotType>> map, WrappedTrinketComponent component, Map<String, AccessoriesContainer> containerMap) {
        return (Map<String, Map<String, TrinketInventory>>) (Map) new ImmutableDelegatingMap<>(
                "grouped_trinket_inventories",
                String.class, Map.class, map,
                WrappingTrinketsUtils::accessoriesToTrinkets_Group,
                WrappingTrinketsUtils::trinketsToAccessories_Group,
                (group, slotData) -> groupedTrinketInventories(group, component, slotData, containerMap),
                trinketSlots -> null);
    }

    public static Map<String, TrinketInventory> groupedTrinketInventories(String group, WrappedTrinketComponent component, Map<String, io.wispforest.accessories.api.slot.SlotType> slotTypeMap, Map<String, AccessoriesContainer> containerMap) {
        return new ImmutableDelegatingMap<>(
                "trinket_inventories",
                String.class, TrinketInventory.class, slotTypeMap,
                WrappingTrinketsUtils::accessoriesToTrinkets_Slot,
                (string) -> WrappingTrinketsUtils.trinketsToAccessories_Slot(Optional.of(group), string),
                (key, type) -> {
                    var container = containerMap.get(type.name());

                    if(container == null) throw new IllegalStateException("Unable to get the required Accessories container to wrap for Trinkets API call: [Slot: " + type.name() + "]");

                    return new WrappedTrinketInventory(component, container, type);
                },
                object -> null
        );
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

        return this.toValueMapFunc.apply((K) key, entry);
    }

    @Override
    public @NotNull Set<K> keySet() {
        return this.map.keySet().stream()
                .map(this.toKeyNamespace)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public @NotNull Collection<V> values() {
        return this.map.entrySet().stream()
                .map(kiEntry -> this.toValueMapFunc.apply(kiEntry.getKey(), kiEntry.getValue()))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public @NotNull Set<Entry<K, V>> entrySet() {
        return this.map.entrySet().stream()
                .map(kiEntry ->
                        Map.entry(
                                this.toKeyNamespace.apply(kiEntry.getKey()),
                                this.toValueMapFunc.apply(kiEntry.getKey(), kiEntry.getValue())))
                .collect(Collectors.toUnmodifiableSet());
    }

    //--

    @Override public @Nullable V put(K key, V value) { return null; }
    @Override public V remove(Object key) { return null; }
    @Override public void putAll(@NotNull Map<? extends K, ? extends V> m) {}
    @Override public void clear() {}
}
