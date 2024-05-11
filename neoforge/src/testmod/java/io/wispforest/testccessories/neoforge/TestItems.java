package io.wispforest.testccessories.neoforge;

import io.wispforest.testccessories.neoforge.accessories.WaterBreathingAccessory;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class TestItems {

    public static final DeferredRegister<Item> REGISTER = DeferredRegister.create(ForgeRegistries.ITEMS, Testccessories.MODID);

    public static final Supplier<Item> testItem1 = REGISTER.register("test_item_1", () -> new Item(new Item.Properties().stacksTo(1).durability(64)));
    public static final Supplier<Item> testItem2 = REGISTER.register("test_item_2", () -> new Item(new Item.Properties().stacksTo(1).durability(64)));

    public static void init(RegisterEvent.RegisterHelper<Item> helper){
        //for (var entry : REGISTER.getEntries()) helper.register(entry.getId(), entry.getHolder());

        WaterBreathingAccessory.init();
    }

    public static void addToItemGroup(BuildCreativeModeTabContentsEvent event) {
        var key = BuiltInRegistries.CREATIVE_MODE_TAB.getResourceKey(event.getTab()).get();

        if(!key.equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) return;

        event.accept(testItem1);
        event.accept(testItem2);
    }
}
