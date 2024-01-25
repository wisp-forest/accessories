package io.wispforest.accessories;

import io.wispforest.accessories.client.AccessoriesMenu;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;

public class Accessories {

    public static MenuType<AccessoriesMenu> ACCESSORIES_MENU_TYPE;

    public static final String MODID = "accessories";

    public static void init() {
        ACCESSORIES_MENU_TYPE = AccessoriesAccess.getInternal().registerMenuType(of("accessories_menu"), AccessoriesMenu::new);
    }

    public static ResourceLocation of(String path){
        return new ResourceLocation(MODID, path);
    }

    public static String translation(String path){
        return MODID + "." + path;
    }
}
