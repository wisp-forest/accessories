package io.wispforest.testccessories.fabric.accessories;

import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.Accessory;
import io.wispforest.accessories.api.attributes.AccessoryAttributeBuilder;
import io.wispforest.accessories.api.attributes.SlotAttribute;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.testccessories.fabric.TestItems;
import io.wispforest.testccessories.fabric.Testccessories;
import io.wispforest.testccessories.fabric.UniqueSlotTest;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

public class SlotIncreaserTest implements Accessory {

    public static void init(){
        AccessoriesAPI.registerAccessory(TestItems.testItem2, new SlotIncreaserTest());
    }

    @Override
    public void getDynamicModifiers(ItemStack stack, SlotReference reference, AccessoryAttributeBuilder builder) {
        builder.addExclusive(
                Holder.direct(SlotAttribute.getSlotAttribute(UniqueSlotTest.testSlot3Ref().slotName())),
                new AttributeModifier(Testccessories.of("weewoo"), 2, AttributeModifier.Operation.ADD_VALUE));

        builder.addExclusive(
                Holder.direct(SlotAttribute.getSlotAttribute("something")),
                new AttributeModifier(Testccessories.of("woowoo"), 10, AttributeModifier.Operation.ADD_VALUE));
    }
}
