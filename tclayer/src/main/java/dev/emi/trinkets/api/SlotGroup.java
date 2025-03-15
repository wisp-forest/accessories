/*
 * MIT License
 *
 * Copyright (c) 2019 Emily Rose Ploszaj
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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