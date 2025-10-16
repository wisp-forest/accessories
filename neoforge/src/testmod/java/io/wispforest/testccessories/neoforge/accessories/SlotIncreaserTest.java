package io.wispforest.testccessories.neoforge.accessories;

import io.wispforest.accessories.api.attributes.AccessoryAttributeBuilder;
import io.wispforest.accessories.api.attributes.SlotAttribute;
import io.wispforest.accessories.api.core.Accessory;
import io.wispforest.accessories.api.core.AccessoryRegistry;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.testccessories.neoforge.TestItems;
import io.wispforest.testccessories.neoforge.Testccessories;
import io.wispforest.testccessories.neoforge.UniqueSlotTest;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

public class SlotIncreaserTest implements Accessory {

    public static void init(){
        AccessoryRegistry.register(TestItems.testItem2.get(), new SlotIncreaserTest());
    }

    @Override
    public void getDynamicModifiers(ItemStack stack, SlotReference reference, AccessoryAttributeBuilder builder) {
        builder.addExclusive(
                SlotAttribute.getAttributeHolder(UniqueSlotTest.testSlot3Ref().slotName()),
                new AttributeModifier(Testccessories.of("weewoo"), 2, AttributeModifier.Operation.ADD_VALUE));

        builder.addExclusive(
                SlotAttribute.getAttributeHolder("something"),
                new AttributeModifier(Testccessories.of("woowoo"), 10, AttributeModifier.Operation.ADD_VALUE));
    }
}
