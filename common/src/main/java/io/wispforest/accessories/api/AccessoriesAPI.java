package io.wispforest.accessories.api;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.wispforest.accessories.AccessoriesAccess;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;


import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface AccessoriesAPI {

    Optional<AccessoriesCapability> getCapability(LivingEntity livingEntity);

    void registerAccessory(Item item, Accessory accessory);

    default Optional<Accessory> getAccessory(ItemStack stack){
        return getAccessory(stack.getItem());
    }

    Optional<Accessory> getAccessory(Item item);

    default Accessory getOrDefaultAccessory(ItemStack stack){
        return getOrDefaultAccessory(stack.getItem());
    }

    default Accessory getOrDefaultAccessory(Item item){
        return getAccessory(item).orElse(defaultAccessory());
    }

    Accessory defaultAccessory();

    //--

    static Multimap<Attribute, AttributeModifier> getAttributeModifiers(ItemStack stack, SlotReference reference, UUID uuid){
        Multimap<Attribute, AttributeModifier> multimap = HashMultimap.create();

        if(stack.getTag() != null && stack.getTag().contains("AccessoriesAttributeModifiers", 9)){

        }

        var api = AccessoriesAccess.getAPI();

        api.getAccessory(stack).ifPresent(accessory -> accessory.getModifiers(stack, reference, uuid));

        return multimap;
    }

    UUID getOrCreateSlotUUID(SlotType slotType, int index);

    static String slottedIdentifier(SlotType slotType, int index) {
        return slotType.name() + "/" + index;
    }

    //--

    default Map<String, SlotType> getEntitySlots(LivingEntity livingEntity){
        return getSlots(livingEntity.level(), livingEntity.getType());
    }

    Map<String, SlotType> getSlots(Level level, EntityType<?> entityType);
}
