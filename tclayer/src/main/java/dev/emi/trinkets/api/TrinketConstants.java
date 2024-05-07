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

    public static String filterGroup(String path){
        if(!path.contains("/")) return path;

        var parts = path.split("/");

        if(parts.length <= 1) return path;

        StringBuilder builder = new StringBuilder();

        for (int i = 1; i < parts.length; i++) builder.append(parts[i]);

        return builder.toString();
    }

    public static boolean isValid(SlotReference slotReference, ItemStack stack){
        var ref = WrappingTrinketsUtils.createReference(slotReference);

        if(ref.isEmpty()) return false;

        boolean res = TrinketsApi.evaluatePredicateSet(Set.of(new ResourceLocation("trinkets:all")), stack, ref.get(), slotReference.entity());
        boolean canInsert = TrinketSlot.canInsert(stack, ref.get(), slotReference.entity());

        return res && canInsert;
    }
}
