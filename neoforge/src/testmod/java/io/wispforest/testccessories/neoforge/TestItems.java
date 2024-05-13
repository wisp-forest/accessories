package io.wispforest.testccessories.neoforge;

import io.wispforest.testccessories.neoforge.accessories.WaterBreathingAccessory;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.function.Supplier;

public class TestItems {

    public static final DeferredRegister<Item> REGISTER = DeferredRegister.create(BuiltInRegistries.ITEM, Testccessories.MODID);

    public static final Supplier<Item> testItem1 = REGISTER.register("test_item_1", () -> new Item(new Item.Properties().stacksTo(1).durability(64)));
    public static final Supplier<Item> testItem2 = REGISTER.register("test_item_2", () -> new Item(new Item.Properties().stacksTo(1).durability(64)));

    public static void init(RegisterEvent.RegisterHelper<Item> helper){
        //for (var entry : REGISTER.getEntries()) helper.register(entry.getId(), entry.getHolder());

        WaterBreathingAccessory.init();
    }

    public static void addToItemGroup(BuildCreativeModeTabContentsEvent event) {
        var key = BuiltInRegistries.CREATIVE_MODE_TAB.getResourceKey(event.getTab()).get();

        if(!key.equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) return;

        event.accept(testItem1.get());
        event.accept(testItem2.get());
    }
}
