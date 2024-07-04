package io.wispforest.testccessories.fabric;

import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.slot.SlotBasedPredicate;
import io.wispforest.accessories.api.slot.SlotTypeReference;
import io.wispforest.accessories.api.slot.UniqueSlotHandling;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

public class UniqueSlotTest implements UniqueSlotHandling.RegistrationCallback {

    private final ResourceLocation slotPredicate1 = Testccessories.of("test_slot_1_equipment");
    private final ResourceLocation slotPredicate2 = Testccessories.of("test_slot_2_equipment");

    public static final UniqueSlotTest INSTANCE = new UniqueSlotTest();

    private UniqueSlotTest(){
        AccessoriesAPI.registerPredicate(slotPredicate1, SlotBasedPredicate.ofItem(item -> item.equals(TestItems.testItem1)));
        AccessoriesAPI.registerPredicate(slotPredicate2, SlotBasedPredicate.ofItem(item -> item.equals(TestItems.testItem2)));
    }

    private static SlotTypeReference testSlot1Getter;
    private static SlotTypeReference testSlot2Getter;

    @Override
    public void registerSlots(UniqueSlotHandling.UniqueSlotBuilderFactory factory) {
        testSlot1Getter = factory.create(Testccessories.of("test_slot_1"), 1)
                .slotPredicates(slotPredicate1)
                .validTypes(EntityType.PLAYER)
                .build();

        testSlot2Getter = factory.create(Testccessories.of("test_slot_2"), 1)
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
