package dev.emi.trinkets.compat;

import dev.emi.trinkets.api.SlotGroup;
import dev.emi.trinkets.api.SlotType;

import java.util.Map;

public class WrappedSlotGroup extends SlotGroup {

    public WrappedSlotGroup(Map<String, SlotType> slotTypeMap) {
        super("", 0, 0, slotTypeMap);
    }

    @Override
    public int getSlotId() {
        return super.getSlotId();
    }

    @Override
    public int getOrder() {
        return super.getOrder();
    }

    @Override
    public String getName() {
        return super.getName();
    }

    @Override
    public Map<String, SlotType> getSlots() {
        return super.getSlots();
    }
}
