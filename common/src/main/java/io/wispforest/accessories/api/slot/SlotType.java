package io.wispforest.accessories.api.slot;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.DropRule;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;

/**
 * A Holder for information pertaining to a given Slot entry registered
 */
public interface SlotType {

    ResourceLocation EMPTY_SLOT_ICON = Accessories.of("gui/slot/empty");

    /**
     * @return The name of the given slot type.
     */
    String name();

    /**
     * @return The {@link Component} Translation key for the given slot type.
     */
    default String translation(){
        return Accessories.translation("slot." + name().replace(":", "."));
    }

    /**
     * @return The location for the given icon within the Block Atlas for the given slot type.
     */
    ResourceLocation icon();

    /**
     * @return The priority order for the given slot type.
     */
    int order();

    /**
     * @return The base amount for a given slot type.
     */
    int amount();

    /**
     * @return A set of ResourceLocation used to check if an accessory is valid for the given slot used within {@link AccessoriesAPI#canInsertIntoSlot}.
     */
    Set<ResourceLocation> validators();

    /**
     * @return The given {@link DropRule} used to upon an entity's death to handle accessory's equipped.
     */
    DropRule dropRule();
}
