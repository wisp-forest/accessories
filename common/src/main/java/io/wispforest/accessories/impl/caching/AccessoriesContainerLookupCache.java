package io.wispforest.accessories.impl.caching;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.api.caching.ItemStackBasedPredicate;
import io.wispforest.accessories.api.equip.EquipmentChecking;
import io.wispforest.accessories.api.slot.SlotEntryReference;
import io.wispforest.accessories.api.core.AccessoryNestUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AccessoriesContainerLookupCache extends EquipmentLookupCache {

    private final AccessoriesContainer container;

    public AccessoriesContainerLookupCache(AccessoriesContainer container) {
        this.container = container;
    }

    public boolean isEquipped(ItemStackBasedPredicate predicate) {
        if(isEmpty) this.isEmpty = false;

        var value = isEquipped.getIfPresent(predicate);

        if (value == null) {
            value = getAllEquipped().stream().anyMatch(slotEntryReference -> predicate.test(slotEntryReference.stack()));

            this.isEquipped.put(predicate, value);
        }

        return value;
    }

    @Nullable
    public SlotEntryReference firstEquipped(ItemStackBasedPredicate predicate, EquipmentChecking check) {
        if(isEmpty) this.isEmpty = false;

        var cache = (check == EquipmentChecking.ACCESSORIES_ONLY ? firstEquipped_ACCESSORIES_ONLY : firstEquipped_COSMETICALLY_OVERRIDABLE);

        @Nullable var value = cache.getIfPresent(predicate);

        if (value == null) {
            value = this.container.getAccessories().foreach((i, stack) -> {
                var reference = this.container.createReference(i);

                if(check == EquipmentChecking.COSMETICALLY_OVERRIDABLE) {
                    var cosmetic = this.container.getCosmeticAccessories().getItem(reference.index());

                    if(!cosmetic.isEmpty() && Accessories.config().clientOptions.showCosmeticAccessories()) stack = cosmetic;
                }

                var entryReference = AccessoryNestUtils.recursivelyHandle(stack, reference, (innerStack, ref) -> {
                    return (!innerStack.isEmpty() && predicate.test(innerStack))
                            ? new SlotEntryReference(reference, innerStack)
                            : null;
                });

                return entryReference != null ? Optional.of(entryReference) : null;
            });

            if (value == null) value = Optional.empty();

            cache.put(predicate, value);
        }

        return value.orElse(null);
    }

    @Nullable
    public List<SlotEntryReference> getEquipped(ItemStackBasedPredicate predicate) {
        if(isEmpty) this.isEmpty = false;

        var value = this.equipped.getIfPresent(predicate);

        if (value == null) {
            value = getAllEquipped().stream()
                    .filter(slotEntryReference -> predicate.test(slotEntryReference.stack()))
                    .toList();

            this.equipped.put(predicate, value);
        }

        return value;
    }

    public List<SlotEntryReference> getAllEquipped() {
        if(isEmpty) this.isEmpty = false;

        if(this.getAllEquipped != null) return this.getAllEquipped;

        var currentlyAllEquipped = new ArrayList<SlotEntryReference>();

        this.container.getAccessories().foreach((i, stack) -> {
            if (stack.isEmpty()) return;

            var reference = this.container.createReference(i);

            AccessoryNestUtils.recursivelyConsume(stack, reference, (innerStack, ref) -> currentlyAllEquipped.add(new SlotEntryReference(ref, innerStack)));
        });

        this.getAllEquipped = currentlyAllEquipped;

        return currentlyAllEquipped;
    }

    private boolean isEmpty = true;

    public boolean isEmpty() {
        return this.isEmpty;
    }

    @Override
    public void clearCache() {
        super.clearCache();

        this.isEmpty = true;
    }
}
