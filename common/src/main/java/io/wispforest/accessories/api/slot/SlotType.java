package io.wispforest.accessories.api.slot;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.DropRule;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;

/**
 * A Holder for information pertaining to a given Slot entry registered
 */
public interface SlotType {

    ResourceLocation EMPTY_SLOT_LOCATION = Accessories.of("gui/slot/empty");

    /**
     * Name of Slot
     */
    String name();

    default String translation(){
        return Accessories.translation("slot." + name().replace(":", "."));
    }

    /**
     * Location of icon
     */
    ResourceLocation icon();

    /**
     * Priority Order for Slot
     */
    int order();

    /**
     * Amount of slots of a given type
     */
    int amount();

    /**
     * Set of ResourceLocation used to check if such is a valid slot for given entry. Check {@link AccessoriesAPI#canInsertIntoSlot}
     */
    Set<ResourceLocation> validators();

    /**
     * Drop Rule used to result in item drops on Death
     */
    DropRule dropRule();
}
