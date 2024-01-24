package io.wispforest.accessories.impl;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.AccessoriesAccess;
import io.wispforest.accessories.api.*;
import io.wispforest.accessories.networking.AccessoriesNetworkHandler;
import io.wispforest.accessories.networking.client.SyncContainers;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus;

import java.util.*;
import java.util.function.Supplier;

@ApiStatus.Internal
public class AccessoriesEvents {

    public static boolean dataReloadOccured = false;

    public static void onWorldTick(Level level){
        if(!dataReloadOccured || !(level instanceof ServerLevel serverLevel)) return;

        var api = AccessoriesAccess.getAPI();

        for (var player : serverLevel.getServer().getPlayerList().getPlayers()) {
            var capability = api.getCapability(player);

            if(capability.isEmpty()) continue;

            var validSlotTypes = api.getEntitySlots(player).values();

            for (var containerEntry : capability.get().getContainers().entrySet()) {
                var slotType = api.getSlotType(level, containerEntry.getKey());

                var container = containerEntry.getValue();

                if(slotType.isPresent() && validSlotTypes.contains(slotType.get())){
                    var stacks = container.getAccessories();
                    var cosmeticStacks = container.getCosmeticAccessories();

                    for (int i = 0; i < container.getSize(); i++) {
                        var reference = new SlotReference(slotType.get(), player, i);

                        handleInvalidStacks(stacks, reference, player);
                        handleInvalidStacks(cosmeticStacks, reference, player);
                    }
                } else {
                    // TODO: DROP CONTAINER!
                }
            }
        }

        dataReloadOccured = false;
    }

    private static void handleInvalidStacks(Container container, SlotReference reference, ServerPlayer player){
        var api = AccessoriesAccess.getAPI();
        var stack = container.getItem(reference.slot());

        if (!api.canInsertIntoSlot(player, reference, stack)) {
            container.setItem(reference.slot(), ItemStack.EMPTY);
            AccessoriesAccess.giveItemToPlayer(player, stack);
        }
    }

