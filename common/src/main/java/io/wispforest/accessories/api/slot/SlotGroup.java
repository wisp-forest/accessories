package io.wispforest.accessories.api.slot;

import io.wispforest.accessories.Accessories;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;

/**
 * A Group of {@link SlotType}'s based on name used to cosmetically
 * group them together for the UI
 */
public interface SlotGroup {

    ResourceLocation UNKNOWN = Accessories.of("gui/group/unknown");

    /**
     * @return The name of the given group
     */
    String name();

    /**
     * @return The {@link Component} Translation key for the given group
     */
    default String translation(){
        return Accessories.translation("slot_group." + name());
    }

    /**
     * @return The priority order for the given group
     */
    int order();

    /**
     * @return All slot names bound to the given group
     */
    Set<String> slots();

    /**
     * @return The location for the given icon within the Block Atlas for the given slot group
     */
    ResourceLocation icon();
}
