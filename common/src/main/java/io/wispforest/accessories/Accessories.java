package io.wispforest.accessories;

import io.wispforest.accessories.client.AccessoriesMenu;
import io.wispforest.accessories.compat.AccessoriesConfig;
import io.wispforest.accessories.criteria.AccessoryEquippedCriterion;
import io.wispforest.accessories.mixin.CriteriaTriggersAccessor;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.storage.WorldData;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class Accessories {

    public static MenuType<AccessoriesMenu> ACCESSORIES_MENU_TYPE;

    public static AccessoryEquippedCriterion ACCESSORY_EQUIPPED;
    public static AccessoryEquippedCriterion ACCESSORY_UNEQUIPPED;

    public static final String MODID = "accessories";

    @Nullable
    private static ConfigHolder<AccessoriesConfig> CONFIG_HOLDER = null;

    public static void registerCriteria(){
        ACCESSORY_EQUIPPED = CriteriaTriggersAccessor.accessories$callRegister("accessories:equip_accessory", new AccessoryEquippedCriterion());
        ACCESSORY_UNEQUIPPED = CriteriaTriggersAccessor.accessories$callRegister("accessories:unequip_accessory", new AccessoryEquippedCriterion());
    }

    public static void registerMenuType() {
        ACCESSORIES_MENU_TYPE = AccessoriesInternals.registerMenuType(of("accessories_menu"), (integer, inventory) -> new AccessoriesMenu(integer, inventory, false, inventory.player));
    }

    public static void setupConfig(){
        CONFIG_HOLDER = AutoConfig.register(AccessoriesConfig.class, JanksonConfigSerializer::new);
    }

    public static AccessoriesConfig getConfig(){
        if(CONFIG_HOLDER == null) return null;

        return CONFIG_HOLDER.getConfig();
    }

    public static ResourceLocation of(String path){
        return new ResourceLocation(MODID, path);
    }

    public static String translation(String path){
        return MODID + "." + path;
    }
}