    public static void onLivingEntityTick(LivingEntity entity){
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

                    bufData.release();
                }
            }
        }
    }

    public static void addTooltipInfo(LivingEntity entity, ItemStack stack, List<Component> tooltip){
        var api = AccessoriesAccess.getAPI();

        var accessory = api.getAccessory(stack);

        if(accessory.isEmpty()) return;

        // TODO: MAYBE DEPENDING ON ENTITY OR SOMETHING SHOW ALL VALID SLOTS BUT COLOR CODE THEM IF NOT VALID FOR ENTITY?
        var slotTypes = api.getValidSlotTypes(entity, stack);
        var allSlotTypes = api.getAllSlots(entity.level());

        if(slotTypes.isEmpty()) return;

        var capability = api.getCapability(entity);

        if(capability.isEmpty()) return;

        var slotInfoComponent = Component.literal("");

        var slotsComponent = Component.literal("");
        boolean allSlots = false;

        if(slotTypes.containsAll(allSlotTypes.values())) {
            slotsComponent.append("slot.any");
            allSlots = true;
        } else {
            slotTypes.forEach(slotType -> slotsComponent.append(Component.translatable(slotType.translation())));
        }

        var slotTranslationKey = "slot.tooltip." + ((slotTypes.size() > 1 && !allSlots) ? "plural" : "singular");

        slotInfoComponent.append(Component.translatable(Accessories.translation(slotTranslationKey), slotsComponent));

        slotInfoComponent.append(Component.translatable(Accessories.translation("accessories.tooltip.attributes")));

        Map<SlotType, Multimap<Attribute, AttributeModifier>> slotSpecificModifiers = new HashMap<>();
        Multimap<Attribute, AttributeModifier> defaultModifiers = null;

        boolean allDuplicates = true;

        for (SlotType slotType : slotTypes) {
            var reference = new SlotReference(slotType, entity, 0);
            var uuid = api.getOrCreateSlotUUID(slotType, 0);

            var slotModifiers = accessory.get().getModifiers(stack, reference, uuid);

            slotSpecificModifiers.put(slotType, slotModifiers);

            if(defaultModifiers != null){
                defaultModifiers = slotModifiers;
            } else if(allDuplicates) {
                // WARNING: THIS MAY NOT WORK?
                allDuplicates = defaultModifiers.equals(slotModifiers);
            }
        }

        accessory.get().getExtraTooltip(stack, tooltip);

        Map<SlotType, List<Component>> slotTypeToTooltipInfo = new HashMap<>();

        if(allDuplicates){
            var attributeTooltip = new ArrayList<Component>();
            attributeTooltip.add(Component.translatable(Accessories.translation("tooltip.attributes.any")));
            addAttributeTooltip(defaultModifiers, attributeTooltip);

            slotTypeToTooltipInfo.put(null, attributeTooltip);
        } else {
            for (var slotModifiers : slotSpecificModifiers.entrySet()) {
                tooltip.add(Component.translatable(Accessories.translation("tooltip.attributes.slot")));
                addAttributeTooltip(slotModifiers.getValue(), tooltip);
            }
        }

        Map<SlotType, List<Component>> extraAttributeTooltips = new HashMap<>();
        List<Component> defaultExtraAttributeTooltip = null;

        boolean allDuplicatesExtras = true;

        for (SlotType slotType : slotTypes) {
            var extraAttributeTooltip = new ArrayList<Component>();
            accessory.get().getAttributesTooltip(stack, extraAttributeTooltip);

            extraAttributeTooltips.put(slotType, extraAttributeTooltip);

            if(defaultExtraAttributeTooltip == null){
                defaultExtraAttributeTooltip = null;
            } else if(allDuplicatesExtras){
                allDuplicatesExtras = extraAttributeTooltip.equals(defaultExtraAttributeTooltip);
            }
        }

        if(allDuplicatesExtras){
            slotTypeToTooltipInfo.computeIfAbsent(null, s -> new ArrayList<>())
                    .addAll(defaultExtraAttributeTooltip);
        } else {
            extraAttributeTooltips.forEach((slotType, components) -> {
                slotTypeToTooltipInfo.computeIfAbsent(slotType, s -> new ArrayList<>())
                        .addAll(components);
            });
        }

        if(slotTypeToTooltipInfo.size() > 1) {
            for (var entry : slotTypeToTooltipInfo.entrySet()) {
                tooltip.add(Component.translatable(Accessories.translation("accessories.tooltip.attributes.slot"), Component.translatable(entry.getKey().translation())));
                tooltip.addAll(entry.getValue());
            }
        } else {
            tooltip.add(Component.translatable(Accessories.translation("accessories.tooltip.attributes.any")));
            tooltip.addAll(slotTypeToTooltipInfo.get(null));
        }

        accessory.get().getExtraTooltip(stack, tooltip);
    }

    private static void addAttributeTooltip(Multimap<Attribute, AttributeModifier> multimap, List<Component> tooltip){
        if (multimap.isEmpty()) return;

        tooltip.add(CommonComponents.EMPTY);

        for(Map.Entry<Attribute, AttributeModifier> entry : multimap.entries()) {
            AttributeModifier attributeModifier = entry.getValue();
            double d = attributeModifier.getAmount();

            if (attributeModifier.getOperation() == AttributeModifier.Operation.MULTIPLY_BASE
                    || attributeModifier.getOperation() == AttributeModifier.Operation.MULTIPLY_TOTAL) {
                d *= 100.0;
            } else if (entry.getKey().equals(Attributes.KNOCKBACK_RESISTANCE)) {
                d *= 10.0;
            }

            if (d > 0.0) {
                tooltip.add(
                        Component.translatable(
                                        "attribute.modifier.plus." + attributeModifier.getOperation().toValue(),
                                        ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(d),
                                        Component.translatable(entry.getKey().getDescriptionId())
                                )
                                .withStyle(ChatFormatting.BLUE)
                );
            } else if (d < 0.0) {
                d *= -1.0;
                tooltip.add(
                        Component.translatable(
                                        "attribute.modifier.take." + attributeModifier.getOperation().toValue(),
                                        ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(d),
                                        Component.translatable(entry.getKey().getDescriptionId())
                                )
                                .withStyle(ChatFormatting.RED)
                );
            }
        }
    }

    public static void onDeath(LivingEntity entity){
        var api = AccessoriesAccess.getAPI();

        var capability = api.getCapability(entity);

        if(capability.isEmpty()) return;

        for (var containerEntry : capability.get().getContainers().entrySet()) {
            var slotType = containerEntry.getValue().slotType();

            var slotDropRule = slotType.dropRule();

            var container = containerEntry.getValue();

            var stacks = container.getAccessories();
            var cosmeticStacks = container.getCosmeticAccessories();

            for (int i = 0; i < container.getSize(); i++) {
                var reference = new SlotReference(slotType, entity, i);

                dropStack(slotDropRule, stacks, reference, entity);
                dropStack(slotDropRule, cosmeticStacks, reference, entity);
            }
        }
    }

    private static void dropStack(SlotType.DropRule dropRule, Container container, SlotReference reference, LivingEntity entity){
        var api = AccessoriesAccess.getAPI();

        var stack = container.getItem(reference.slot());
        var accessory = api.getAccessory(stack);

        if(accessory.isPresent() && dropRule == SlotType.DropRule.DEFAULT) {
            dropRule = accessory.get().getDropRule(stack, reference);
        }

        boolean dropStack = true;

        if(dropRule == SlotType.DropRule.DESTROY){
            container.setItem(reference.slot(), ItemStack.EMPTY);
            dropStack = false;
            // TODO: Do we call break here?
        } else if(dropRule == SlotType.DropRule.DEFAULT){
            if(entity.level().getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).get()) {
                dropStack = true;
            } else if(EnchantmentHelper.hasVanishingCurse(stack)){
                container.setItem(reference.slot(), ItemStack.EMPTY);
                dropStack = false;
            }
        } else if(dropRule == SlotType.DropRule.KEEP) {
            dropStack = false;
        }

        if(!dropStack) return;

        if(entity instanceof Player player){
            player.drop(stack, true);
        } else {
            entity.spawnAtLocation(stack);
        }
    }
}
