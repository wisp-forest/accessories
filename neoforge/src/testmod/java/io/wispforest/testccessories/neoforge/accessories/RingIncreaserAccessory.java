package io.wispforest.testccessories.neoforge.accessories;

import com.google.common.collect.HashMultimap;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.Accessory;
import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.UUID;

public class RingIncreaserAccessory implements Accessory {

    public static void init(){
        AccessoriesAPI.registerAccessory(Items.BEACON, new RingIncreaserAccessory());
    }

    private static final UUID ringAdditonUUID = UUID.nameUUIDFromBytes("additional_rings_uuid".getBytes());

    @Override
    public void onEquip(ItemStack stack, SlotReference reference) {
        var map = HashMultimap.<String, AttributeModifier>create();

        map.put("ring", new AttributeModifier(ringAdditonUUID, "additional_rings", 100, AttributeModifier.Operation.ADDITION));
        
        reference.capability().addPersistentSlotModifiers(map);
    }

    @Override
    public void onUnequip(ItemStack stack, SlotReference reference) {
        var map = HashMultimap.<String, AttributeModifier>create();

        map.put("ring", new AttributeModifier(ringAdditonUUID, "additional_rings", 100, AttributeModifier.Operation.ADDITION));

       reference.capability().removeSlotModifiers(map);
    }
}
