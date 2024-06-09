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

    ResourceLocation UNKNOWN = Accessories.of("gui/group/unknown");

    /**
     * Name of Group
     */
    String name();

    default String translation(){
        return Accessories.translation("slot_group." + name());
    }

    /**
     * Priority Order for Group
     */
    int order();

    /**
     * All slot names bound to the given group
     */
    Set<String> slots();

    /**
     * Location of icon
     */
    ResourceLocation icon();

    default boolean uniqueGroup() {
        return UniqueSlotHandling.getGroups().contains(name());
    }
}
