package dev.emi.trinkets.api;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import dev.emi.trinkets.compat.WrappingTrinketsUtils;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.Map;
import java.util.UUID;

public class SlotAttributes {
    private static Map<String, UUID> CACHED_UUIDS = Maps.newHashMap();
    private static Map<String, SlotAttribute> CACHED_ATTRIBUTES = Maps.newHashMap();

    /**
     * Adds an Entity Attribute Nodifier for slot count to the provided multimap
     */
    public static void addSlotModifier(Multimap<Attribute, AttributeModifier> map, String slot, UUID uuid, double amount,
                                       AttributeModifier.Operation operation) {
        io.wispforest.accessories.api.slot.SlotAttribute.addSlotModifier(map, WrappingTrinketsUtils.trinketsToAccessories_Slot(TrinketConstants.filterGroup(slot)), uuid, amount, operation);
    }

    public static UUID getUuid(SlotReference ref) {
        String key = ref.inventory().getSlotType().getGroup() + "/" + ref.inventory().getSlotType().getName() + "/" + ref.index();
        CACHED_UUIDS.putIfAbsent(key, UUID.nameUUIDFromBytes(key.getBytes()));
        return CACHED_UUIDS.get(key);
    }

    public static class SlotAttribute extends Attribute {
        public String slot;

        private SlotAttribute(String slot) {
            super("curios.slot." + slot, 0);
            this.slot = slot;
        }
    }

    public static class WrappedSlotAttribute extends SlotAttribute {
        private final io.wispforest.accessories.api.slot.SlotAttribute attribute;

        public WrappedSlotAttribute(io.wispforest.accessories.api.slot.SlotAttribute attribute){
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