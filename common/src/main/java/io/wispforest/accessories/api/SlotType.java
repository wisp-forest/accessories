package io.wispforest.accessories.api;

import io.wispforest.accessories.Accessories;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;

public interface SlotType {

    /**
     * Name of Slot
     */
    String name();

    default String translation(){
        return Accessories.translation(".slot." + name());
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

    Set<ResourceLocation> getValidators();

    DropRule dropRule();

    boolean hasCosmetics();

    enum DropRule {
        KEEP,
        DROP,
        DESTROY,
        DEFAULT
    }
}
