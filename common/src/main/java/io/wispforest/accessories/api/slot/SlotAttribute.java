package io.wispforest.accessories.api.slot;

import net.minecraft.world.entity.ai.attributes.Attribute;

import java.util.HashMap;
import java.util.Map;

public class SlotAttribute extends Attribute {

    private static final Map<String, SlotAttribute> CACHED_ATTRIBUTES = new HashMap<>();

    private final String slotName;

    private SlotAttribute(String slotName) {
        super(slotName, 0);

        this.slotName = slotName;
    }

    public String slotName(){
        return this.slotName;
    }

    public static SlotAttribute getSlotAttribute(SlotType slotType){
        return getSlotAttribute(slotType.name());
    }

    public static SlotAttribute getSlotAttribute(String slotName){
        return CACHED_ATTRIBUTES.computeIfAbsent(slotName, SlotAttribute::new);
    }
}
