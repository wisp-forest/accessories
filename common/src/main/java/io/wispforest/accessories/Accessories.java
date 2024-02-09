package io.wispforest.accessories;

import io.wispforest.accessories.api.Accessory;
import io.wispforest.accessories.api.SlotReference;
import io.wispforest.accessories.client.AccessoriesMenu;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class Accessories {

    public static MenuType<AccessoriesMenu> ACCESSORIES_MENU_TYPE;

    public static final String MODID = "accessories";

    public static void init() {
        ACCESSORIES_MENU_TYPE = AccessoriesAccess.getInternal().registerMenuType(of("accessories_menu"), (integer, inventory) -> new AccessoriesMenu(integer, inventory, false, inventory.player));
    }

    public static ResourceLocation of(String path){
        return new ResourceLocation(MODID, path);
    }

    public static String translation(String path){
        return MODID + "." + path;
    }
}