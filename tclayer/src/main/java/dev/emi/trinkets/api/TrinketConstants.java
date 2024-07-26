package dev.emi.trinkets.api;

import dev.emi.trinkets.TrinketSlot;
import dev.emi.trinkets.compat.WrappingTrinketsUtils;
import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;

public class TrinketConstants {
    public static final String MOD_ID = "trinkets";
    public static final Logger LOGGER = LogManager.getLogger();

    public static boolean isValid(SlotReference slotReference, ItemStack stack){
        var ref = WrappingTrinketsUtils.createReference(slotReference);

        if(ref.isEmpty()) return false;

        boolean res = TrinketsApi.evaluatePredicateSet(Set.of(ResourceLocation.tryParse("trinkets:all")), stack, ref.get(), slotReference.entity());
        boolean canInsert = TrinketSlot.canInsert(stack, ref.get(), slotReference.entity());

        return res && canInsert;
    }
}
