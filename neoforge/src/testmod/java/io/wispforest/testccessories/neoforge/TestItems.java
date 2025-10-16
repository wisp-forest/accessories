package io.wispforest.testccessories.neoforge;

import io.wispforest.testccessories.neoforge.accessories.SlotIncreaserTest;
import io.wispforest.testccessories.neoforge.accessories.WaterBreathingAccessory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.function.Supplier;

public class TestItems {

    public static final DeferredRegister.Items REGISTER = DeferredRegister.createItems(Testccessories.MODID);

    public static final Supplier<Item> testItem1 = REGISTER.registerItem("test_item_1", (prop) -> new Item(prop.stacksTo(1).durability(64)));
    public static final Supplier<Item> testItem2 = REGISTER.registerItem("test_item_2", (prop) -> new Item(prop.stacksTo(1).durability(64)));

    public static void init(RegisterEvent.RegisterHelper<Item> helper){
        //for (var entry : REGISTER.getEntries()) helper.register(entry.getId(), entry.getHolder());

        WaterBreathingAccessory.init();
        SlotIncreaserTest.init();
    }

    public static void addToItemGroup(BuildCreativeModeTabContentsEvent event) {
        var key = BuiltInRegistries.CREATIVE_MODE_TAB.getResourceKey(event.getTab()).get();

        if(!key.equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) return;

        event.accept(testItem1.get());
        event.accept(testItem2.get());
    }
}
