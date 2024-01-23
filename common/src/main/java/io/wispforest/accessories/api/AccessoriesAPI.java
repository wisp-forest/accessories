package io.wispforest.accessories.api;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.AccessoriesAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;


import java.util.*;

public abstract class AccessoriesAPI {

    public static final Accessory DEFAULT = new Accessory() {};

    private static final Map<String, UUID> CACHED_UUIDS = new HashMap<>();

    public abstract Optional<AccessoriesCapability> getCapability(LivingEntity livingEntity);

    public abstract void registerAccessory(Item item, Accessory accessory);

    public Optional<Accessory> getAccessory(ItemStack stack){
        return getAccessory(stack.getItem());
    }

    public abstract Optional<Accessory> getAccessory(Item item);

    public Accessory getOrDefaultAccessory(ItemStack stack){
        return getOrDefaultAccessory(stack.getItem());
    }

    public Accessory getOrDefaultAccessory(Item item){
        return getAccessory(item).orElse(defaultAccessory());
    }

    public Accessory defaultAccessory(){
        return DEFAULT;
    }

    //--

    public static Multimap<Attribute, AttributeModifier> getAttributeModifiers(ItemStack stack, SlotReference reference, UUID uuid){
        Multimap<Attribute, AttributeModifier> multimap = HashMultimap.create();
        var api = AccessoriesAccess.getAPI();

        if(stack.getTag() != null && stack.getTag().contains("AccessoriesAttributeModifiers", Tag.TAG_LIST)){
            var attributes = stack.getTag().getList("AccessoriesAttributeModifiers", Tag.TAG_COMPOUND);

            for (int i = 0; i < attributes.size(); i++) {
                var attributeTag = attributes.getCompound(i);

                if(attributeTag.contains("slot") && attributeTag.getString("slot").equals(reference.type().name())){
                    var attributeType = ResourceLocation.tryParse(attributeTag.getString("AttributeName"));
                    var id = uuid;

                    if(attributeType != null) continue;

                    if(attributeTag.contains("UUID")) id = attributeTag.getUUID("UUID");

                    if(id.getLeastSignificantBits() == 0 || id.getMostSignificantBits() == 0) continue;

                    var operation = AttributeModifier.Operation.fromValue(attributeTag.getInt("Operation"));
                    var amount = attributeTag.getDouble("Amount");
                    var name = attributeTag.getString("Name");

                    var attributeModifier = new AttributeModifier(id, name, amount, operation);

                    if(attributeType.getNamespace().equals(Accessories.MODID)){
                        var slotName = attributeType.getPath();

                        if(!api.getAllSlots(reference.entity().level()).containsKey(slotName)) continue;

                        multimap.put(SlotAttribute.getSlotAttribute(slotName), attributeModifier);
                    } else {
                        var attribute = BuiltInRegistries.ATTRIBUTE.getOptional(attributeType);

                        if(attribute.isEmpty()) continue;

                        multimap.put(attribute.get(), attributeModifier);
                    }
                }
            }
        }

        //TODO: Decide if such presents of modifiers prevents the accessory modifiers from existing

        api.getAccessory(stack).ifPresent(accessory -> accessory.getModifiers(stack, reference, uuid));

        return multimap;
    }

    public UUID getOrCreateSlotUUID(SlotType slotType, int index) {
        return CACHED_UUIDS.computeIfAbsent(
                slottedId(slotType, index),
                s -> UUID.nameUUIDFromBytes(s.getBytes())
        );
    }

    public static String slottedId(SlotType slotType, int index) {
        return slotType.name() + "/" + index;
    }

    //--

    public Map<String, SlotType> getEntitySlots(LivingEntity livingEntity){
        return getSlots(livingEntity.level(), livingEntity.getType());
    }

    public abstract Map<String, SlotType> getSlots(Level level, EntityType<?> entityType);

    public abstract Map<String, SlotType> getAllSlots(Level level);

}
