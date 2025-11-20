package io.wispforest.accessories.impl.event;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.data.AccessoriesBaseData;
import io.wispforest.accessories.api.data.AccessoriesTags;
import io.wispforest.accessories.api.slot.SlotBasedPredicate;
import io.wispforest.accessories.api.slot.SlotPredicateRegistry;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BannerItem;

public class VanillaItemPredicates {
    public static final SlotBasedPredicate ELYTRA_PREDICATE = (level, slotType, slot, stack) -> {
        var validSlots = Accessories.config().contentOptions.validGliderSlots();

        if (stack.has(DataComponents.GLIDER)) {
            if ((validSlots.contains(slotType.name()) || validSlots.contains("any")) && Accessories.config().contentOptions.allowGliderEquip()) {
                return TriState.TRUE;
            } else if (slotType.name().equals("cape") && stack.is(AccessoriesTags.VALID_GLIDER_EQUIP)){
                return TriState.TRUE;
            }
        }

        return TriState.DEFAULT;
    };

    public static final SlotBasedPredicate TOTEM_PREDICATE = (level, slotType, slot, stack) -> {
        var validSlots = Accessories.config().contentOptions.validTotemSlots();

        if(stack.has(DataComponents.DEATH_PROTECTION)) {
            if ((validSlots.contains(slotType.name()) || validSlots.contains("any")) && Accessories.config().contentOptions.allowTotemEquip()) {
                return TriState.TRUE;
            } else if (slotType.name().equals(AccessoriesBaseData.CHARM_SLOT) && stack.is(AccessoriesTags.VALID_TOTEM_EQUIP)){
                return TriState.TRUE;
            }
        }

        return TriState.DEFAULT;
    };

    public static final SlotBasedPredicate BANNER_PREDICATE = (level, slotType, slot, stack) -> {
        var validSlots = Accessories.config().contentOptions.validBannerSlots();

        if(stack.getItem() instanceof BannerItem) {
            if ((validSlots.contains(slotType.name()) || validSlots.contains("any")) && Accessories.config().contentOptions.allowBannerEquip()) {
                return TriState.TRUE;
            }
        }

        return TriState.DEFAULT;
    };

    public static void init() {
        SlotPredicateRegistry.register(Accessories.of("elytra_item"), ELYTRA_PREDICATE);
        SlotPredicateRegistry.register(Accessories.of("totem_item"), TOTEM_PREDICATE);
        SlotPredicateRegistry.register(Accessories.of("banner_item"), BANNER_PREDICATE);
    }
}
