package dev.emi.trinkets.compat;

import dev.emi.trinkets.api.SlotGroup;
import io.wispforest.accessories.data.SlotGroupLoader;
import io.wispforest.tclayer.ImmutableDelegatingMap;

import java.util.Map;

public class WrappedSlotGroup extends SlotGroup {

    private final io.wispforest.accessories.api.slot.SlotGroup slotGroup;
    private final Map<String, io.wispforest.accessories.api.slot.SlotType> slots;

    public WrappedSlotGroup(String group, Map<String, io.wispforest.accessories.api.slot.SlotType> slots, boolean isClientSide) {
        super(WrappingTrinketsUtils.accessoriesToTrinkets_Group(group), 0, 0, ImmutableDelegatingMap.slotType(slots, group));

        this.slots = slots;
        this.slotGroup = SlotGroupLoader.INSTANCE.getGroup(isClientSide, group);
    }

    public Map<String, io.wispforest.accessories.api.slot.SlotType> innerSlots() {
        return slots;
    }

    @Override
    public int hashCode() {
        return this.slotGroup.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof WrappedSlotGroup wrappedSlotType) {
            obj = wrappedSlotType.slotGroup;
        }

        return this.slotGroup.equals(obj);
    }
}
