package io.wispforest.tclayer;

import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.compat.WrappedTrinketComponent;
import dev.emi.trinkets.compat.WrappedTrinketInventory;
import dev.emi.trinkets.compat.WrappingTrinketsUtils;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.tclayer.compat.config.SlotIdRedirect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class OuterGroupMap implements Map<String, Map<String, TrinketInventory>>  {

    private final Map<String, Map<String, io.wispforest.accessories.api.slot.SlotType>> groupedAccessorySlots;

    private final WrappedTrinketComponent trinketComponent;
    private final AccessoriesCapability capability;

    private final Consumer<String> errorMessage;

    public OuterGroupMap(Map<String, Map<String, io.wispforest.accessories.api.slot.SlotType>> groupedAccessorySlots,
                         WrappedTrinketComponent trinketComponent,
                         AccessoriesCapability capability,
                         Consumer<String> errorMessage) {
        this.groupedAccessorySlots = groupedAccessorySlots;

        this.trinketComponent = trinketComponent;
        this.capability = capability;

        this.errorMessage = errorMessage;
    }

    @Override
    public Map<String, TrinketInventory> get(Object key) {
        if (!(key instanceof String str)) return null;

        return new InnerSlotMap(str);
    }

    @Override
    public int size() {
        return this.groupedAccessorySlots.size();
    }

    @Override
    public boolean isEmpty() {
        return this.groupedAccessorySlots.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        if (!(key instanceof String str)) return false;

        return this.groupedAccessorySlots.containsKey(WrappingTrinketsUtils.trinketsToAccessories_Group(str));
    }

    @Override
    public boolean containsValue(Object value) {
        return true;
    }

    @Override
    @NotNull
    public Set<String> keySet() {
        return this.groupedAccessorySlots.keySet().stream().map(WrappingTrinketsUtils::accessoriesToTrinkets_Group).collect(Collectors.toSet());
    }

    @Override
    @NotNull
    public Collection<Map<String, TrinketInventory>> values() {
        return this.keySet().stream().<Map<String, TrinketInventory>>map(InnerSlotMap::new).toList();
    }

    @Override
    @NotNull
    public Set<Entry<String, Map<String, TrinketInventory>>> entrySet() {
        return this.keySet().stream()
                .map(string -> Map.entry(string, (Map<String, TrinketInventory>) new InnerSlotMap(string)))
                .collect(Collectors.toSet());
    }

    @Override public @Nullable Map<String, TrinketInventory> put(String key, Map<String, TrinketInventory> value) { return null; }
    @Override public Map<String, TrinketInventory> remove(Object key) { return null; }
    @Override public void putAll(@NotNull Map<? extends String, ? extends Map<String, TrinketInventory>> m) {}
    @Override public void clear() {}

    public class InnerSlotMap implements Map<String, TrinketInventory> {

        private final String currentTrinketsGroup;

        public InnerSlotMap(String currentTrinketsGroup) {
            this.currentTrinketsGroup = currentTrinketsGroup;
        }

        @Nullable
        public Map<String, io.wispforest.accessories.api.slot.SlotType> groupMap() {
            return OuterGroupMap.this.groupedAccessorySlots.get(WrappingTrinketsUtils.trinketsToAccessories_Group(this.currentTrinketsGroup));
        }

        @Override
        public int size() {
            var groupMap = groupMap();

            return groupMap != null ? groupMap.size() : 0;
        }

        @Override
        public boolean isEmpty() {
            var groupMap = groupMap();

            return groupMap == null || groupMap.isEmpty();
        }

        @Override
        public boolean containsKey(Object key) {
            if (!(key instanceof String str)) return false;

            var redirects = SlotIdRedirect.getBiMap(TCLayer.CONFIG.slotIdRedirects());

            var redirect = redirects.get(this.currentTrinketsGroup + "/" + str);

            if (redirect != null && SlotTypeLoader.getSlotType(OuterGroupMap.this.capability.entity(), redirect) != null) {
                return true;
            }

            var groupMap = groupMap();

            return groupMap != null && groupMap.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return true;
        }

        @Override
        public TrinketInventory get(Object key) {
            if (!(key instanceof String str)) return null;

            var redirects = SlotIdRedirect.getBiMap(TCLayer.CONFIG.slotIdRedirects());

            var redirect = redirects.get(this.currentTrinketsGroup + "/" + str);

            if (redirect != null) {
                var redirectSlot = SlotTypeLoader.getSlotType(OuterGroupMap.this.capability.entity(), redirect);

                if (redirectSlot != null) return create(redirectSlot);
            }

            var groupMap = groupMap();

            if (groupMap == null) {
                errorMessage.accept("Unable to locate the given group: [" + this.currentTrinketsGroup + "]");

                return null;
            }

            var slotType = groupMap.get(key);

            if (slotType == null) {
                errorMessage.accept("Unable to locate the given slot type: [" + key + "]");

                return null;
            }

            return create(slotType);
        }

        private TrinketInventory create(SlotType type) {
            var container = OuterGroupMap.this.capability.getContainers().get(type.name());

            if(container == null) throw new IllegalStateException("Unable to get the required Accessories container to wrap for Trinkets API call: [Slot: " + type.name() + "]");

            return new WrappedTrinketInventory(OuterGroupMap.this.trinketComponent, container, type);
        }

        @Override
        @NotNull
        public Set<String> keySet() {
            var groupMap = groupMap();

            if (groupMap == null) return Set.of();

            return groupMap.keySet().stream()
                    .map(WrappingTrinketsUtils::accessoriesToTrinkets_Slot)
                    .collect(Collectors.toSet());
        }

        @Override
        @NotNull
        public Collection<TrinketInventory> values() {
            var groupMap = groupMap();

            if (groupMap == null) return Set.of();

            return groupMap.values().stream().map(this::create).collect(Collectors.toSet());
        }

        @Override
        @NotNull
        public Set<Entry<String, TrinketInventory>> entrySet() {
            var groupMap = groupMap();

            if (groupMap == null) return Set.of();

            return groupMap.values().stream()
                    .map(slotType -> Map.entry(WrappingTrinketsUtils.accessoriesToTrinkets_Slot(slotType.name()), this.create(slotType)))
                    .collect(Collectors.toSet());
        }

        @Override public @Nullable TrinketInventory put(String key, TrinketInventory value) { return null; }
        @Override public TrinketInventory remove(Object key) { return null; }
        @Override public void putAll(@NotNull Map<? extends String, ? extends TrinketInventory> m) {}
        @Override public void clear() {}
    }
}