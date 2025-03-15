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

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import dev.emi.trinkets.compat.WrappingTrinketsUtils;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.Map;

public class SlotAttributes {
    private static final Map<String, ResourceLocation> CACHED_UUIDS = Maps.newHashMap();
    private static final Map<String, Holder<Attribute>> CACHED_ATTRIBUTES = Maps.newHashMap();

    /**
     * Adds an Entity Attribute Nodifier for slot count to the provided multimap
     */
    public static void addSlotModifier(Multimap<Holder<Attribute>, AttributeModifier> map, String slot, ResourceLocation location, double amount, AttributeModifier.Operation operation) {
        var data = WrappingTrinketsUtils.splitGroupInfo(slot);

        var slotType = WrappingTrinketsUtils.trinketsToAccessories_Slot(data.left(), data.right());

        io.wispforest.accessories.api.attributes.SlotAttribute.addSlotModifier(map, slotType, location, amount, operation);
    }

    public static ResourceLocation getIdentifier(SlotReference ref) {
        String key = ref.inventory().getSlotType().getId() + "/" + ref.index();
        CACHED_UUIDS.computeIfAbsent(key, ResourceLocation::withDefaultNamespace);
        return CACHED_UUIDS.get(key);
    }

    public static class SlotAttribute extends Attribute {
        public String slot;

        private SlotAttribute(String slot) {
            super("trinkets.slot." + slot, 0);
            this.slot = slot;
        }
    }

    public static class WrappedSlotAttribute extends SlotAttribute {
        private final io.wispforest.accessories.api.attributes.SlotAttribute attribute;

        public WrappedSlotAttribute(io.wispforest.accessories.api.attributes.SlotAttribute attribute){
            super(attribute.slotName());

            this.attribute = attribute;
        }

        @Override
        public double getDefaultValue() {
            return attribute.getDefaultValue();
        }

        @Override
        public boolean isClientSyncable() {
            return attribute.isClientSyncable();
        }

        @Override
        public Attribute setSyncable(boolean watch) {
            return attribute.setSyncable(watch);
        }

        @Override
        public double sanitizeValue(double value) {
            return attribute.sanitizeValue(value);
        }

        @Override
        public String getDescriptionId() {
            return attribute.getDescriptionId();
        }
    }
}