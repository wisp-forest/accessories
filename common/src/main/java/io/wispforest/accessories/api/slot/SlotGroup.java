package io.wispforest.accessories.api.slot;

import io.wispforest.accessories.Accessories;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;

/**
 * A Group of {@link SlotType}'s based on name used to cosmetically
 * group them together for the UI
 */
public interface SlotGroup {

    ResourceLocation UNKNOWN = Accessories.of("gui/slot/unknown");

    String name();

    default String translation(){
        return Accessories.translation("slot_group." + name());
    }

    int order();

    Set<String> slots();

    Pair<Integer, ResourceLocation> iconInfo();

    default boolean uniqueGroup() {
        return UniqueSlotHandling.getGroups().containsKey(name());
    }
}
