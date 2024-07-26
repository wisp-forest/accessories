package dev.emi.trinkets.api;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import dev.emi.trinkets.compat.WrappingTrinketsUtils;
import io.wispforest.accessories.api.attributes.SlotAttribute;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.Map;
import java.util.UUID;

public class SlotAttributes {
    private static final Map<String, ResourceLocation> CACHED_UUIDS = Maps.newHashMap();
    private static final Map<String, Holder<Attribute>> CACHED_ATTRIBUTES = Maps.newHashMap();

    /**
     * Adds an Entity Attribute Nodifier for slot count to the provided multimap
     */
    public static void addSlotModifier(Multimap<Attribute, AttributeModifier> map, String slot, ResourceLocation location, double amount, AttributeModifier.Operation operation) {
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