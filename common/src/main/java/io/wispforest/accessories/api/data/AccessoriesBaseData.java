package io.wispforest.accessories.api.data;

import io.wispforest.accessories.Accessories;
import net.minecraft.resources.ResourceLocation;

public class AccessoriesBaseData {

    public static final String ANKLET_SLOT = "anklet";
    public static final String BACK_SLOT = "back";
    public static final String BELT_SLOT = "belt";
    public static final String CAPE_SLOT = "cape";
    public static final String CHARM_SLOT = "charm";
    public static final String FACE_SLOT = "face";
    public static final String HAND_SLOT = "hand";
    public static final String HAT_SLOT = "hat";
    public static final String NECKLACE_SLOT = "necklace";
    public static final String RING_SLOT = "ring";
    public static final String SHOES_SLOT = "shoes";
    public static final String WRIST_SLOT = "wrist";

    public static final String MISC_GROUP = "misc";
    public static final String HEAD_GROUP = "head";
    public static final String CHEST_GROUP = "chest";
    public static final String ARM_GROUP = "arm";
    public static final String LEG_GROUP = "leg";
    public static final String FEET_GROUP = "feet";
    public static final String UNSORTED_GROUP = "unsorted";

    public static final ResourceLocation ALL_PREDICATE_ID = Accessories.of("all");
    public static final ResourceLocation NONE_PREDICATE_ID = Accessories.of("none");
    public static final ResourceLocation TAG_PREDICATE_ID = Accessories.of("tag");
    public static final ResourceLocation RELEVANT_PREDICATE_ID = Accessories.of("relevant");
    public static final ResourceLocation COMPONENT_PREDICATE_ID = Accessories.of("component");
}
