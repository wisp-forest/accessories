package io.wispforest.testccessories.fabric;

import io.wispforest.accessories.api.action.ActionResponse;
import io.wispforest.accessories.api.slot.SlotTypeReference;
import io.wispforest.accessories.api.slot.UniqueSlotHandling;
import io.wispforest.accessories.api.slot.validator.SlotValidator;
import io.wispforest.accessories.api.slot.validator.SlotValidatorRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import org.jetbrains.annotations.Nullable;

public class UniqueSlotTest implements UniqueSlotHandling.RegistrationCallback {

    private final ResourceLocation slotPredicate1 = Testccessories.of("test_slot_1_equipment");
    private final ResourceLocation slotPredicate2 = Testccessories.of("test_slot_2_equipment");
    private final ResourceLocation slotPredicate3 = Testccessories.of("test_slot_3_equipment");

    public static final UniqueSlotTest INSTANCE = new UniqueSlotTest();

    private UniqueSlotTest(){
        SlotValidatorRegistry.register(slotPredicate1, SlotValidator.ofItem(item -> item.equals(TestItems.testItem1)));
        SlotValidatorRegistry.register(slotPredicate2, SlotValidator.ofItem(item -> item.equals(TestItems.testItem2)));
        SlotValidatorRegistry.register(slotPredicate3, (level, slotType, slot, stack, buffer) -> {
            if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock().builtInRegistryHolder().is(BlockTags.BEDS)) {
                buffer.respondWith(ActionResponse.of(true, Component.literal("The bed fits within the bed slot!")));
            }
        });
    }

    private static SlotTypeReference testSlot1Getter;
    private static SlotTypeReference testSlot2Getter;
    private static SlotTypeReference testSlot3Getter;

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

        testSlot3Getter = factory.create(Testccessories.of("test_slot_3"), 0)
                .allowResizing(true)
                .slotPredicates(slotPredicate3)
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

    @Nullable
    public static SlotTypeReference testSlot3Ref() {
        return testSlot3Getter;
    }
}
