package io.wispforest.accessories.impl.caching;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.api.EquipmentChecking;
import io.wispforest.accessories.api.caching.ItemStackBasedPredicate;
import io.wispforest.accessories.api.slot.SlotEntryReference;
import io.wispforest.accessories.impl.AccessoryNestUtils;
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
        var value = isEquipped.getIfPresent(predicate);

        if (value == null) {
            value = getAllEquipped().stream().anyMatch(slotEntryReference -> predicate.test(slotEntryReference.stack()));

            this.isEquipped.put(predicate, value);
        }

        return value;
    }

    @Nullable
    public SlotEntryReference firstEquipped(ItemStackBasedPredicate predicate, EquipmentChecking check) {
        var cache = (check == EquipmentChecking.ACCESSORIES_ONLY ? firstEquipped_ACCESSORIES_ONLY : firstEquipped_COSMETICALLY_OVERRIDABLE);

        @Nullable var value = cache.getIfPresent(predicate);

        if (value == null) {
            for (var stackEntry : this.container.getAccessories()) {
                var stack = stackEntry.getSecond();
                var reference = this.container.createReference(stackEntry.getFirst());

                if(check == EquipmentChecking.COSMETICALLY_OVERRIDABLE) {
                    var cosmetic = this.container.getCosmeticAccessories().getItem(reference.slot());

                    if(!cosmetic.isEmpty() && Accessories.config().clientOptions.showCosmeticAccessories()) stack = cosmetic;
                }

                var entryReference = AccessoryNestUtils.recursiveStackHandling(stack, reference, (innerStack, ref) -> {
                    return (!innerStack.isEmpty() && predicate.test(innerStack))
                            ? new SlotEntryReference(reference, innerStack)
                            : null;
                });

                if (entryReference != null) {
                    value = Optional.of(entryReference);

                    break;
                }
            }

            if (value == null) value = Optional.empty();

            cache.put(predicate, value);
        }

        return value.orElse(null);
    }

    @Nullable
    public List<SlotEntryReference> getEquipped(ItemStackBasedPredicate predicate) {
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
        if(this.getAllEquipped == null) {
            this.getAllEquipped = new ArrayList<>();

            for (var stackEntry : this.container.getAccessories()) {
                var stack = stackEntry.getSecond();

                if (stack.isEmpty()) continue;

                var reference = this.container.createReference(stackEntry.getFirst());

                AccessoryNestUtils.recursiveStackConsumption(stack, reference, (innerStack, ref) -> this.getAllEquipped.add(new SlotEntryReference(ref, innerStack)));
            }
        }

        return getAllEquipped;
    }
}
