package dev.emi.trinkets.api;

import com.google.common.collect.ImmutableMap;
import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;

public class SlotGroup {

    private final String name;
    private final int slotId;
    private final int order;
    private final Map<String, SlotType> slots;

    private SlotGroup(Builder builder) {
        this.name = builder.name;
        this.slots = builder.slots;
        this.slotId = builder.slotId;
        this.order = builder.order;
    }

    protected SlotGroup(String name, int slotId, int order, Map<String, SlotType> slots){
        this.name = name;
        this.slotId = slotId;
        this.order= order;
        this.slots = slots;
    }

    public int getSlotId() {
        return slotId;
    }

    public int getOrder() {
        return order;
    }

    public String getName() {
        return name;
    }

    public Map<String, SlotType> getSlots() {
        return ImmutableMap.copyOf(slots);
    }

    public void write(CompoundTag data) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Name", name);
        tag.putInt("SlotId", slotId);
        tag.putInt("Order", order);
        CompoundTag typesTag = new CompoundTag();

        slots.forEach((id, slot) -> {
            CompoundTag typeTag = new CompoundTag();
            slot.write(typeTag);
            typesTag.put(id, typeTag);
        });
        tag.put("SlotTypes", typesTag);
        data.put("GroupData", tag);
    }

    public static SlotGroup read(CompoundTag data) {
        CompoundTag groupData = data.getCompound("GroupData");
        String name = groupData.getString("Name");
        int slotId = groupData.getInt("SlotId");
        int order = groupData.getInt("Order");
        CompoundTag typesTag = groupData.getCompound("SlotTypes");
        Builder builder = new Builder(name, slotId, order);

        for (String id : typesTag.getAllKeys()) {
            CompoundTag tag = (CompoundTag) typesTag.get(id);

            if (tag != null) {
                builder.addSlot(id, SlotType.read(tag));
            }
        }
        return builder.build();
    }

    public static class Builder {

        private final String name;
        private final int slotId;
        private final int order;
        public final Map<String, SlotType> slots = new HashMap<>();

        public Builder(String name, int slotId, int order) {
            this.name = name;
            this.slotId = slotId;
            this.order = order;
        }

        public Builder addSlot(String name, SlotType slot) {
            this.slots.put(name, slot);
            return this;
        }

        public SlotGroup build() {
            return new SlotGroup(this);
        }
    }
}