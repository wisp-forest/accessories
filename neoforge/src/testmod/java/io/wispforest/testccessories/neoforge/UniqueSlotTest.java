package io.wispforest.testccessories.neoforge;

import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.slot.SlotBasedPredicate;
import io.wispforest.accessories.api.slot.SlotTypeReference;
import io.wispforest.accessories.api.slot.UniqueSlotHandling;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

public class UniqueSlotTest implements UniqueSlotHandling.RegistrationCallback {

    public static final UniqueSlotTest INSTANCE = new UniqueSlotTest();

    private UniqueSlotTest(){}

    private static SlotTypeReference testSlot1Getter;
    private static SlotTypeReference testSlot2Getter;

    @Override
    public void registerSlots(UniqueSlotHandling.UniqueSlotBuilderFactory factory) {
        var slotPredicate1 = new ResourceLocation(Testccessories.MODID, "test_slot_1_equipment");
        var slotPredicate2 = new ResourceLocation(Testccessories.MODID, "test_slot_2_equipment");

        AccessoriesAPI.registerPredicate(slotPredicate1, SlotBasedPredicate.ofItem(item -> item.equals(TestItems.testItem1)));
        AccessoriesAPI.registerPredicate(slotPredicate2, SlotBasedPredicate.ofItem(item -> item.equals(TestItems.testItem2)));

        testSlot1Getter = factory.create(new ResourceLocation(Testccessories.MODID, "test_slot_1"), 1)
                .slotPredicates(slotPredicate1)
                .validTypes(EntityType.PLAYER)
                .build();

        testSlot2Getter = factory.create(new ResourceLocation(Testccessories.MODID, "test_slot_2"), 1)
                .slotPredicates(slotPredicate2)
                .validTypes(EntityType.PLAYER)
                .build();
    }

    @Nullable
    public static SlotTypeReference testSlot1Ref() {
        return testSlot1Getter;
    }

    @Nullable
    public static SlotTypeReference testSlot2Ref() {
        return testSlot2Getter;
    }
}
