package io.wispforest.accessories.impl;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.AccessoriesAccess;
import io.wispforest.accessories.api.*;
import io.wispforest.accessories.api.events.AccessoriesEvents;
import io.wispforest.accessories.networking.AccessoriesNetworkHandler;
import io.wispforest.accessories.networking.client.SyncContainer;
import io.wispforest.accessories.networking.client.SyncContainerData;
import io.wispforest.accessories.networking.client.SyncData;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.DamageSource;
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
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

@ApiStatus.Internal
public class AccessoriesEventHandler {

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
                        var reference = new SlotReference(container.getSlotName(), player, i);

                        handleInvalidStacks(stacks, reference, player);
                        handleInvalidStacks(cosmeticStacks, reference, player);
                    }
                } else {
                    // TODO: DROP CONTAINER!
                    var stacks = container.getAccessories();
                    var cosmeticStacks = container.getCosmeticAccessories();

                    for (int i = 0; i < container.getSize(); i++) {
                        var reference = new SlotReference(container.getSlotName(), player, i);

                        dropAndRemoveStack(stacks, reference, player);
                        dropAndRemoveStack(cosmeticStacks, reference, player);
                    }
                }
            }
        }

        dataReloadOccured = false;
    }

    private static void handleInvalidStacks(Container container, SlotReference reference, ServerPlayer player){
        var bl = AccessoriesAccess.getAPI()
                .canInsertIntoSlot(player, reference, container.getItem(reference.slot()));

        if(!bl) dropAndRemoveStack(container, reference, player);
    }

    private static void dropAndRemoveStack(Container container, SlotReference reference, ServerPlayer player){
        var stack = container.getItem(reference.slot());

        container.setItem(reference.slot(), ItemStack.EMPTY);
        AccessoriesAccess.getInternal().giveItemToPlayer(player, stack);
    }

    public static void entityLoad(LivingEntity entity, Level level){
        if(!level.isClientSide() || !(entity instanceof ServerPlayer serverPlayer)) return;

        AccessoriesAccess.getAPI()
                .getCapability(serverPlayer)
                .ifPresent(capability -> {
                    var holder = AccessoriesAccess.getHolder(capability.getEntity());

                    var tag = new CompoundTag();

                    holder.write(tag);

                    AccessoriesAccess.getHandler().sendToTrackingAndSelf(serverPlayer, new SyncContainer(tag, capability.getEntity().getId()));
                });
    }

    public static void onTracking(LivingEntity entity, ServerPlayer player){
        AccessoriesAccess.getAPI()
                .getCapability(entity)
                .ifPresent(capability -> {
                    var holder = AccessoriesAccess.getHolder(capability.getEntity());

                    var tag = new CompoundTag();

                    holder.write(tag);

                    AccessoriesAccess.getHandler().sendToPlayer(player, new SyncContainer(tag, capability.getEntity().getId()));
                });
    }

    public static void dataSync(@Nullable PlayerList list, @Nullable ServerPlayer player){
        var api = AccessoriesAccess.getAPI();

        var networkHandler = AccessoriesAccess.getHandler();
        var syncPacket = SyncData.create();

        if(list != null){
            var buf = AccessoriesNetworkHandler.createBuf();

            syncPacket.readPacket(buf);

            for (var player1 : list.getPlayers()) {
                networkHandler.sendToPlayer(player1, new SyncData(buf));

                api.getCapability(player1).ifPresent(capability -> {
                    var holder = AccessoriesAccess.getHolder(capability.getEntity());

                    var tag = new CompoundTag();

                    holder.write(tag);

                    networkHandler.sendToTrackingAndSelf(player1, new SyncContainer(tag, capability.getEntity().getId()));
                });
            }

            buf.release();

            //--
            //TODO: HANDLE SCREEN STUFF??
        } else if(player != null){
            networkHandler.sendToPlayer(player, syncPacket);

            api.getCapability(player).ifPresent(capability -> {
                var holder = AccessoriesAccess.getHolder(capability.getEntity());

                var tag = new CompoundTag();

                holder.write(tag);

                networkHandler.sendToTrackingAndSelf(player, new SyncContainer(tag, capability.getEntity().getId()));
            });
            //--
            //TODO: HANDLE SCREEN STUFF??
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
            var slotType = container.slotType().get();

            var accessories = (ExpandedSimpleContainer) container.getAccessories();

            for (int i = 0; i < accessories.getContainerSize(); i++) {
                var slotReference = new SlotReference(container.getSlotName(), capability.getEntity(), i);

                var slotId = AccessoriesAPI.slottedId(slotType, i);

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
                        var uuid = api.getOrCreateSlotUUID(slotType, i);

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
                        cosmetics.setPreviousItem(i, currentCosmeticStack.copy());
                        dirtyCosmeticStacks.put(slotId, currentCosmeticStack.copy());
                    }
                }
            }

            if(!entity.level().isClientSide()) {
                Set<AccessoriesContainer> updatedContainers = capability.getUpdatingInventories();

                if(!dirtyStacks.isEmpty() || !dirtyCosmeticStacks.isEmpty() || !updatedContainers.isEmpty()) {
                    var packet = new SyncContainerData(entity.getId(), updatedContainers, dirtyStacks, dirtyCosmeticStacks);

                    var bufData = AccessoriesNetworkHandler.createBuf();

                    packet.write(bufData);

                    var networkHandler = AccessoriesAccess.getHandler();

                    networkHandler.sendToTrackingAndSelf(entity, (Supplier<SyncContainerData>) () -> new SyncContainerData(bufData));

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
            var slotTypesList = List.copyOf(slotTypes);

            for (int i = 0; i < slotTypesList.size(); i++) {
                var type = slotTypesList.get(i);

                slotsComponent.append(Component.translatable(type.translation()));

                if(i + 1 != slotTypesList.size()){
                    slotInfoComponent.append(",");
                }
            }
        }

        var slotTranslationKey = "slot.tooltip." + ((slotTypes.size() > 1 && !allSlots) ? "plural" : "singular");

        slotInfoComponent.append(
                Component.translatable(Accessories.translation(slotTranslationKey))
                        .withStyle(ChatFormatting.GRAY)
                        .append(slotsComponent.withStyle(ChatFormatting.BLUE))
        );

        tooltip.add(slotInfoComponent);

        Map<SlotType, Multimap<Attribute, AttributeModifier>> slotSpecificModifiers = new HashMap<>();
        Multimap<Attribute, AttributeModifier> defaultModifiers = null;

        boolean allDuplicates = true;

        for (SlotType slotType : slotTypes) {
            var reference = new SlotReference(slotType.name(), entity, 0);
            var uuid = api.getOrCreateSlotUUID(slotType, 0);

            var slotModifiers = accessory.get().getModifiers(stack, reference, uuid);

            slotSpecificModifiers.put(slotType, slotModifiers);

            if(defaultModifiers == null){
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
            attributeTooltip.add(
                    Component.translatable(Accessories.translation("tooltip.attributes.any"))
                            .withStyle(ChatFormatting.GRAY)
            );
            addAttributeTooltip(defaultModifiers, attributeTooltip);

            slotTypeToTooltipInfo.put(null, attributeTooltip);
        } else {
            for (var slotModifiers : slotSpecificModifiers.entrySet()) {
                tooltip.add(
                        Component.translatable(Accessories.translation("tooltip.attributes.slot"))
                                .withStyle(ChatFormatting.GRAY)
                );
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
                defaultExtraAttributeTooltip = extraAttributeTooltip;
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
                tooltip.add(
                        Component.translatable(Accessories.translation("tooltip.attributes.slot"), Component.translatable(entry.getKey().translation()))
                                .withStyle(ChatFormatting.GRAY)
                );
                tooltip.addAll(entry.getValue());
            }
        } else {
            var anyTooltipInfo = slotTypeToTooltipInfo.get(null);

            if(anyTooltipInfo.size() > 1) {
                tooltip.addAll(anyTooltipInfo);
            }
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

    public static void onDeath(LivingEntity entity, DamageSource damageSource){
        var api = AccessoriesAccess.getAPI();

        var capability = api.getCapability(entity);

        if(capability.isEmpty()) return;

        var shouldDrop = AccessoriesEvents.ON_DEATH_EVENT.invoker().shouldDrop(entity, capability.get());

        if(!shouldDrop) return;

        for (var containerEntry : capability.get().getContainers().entrySet()) {
            var slotType = containerEntry.getValue().slotType();

            var slotDropRule = slotType.map(SlotType::dropRule).orElse(SlotType.DropRule.DEFAULT);

            var container = containerEntry.getValue();

            var stacks = container.getAccessories();
            var cosmeticStacks = container.getCosmeticAccessories();

            for (int i = 0; i < container.getSize(); i++) {
                var reference = new SlotReference(container.getSlotName(), entity, i);

                dropStack(slotDropRule, entity, stacks, reference);
                dropStack(slotDropRule, entity, cosmeticStacks, reference);
            }
        }
    }

    private static void dropStack(SlotType.DropRule dropRule, LivingEntity entity, Container container, SlotReference reference){
        var api = AccessoriesAccess.getAPI();

        var stack = container.getItem(reference.slot());
        var accessory = api.getAccessory(stack);

        if(accessory.isPresent() && dropRule == SlotType.DropRule.DEFAULT) {
            dropRule = accessory.get().getDropRule(stack, reference);
        }

        dropRule = AccessoriesEvents.ON_DROP_EVENT.invoker().onDrop(dropRule, entity, reference, stack);

        boolean dropStack = true;

        if(dropRule == SlotType.DropRule.DESTROY){
            container.setItem(reference.slot(), ItemStack.EMPTY);
            dropStack = false;
            // TODO: Do we call break here for the accessory?
        } else if(dropRule == SlotType.DropRule.DEFAULT){
            if(entity.level().getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).get()) {
                dropStack = true;
            } else if(EnchantmentHelper.hasVanishingCurse(stack)){
                container.setItem(reference.slot(), ItemStack.EMPTY);
                dropStack = false;
                // TODO: Do we call break here for the accessory?
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
