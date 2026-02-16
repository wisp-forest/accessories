package io.wispforest.testccessories.fabric.accessories;

import com.google.common.collect.HashMultimap;
import io.wispforest.accessories.api.action.ActionResponseBuffer;
import io.wispforest.accessories.api.action.HolderSetValidationResponse;
import io.wispforest.accessories.api.core.Accessory;
import io.wispforest.accessories.api.core.AccessoryRegistry;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.testccessories.fabric.Testccessories;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public class RingIncreaserAccessory implements Accessory {

    public static void init(){
        AccessoryRegistry.register(Items.BEACON, new RingIncreaserAccessory());
    }

    private static final ResourceLocation ringAdditionLocation = Testccessories.of("additional_rings");

    @Override
    public void onEquip(ItemStack stack, SlotReference reference) {
        var map = HashMultimap.<String, AttributeModifier>create();

        map.put("ring", new AttributeModifier(ringAdditionLocation, 100, AttributeModifier.Operation.ADD_VALUE));

        stack.set(DataComponents.BASE_COLOR, DyeColor.BLACK);
        
        reference.capability().addPersistentSlotModifiers(map);
    }

    @Override
    public void onUnequip(ItemStack stack, SlotReference reference) {
        var map = HashMultimap.<String, AttributeModifier>create();

        map.put("ring", new AttributeModifier(ringAdditionLocation, 100, AttributeModifier.Operation.ADD_VALUE));

        stack.remove(DataComponents.BASE_COLOR);

       reference.capability().removeSlotModifiers(map);
    }

    @Override
    public void canEquip(ItemStack stack, SlotReference reference, ActionResponseBuffer buffer) {
        buffer.respondWith(new HolderSetValidationResponse<>(List.of(EntityType.PLAYER.builtInRegistryHolder()), reference.entity().getType().builtInRegistryHolder()));
    }
}
