package io.wispforest.accessories.api.events.extra;

import io.wispforest.accessories.api.slot.SlotReference;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.world.item.ItemStack;

/**
 * Event implemented on an Accessory to control if the given referenced entity can walk on powdered snow
 * <p/>
 * Such event is called within {@link ImplementedEvents#allowWalkingOnSnow} from either {@link ImplementedEvents#ALLOW_WALING_ON_SNOW_EVENT}
 * or if a given Accessory implements this interface
 */
public interface AllowWalingOnSnow {
    TriState allowWalkingOnSnow(ItemStack stack, SlotReference reference);
}
