package io.wispforest.accessories.api;

import net.minecraft.resources.ResourceLocation;

public interface SlotType {

    /**
     * Name of Slot
     */
    public String name();

    /**
     * Location of icon
     */
    public ResourceLocation icon();

    /**
     * Priority Order for Slot
     */
    public int order();

    /**
     * Amount of slots of a given type
     */
    public int amount();

    public DropRule dropRule();

    public boolean hasCosmetics();

    public enum DropRule {
        KEEP,
        DROP,
        DESTROY,
        DEFAULT
    }
}
