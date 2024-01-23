package io.wispforest.accessories.impl;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.wispforest.accessories.AccessoriesAccess;
import io.wispforest.accessories.api.*;
import io.wispforest.accessories.networking.AccessoriesNetworkHandler;
import io.wispforest.accessories.networking.client.SyncContainers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.function.Supplier;

public class AccessoriesEvents {

    public static void tick(LivingEntity entity){
        var api = AccessoriesAccess.getAPI();
        var possibleCapability = api.getCapability(entity);

        if(possibleCapability.isEmpty()) return;

        var capability = (AccessoriesCapabilityImpl) possibleCapability.get();

        var dirtyStacks = new HashMap<String, ItemStack>();
        var dirtyCosmeticStacks = new HashMap<String, ItemStack>();

        for (var containerEntry : capability.getContainers().entrySet()) {
            var container = containerEntry.getValue();
            var accessories = (ExpandedSimpleContainer) container.getAccessories();

            for (int i = 0; i < accessories.getContainerSize(); i++) {
                var slotReference = new SlotReference(container.slotType(), capability.getEntity(), i);

                var slotId = AccessoriesAPI.slottedId(container.slotType(), i);

                var currentStack = accessories.getItem(i);

                if(!currentStack.isEmpty()){
                    currentStack.inventoryTick(entity.level(), entity, -1, false);

                    api.getAccessory(currentStack).ifPresent(accessory -> accessory.tick(currentStack, slotReference));
                }

                var lastStack = accessories.getPreviousItem(i);

                if(!ItemStack.matches(currentStack, lastStack)){
                    if(!entity.level().isClientSide()) {
                        accessories.setPreviousItem(i, currentStack.copy());
                        dirtyStacks.put(slotId, currentStack.copy());
                        var uuid = api.getOrCreateSlotUUID(container.slotType(), i);

                        if (!lastStack.isEmpty()) {
                            Accessory accessory = api.getOrDefaultAccessory(lastStack);
                            Multimap<Attribute, AttributeModifier> attributes = accessory.getModifiers(lastStack, slotReference, uuid);
                            Multimap<String, AttributeModifier> slotModifiers = HashMultimap.create();

                            Set<Attribute> slotAttributes = new HashSet<>();

                            for (var entry : attributes.asMap().entrySet()) {
                                if (!(entry.getKey() instanceof SlotAttribute slotAttribute)) continue;

                                slotModifiers.putAll(slotAttribute.slotName(), entry.getValue());
                                slotAttributes.add(slotAttribute);
                            }

                            slotAttributes.forEach(attributes::removeAll);

                            entity.getAttributes().removeAttributeModifiers(attributes);
                            capability.removeSlotModifiers(slotModifiers);
                        }

                        if (!currentStack.isEmpty()) {
                            Accessory accessory = api.getOrDefaultAccessory(currentStack);
                            Multimap<Attribute, AttributeModifier> attributes = accessory.getModifiers(currentStack, slotReference, uuid);
                            Multimap<String, AttributeModifier> slotModifiers = HashMultimap.create();

                            Set<Attribute> slotAttributes = new HashSet<>();

                            for (var entry : attributes.asMap().entrySet()) {
                                if (!(entry.getKey() instanceof SlotAttribute slotAttribute)) continue;

                                slotModifiers.putAll(slotAttribute.slotName(), entry.getValue());
                                slotAttributes.add(slotAttribute);
                            }

                            slotAttributes.forEach(attributes::removeAll);

                            entity.getAttributes().addTransientAttributeModifiers(attributes);
                            capability.addTransientSlotModifiers(slotModifiers);
                        }
                    }

                    if(!ItemStack.isSameItem(currentStack, lastStack)){
                        api.getOrDefaultAccessory(lastStack.getItem()).onUnequip(lastStack, slotReference);
                        api.getOrDefaultAccessory(currentStack.getItem()).onEquip(currentStack, slotReference);
                    }
                }

                var cosmetics = container.getCosmeticAccessories();

                var currentCosmeticStack = cosmetics.getItem(i);
                var lastCosmeticStack = cosmetics.getPreviousItem(i);

                if(!ItemStack.matches(currentCosmeticStack, lastCosmeticStack)){
                    if(!entity.level().isClientSide()) {
                        dirtyCosmeticStacks.put(slotId, currentCosmeticStack.copy());
                        cosmetics.setPreviousItem(i, currentCosmeticStack.copy());
                    }
                }
            }

            if(!entity.level().isClientSide()) {
                Set<AccessoriesContainer> updatedContainers = capability.getUpdatingInventories();

                if(!dirtyStacks.isEmpty() || !dirtyCosmeticStacks.isEmpty() || !updatedContainers.isEmpty()) {
                    var packet = new SyncContainers(entity.getId(), updatedContainers, dirtyStacks, dirtyCosmeticStacks);

                    var bufData = AccessoriesNetworkHandler.createBuf();

                    packet.read(bufData);

                    var networkHandler = AccessoriesAccess.getHandler();

                    networkHandler.sendToTrackingAndSelf(entity, (Supplier<SyncContainers>) () -> new SyncContainers(bufData));
                }
            }
        }
    }
}
