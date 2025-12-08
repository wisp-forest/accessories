package io.wispforest.accessories.impl.event;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.action.ActionResponse;
import io.wispforest.accessories.api.data.AccessoriesBaseData;
import io.wispforest.accessories.api.data.AccessoriesTags;
import io.wispforest.accessories.api.slot.validator.SlotValidator;
import io.wispforest.accessories.api.slot.validator.SlotValidatorRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BannerItem;

public class VanillaItemPredicates {
    public static final SlotValidator ELYTRA_PREDICATE = (level, slotType, slot, stack, buffer) -> {
        var validSlots = Accessories.config().contentOptions.validGliderSlots();

        if (!stack.has(DataComponents.GLIDER)) return;

        if (Accessories.config().contentOptions.allowGliderEquip() && AccessoriesBaseData.isValidSlotWithAny(validSlots, slotType)) {
            buffer.respondWith(ActionResponse.of(true, Component.literal("Found to allow the glider to be equipped as configured.")));
        } else if (slotType.name().equals(AccessoriesBaseData.CAPE_SLOT) && stack.is(AccessoriesTags.VALID_GLIDER_EQUIP)){
            buffer.respondWith(ActionResponse.of(true, Component.literal("Found to allow the glider to be equipped as tagged.")));
        }
    };

    public static final SlotValidator TOTEM_PREDICATE = (level, slotType, slot, stack, buffer) -> {
        var validSlots = Accessories.config().contentOptions.validTotemSlots();

        if(stack.has(DataComponents.DEATH_PROTECTION)) return;

        if (Accessories.config().contentOptions.allowTotemEquip() && AccessoriesBaseData.isValidSlotWithAny(validSlots, slotType)) {
            buffer.respondWith(ActionResponse.of(true, Component.literal("Found to allow the totem to be equipped as configured.")));
        } else if (slotType.name().equals(AccessoriesBaseData.CHARM_SLOT) && stack.is(AccessoriesTags.VALID_TOTEM_EQUIP)){
            buffer.respondWith(ActionResponse.of(true, Component.literal("Found to allow the totem to be equipped as tagged.")));
        }
    };

    public static final SlotValidator BANNER_PREDICATE = (level, slotType, slot, stack, buffer) -> {
        var validSlots = Accessories.config().contentOptions.validBannerSlots();

        if(!(stack.getItem() instanceof BannerItem)) return;

        if (Accessories.config().contentOptions.allowBannerEquip() && AccessoriesBaseData.isValidSlotWithAny(validSlots, slotType)) {
            buffer.respondWith(ActionResponse.of(true, Component.literal("Found to allow the banner to be equipped as configured.")));
        }
    };

    public static void init() {
        SlotValidatorRegistry.register(Accessories.of("elytra_item"), ELYTRA_PREDICATE);
        SlotValidatorRegistry.register(Accessories.of("totem_item"), TOTEM_PREDICATE);
        SlotValidatorRegistry.register(Accessories.of("banner_item"), BANNER_PREDICATE);
    }
}
