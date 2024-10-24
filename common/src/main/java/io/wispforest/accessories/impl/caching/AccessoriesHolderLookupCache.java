package io.wispforest.accessories.impl.caching;

import com.mojang.logging.LogUtils;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.api.EquipmentChecking;
import io.wispforest.accessories.api.caching.ItemStackBasedPredicate;
import io.wispforest.accessories.api.slot.SlotEntryReference;
import io.wispforest.accessories.impl.AccessoriesHolderImpl;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;

public class AccessoriesHolderLookupCache extends EquipmentLookupCache {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final AccessoriesHolderImpl holder;

    private final Map<String, AccessoriesContainerLookupCache> containerLookupCacheMap = new HashMap<>();

    public AccessoriesHolderLookupCache(AccessoriesHolderImpl holder) {
        super();

        this.holder = holder;
    }

    @Override
    public boolean isEquipped(ItemStackBasedPredicate predicate) {
        var value = this.isEquipped.getIfPresent(predicate);

        if (value == null) {
            for (var lookupCache : this.containerLookupCacheMap.values()) {
                value = lookupCache.isEquipped(predicate);

                if (value) break;
            }

            if (value == null) value = false;

            this.isEquipped.put(predicate, value);
        }

        return value;
    }

    @Override
    @Nullable
    public SlotEntryReference firstEquipped(ItemStackBasedPredicate predicate, EquipmentChecking check) {
        var cache = (check == EquipmentChecking.ACCESSORIES_ONLY ? firstEquipped_ACCESSORIES_ONLY : firstEquipped_COSMETICALLY_OVERRIDABLE);

        @Nullable var value = cache.getIfPresent(predicate);

        if (value == null) {
            for (var lookupCache : this.containerLookupCacheMap.values()) {
                var cacheResult = lookupCache.firstEquipped(predicate, check);

                if (cacheResult != null) {
                    value = Optional.of(cacheResult);

                    break;
                }
            }

            if (value == null) value = Optional.empty();

            cache.put(predicate, value);
        }

        return value.orElse(null);
    }

    @Override
    public List<SlotEntryReference> getEquipped(ItemStackBasedPredicate predicate) {
        var value = this.equipped.getIfPresent(predicate);

        if (value == null) {
            value = new ArrayList<>();

            for (var lookupCache : this.containerLookupCacheMap.values()) {
                var cacheResult = lookupCache.getEquipped(predicate);

                if (cacheResult != null) value.addAll(cacheResult);
            }

            this.equipped.put(predicate, value);
        }

        return value;
    }

    @Override
    public List<SlotEntryReference> getAllEquipped() {
        if (this.getAllEquipped == null) {
            this.getAllEquipped = new ArrayList<>();

            for (var value : this.containerLookupCacheMap.values()) {
                var x = value.getAllEquipped();
                if (x != null) this.getAllEquipped.addAll(x);
            }
        }

        return this.getAllEquipped;
    }

    @Override
    public void clearCache() {
        super.clearCache();

        for (var key : Set.copyOf(this.containerLookupCacheMap.keySet())) {
            var container = this.holder.getSlotContainers().get(key);

            if (container != null) {
                this.containerLookupCacheMap.get(key).clearCache();
            } else {
                this.containerLookupCacheMap.remove(key);
            }
        }

        for (var entry : this.holder.getSlotContainers().entrySet()) {
            this.containerLookupCacheMap.computeIfAbsent(entry.getKey(), string -> new AccessoriesContainerLookupCache(entry.getValue())).clearCache();
        }

        //LOGGER.warn("ALL Containers was cleared!");
    }

    public void clearContainerCache(String key) {
        if (!this.containerLookupCacheMap.containsKey(key)) throw new IllegalStateException("Unable to clear the cache! [Key: " + key + "]");

        this.containerLookupCacheMap.get(key).clearCache();

        this.getAllEquipped = null;

        super.clearCache();

        //LOGGER.warn("Single Container was cleared! [Container: {}]", key);
    }

    public void invalidateLookupData(String key, ItemStack stack, List<DataComponentType<?>> types) {
        this.clearContainerCache(key);
    }
}
