package io.wispforest.accessories;

import net.minecraft.resources.ResourceLocation;

public class AccessoriesMod {
    public static final String MODID = "accessories";

    public static void init() {
    }

    public static ResourceLocation of(String path){
        return new ResourceLocation(MODID, path);
    }
}
