package io.wispforest.testccessories.fabric.accessories;

import com.google.common.collect.HashMultimap;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.Accessory;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.testccessories.fabric.Testccessories;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.UUID;

public class RingIncreaserAccessory implements Accessory {

    public static void init(){
        AccessoriesAPI.registerAccessory(Items.BEACON, new RingIncreaserAccessory());
    }

    private static final ResourceLocation ringAdditionLocation = Testccessories.of("additional_rings");

    @Override
    public void onEquip(ItemStack stack, SlotReference reference) {
        var map = HashMultimap.<String, AttributeModifier>create();

        map.put("ring", new AttributeModifier(ringAdditionLocation, 100, AttributeModifier.Operation.ADD_VALUE));
        
        reference.capability().addPersistentSlotModifiers(map);
    }

    @Override
    public void onUnequip(ItemStack stack, SlotReference reference) {
        var map = HashMultimap.<String, AttributeModifier>create();

        map.put("ring", new AttributeModifier(ringAdditionLocation, 100, AttributeModifier.Operation.ADD_VALUE));

       reference.capability().removeSlotModifiers(map);
    }
}
