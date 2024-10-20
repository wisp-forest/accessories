package io.wispforest.accessories.impl.caching;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.wispforest.accessories.api.EquipmentChecking;
import io.wispforest.accessories.api.caching.ItemStackBasedPredicate;
import io.wispforest.accessories.api.slot.SlotEntryReference;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public abstract class EquipmentLookupCache {

    protected final Cache<ItemStackBasedPredicate, @Nullable Boolean> isEquipped;

    protected final Cache<ItemStackBasedPredicate, @Nullable Optional<SlotEntryReference>> firstEquipped_ACCESSORIES_ONLY;
    protected final Cache<ItemStackBasedPredicate, @Nullable Optional<SlotEntryReference>> firstEquipped_COSMETICALLY_OVERRIDABLE;

    protected final Cache<ItemStackBasedPredicate, @Nullable List<SlotEntryReference>> equipped;

    @Nullable
    protected List<SlotEntryReference> getAllEquipped = null;

    protected EquipmentLookupCache() {
        isEquipped = CacheBuilder.newBuilder()
                .concurrencyLevel(1)
                .expireAfterAccess(Duration.ofSeconds(60))
                .build();

        firstEquipped_ACCESSORIES_ONLY = CacheBuilder.newBuilder()
                .concurrencyLevel(1)
                .expireAfterAccess(Duration.ofSeconds(60))
                .build();

        firstEquipped_COSMETICALLY_OVERRIDABLE = CacheBuilder.newBuilder()
                .concurrencyLevel(1)
                .expireAfterAccess(Duration.ofSeconds(60))
                .build();

        equipped = CacheBuilder.newBuilder()
                .concurrencyLevel(1)
                .expireAfterAccess(Duration.ofSeconds(60))
                .build();
    }

    public abstract boolean isEquipped(ItemStackBasedPredicate predicate);

    @Nullable
    public abstract SlotEntryReference firstEquipped(ItemStackBasedPredicate predicate, EquipmentChecking check);

    @Nullable
    public abstract List<SlotEntryReference> getEquipped(ItemStackBasedPredicate predicate);

    public abstract List<SlotEntryReference> getAllEquipped();

    public void clearCache() {
        this.isEquipped.invalidateAll();

        this.firstEquipped_ACCESSORIES_ONLY.invalidateAll();
        this.firstEquipped_COSMETICALLY_OVERRIDABLE.invalidateAll();

        this.equipped.invalidateAll();

        this.getAllEquipped = null;
    }
}
