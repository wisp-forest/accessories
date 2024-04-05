package io.wispforest.accessories.impl;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.api.*;
import io.wispforest.accessories.api.events.AccessoriesEvents;
import io.wispforest.accessories.api.slot.SlotAttribute;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.client.AccessoriesMenu;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.accessories.networking.AccessoriesNetworkHandler;
import io.wispforest.accessories.networking.client.SyncEntireContainer;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
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
import java.util.stream.Collectors;

import static io.wispforest.accessories.Accessories.ACCESSORY_EQUIPPED;
import static io.wispforest.accessories.Accessories.ACCESSORY_UNEQUIPPED;

@ApiStatus.Internal
public class AccessoriesEventHandler {

    public static boolean dataReloadOccured = false;

    public static void onWorldTick(Level level) {
        if (!dataReloadOccured || !(level instanceof ServerLevel serverLevel)) return;

        for (var player : serverLevel.getServer().getPlayerList().getPlayers()) {
            var capability = AccessoriesCapability.get(player);

            if (capability == null) continue;

            var validSlotTypes = EntitySlotLoader.getEntitySlots(player).values();

            for (var container : capability.getContainers().values()) {
                var slotType = container.slotType();

                if (slotType != null && validSlotTypes.contains(slotType)) {
                    var baseSize = ((AccessoriesContainerImpl) container).getBaseSize();

                    if (baseSize != slotType.amount()) {
                        container.markChanged();
                        container.update();
                    }

                    var stacks = container.getAccessories();
                    var cosmeticStacks = container.getCosmeticAccessories();

                    for (int i = 0; i < container.getSize(); i++) {
                        var reference = new SlotReference(container.getSlotName(), player, i);

                        handleInvalidStacks(stacks, reference, player);
                        handleInvalidStacks(cosmeticStacks, reference, player);
                    }
                } else {
                    // TODO: DROP CONTAINER ?!
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

    private static void handleInvalidStacks(Container container, SlotReference reference, ServerPlayer player) {
        var bl = AccessoriesAPI.canInsertIntoSlot(container.getItem(reference.slot()), reference);

        if (!bl) dropAndRemoveStack(container, reference, player);
    }

    private static void dropAndRemoveStack(Container container, SlotReference reference, ServerPlayer player) {
        var stack = container.getItem(reference.slot());

        container.setItem(reference.slot(), ItemStack.EMPTY);

        AccessoriesInternals.giveItemToPlayer(player, stack);
    }

    public static void entityLoad(LivingEntity entity, Level level) {
        if (!level.isClientSide() || !(entity instanceof ServerPlayer serverPlayer)) return;

        var capability = AccessoriesCapability.get(serverPlayer);

        if(capability == null) return;

        var tag = new CompoundTag();

        ((AccessoriesHolderImpl) capability.getHolder()).write(tag);

        AccessoriesInternals.getNetworkHandler().sendToTrackingAndSelf(serverPlayer, new SyncEntireContainer(tag, capability.getEntity().getId()));
    }

    public static void onTracking(LivingEntity entity, ServerPlayer serverPlayer) {
        var capability = AccessoriesCapability.get(serverPlayer);

        if(capability == null) return;

        var tag = new CompoundTag();

        ((AccessoriesHolderImpl) capability.getHolder()).write(tag);

        AccessoriesInternals.getNetworkHandler().sendToPlayer(serverPlayer, new SyncEntireContainer(tag, capability.getEntity().getId()));
    }

    public static void dataSync(@Nullable PlayerList list, @Nullable ServerPlayer player) {
        var networkHandler = AccessoriesInternals.getNetworkHandler();
        var syncPacket = SyncData.create();

        if (list != null && !list.getPlayers().isEmpty()) {
            var buf = AccessoriesNetworkHandler.createBuf();

            syncPacket.write(buf);

            for (var playerEntry : list.getPlayers()) {
                networkHandler.sendToPlayer(playerEntry, new SyncData(buf));

                var capability = AccessoriesCapability.get(playerEntry);

                if(capability == null) return;

                var tag = new CompoundTag();

                ((AccessoriesHolderImpl) capability.getHolder()).write(tag);

                networkHandler.sendToTrackingAndSelf(playerEntry, new SyncEntireContainer(tag, capability.getEntity().getId()));

                if(playerEntry.containerMenu instanceof AccessoriesMenu) {
                    player.openMenu(new SimpleMenuProvider((i, inventory, player1) -> new AccessoriesMenu(i, inventory, true, player1), Component.empty()));
                }
            }

            buf.release();

        } else if (player != null) {
            networkHandler.sendToPlayer(player, syncPacket);

            var capability = AccessoriesCapability.get(player);

            if(capability == null) return;

            var tag = new CompoundTag();

            ((AccessoriesHolderImpl) capability.getHolder()).write(tag);

            networkHandler.sendToPlayer(player, new SyncEntireContainer(tag, capability.getEntity().getId()));

            if(player.containerMenu instanceof AccessoriesMenu) {
                player.openMenu(new SimpleMenuProvider((i, inventory, player1) -> new AccessoriesMenu(i, inventory, true, player1), Component.empty()));
            }
        }
    }

    public static void onLivingEntityTick(LivingEntity entity) {
        if(entity.isRemoved()) return;

        var capability = AccessoriesCapability.get(entity);

        if (capability == null) return;

        var dirtyStacks = new HashMap<String, ItemStack>();
        var dirtyCosmeticStacks = new HashMap<String, ItemStack>();

        for (var containerEntry : capability.getContainers().entrySet()) {
            var container = containerEntry.getValue();
            var slotType = container.slotType();

            var accessories = (ExpandedSimpleContainer) container.getAccessories();

            for (int i = 0; i < accessories.getContainerSize(); i++) {
                var slotReference = new SlotReference(container.getSlotName(), capability.getEntity(), i);

                var slotId = slotType.name() + "/" + i;

                var currentStack = accessories.getItem(i);

                // TODO: Move ticking below checks?
                if (!currentStack.isEmpty()) {
                    // TODO: Document this behavior to prevent double ticking maybe!!!
                    currentStack.inventoryTick(entity.level(), entity, -1, false);

                    var accessory = AccessoriesAPI.getAccessory(currentStack);

                    if(accessory != null) accessory.tick(currentStack, slotReference);
                }

                var lastStack = accessories.getPreviousItem(i);

                if (!ItemStack.matches(currentStack, lastStack)) {
                    if (!entity.level().isClientSide()) {
                        accessories.setPreviousItem(i, currentStack.copy());
                        dirtyStacks.put(slotId, currentStack.copy());
                        var uuid = AccessoriesAPI.getOrCreateSlotUUID(slotType, i);

                        if (!lastStack.isEmpty()) {
                            Multimap<Attribute, AttributeModifier> attributes = AccessoriesAPI.getAttributeModifiers(lastStack, slotReference, uuid);
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
                            Multimap<Attribute, AttributeModifier> attributes = AccessoriesAPI.getAttributeModifiers(currentStack, slotReference, uuid);
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

                    /*
                     * TODO: Dose item check need to exist anymore?
                     */
                    if (!ItemStack.isSameItem(currentStack, lastStack) || accessories.isSlotFlaged(i)) {
                        AccessoriesAPI.getOrDefaultAccessory(lastStack.getItem()).onUnequip(lastStack, slotReference);
                        AccessoriesAPI.getOrDefaultAccessory(currentStack.getItem()).onEquip(currentStack, slotReference);

                        if (entity instanceof ServerPlayer serverPlayer) {
                            if (!currentStack.isEmpty()) {
                                ACCESSORY_EQUIPPED.trigger(serverPlayer, currentStack, slotReference, false);
                            }
                            if (!lastStack.isEmpty()) {
                                ACCESSORY_UNEQUIPPED.trigger(serverPlayer, lastStack, slotReference, false);
                            }
                        }
                    }
                }

                var cosmetics = container.getCosmeticAccessories();

                var currentCosmeticStack = cosmetics.getItem(i);
                var lastCosmeticStack = cosmetics.getPreviousItem(i);

                if (!ItemStack.matches(currentCosmeticStack, lastCosmeticStack)) {
                    if (!entity.level().isClientSide()) {
                        cosmetics.setPreviousItem(i, currentCosmeticStack.copy());
                        dirtyCosmeticStacks.put(slotId, currentCosmeticStack.copy());

                        if (entity instanceof ServerPlayer serverPlayer) {
                            if (!currentStack.isEmpty()) {
                                ACCESSORY_EQUIPPED.trigger(serverPlayer, currentStack, slotReference, true);
                            }
                            if (!lastStack.isEmpty()) {
                                ACCESSORY_UNEQUIPPED.trigger(serverPlayer, lastStack, slotReference, true);
                            }
                        }
                    }
                }
            }
        }

        if (!entity.level().isClientSide()) {
            Set<AccessoriesContainer> updatedContainers = ((AccessoriesCapabilityImpl)capability).getUpdatingInventories();

            if (!dirtyStacks.isEmpty() || !dirtyCosmeticStacks.isEmpty() || !updatedContainers.isEmpty()) {
                var packet = new SyncContainerData(entity.getId(), updatedContainers, dirtyStacks, dirtyCosmeticStacks);

                var bufData = AccessoriesNetworkHandler.createBuf();

                packet.write(bufData);

                var networkHandler = AccessoriesInternals.getNetworkHandler();

                networkHandler.sendToTrackingAndSelf(entity, (Supplier<SyncContainerData>) () -> new SyncContainerData(bufData));

                bufData.release();
            }

            updatedContainers.clear();
        }

        if (!entity.level().isClientSide()) {
            var invalidStacks = ((AccessoriesHolderImpl) capability.getHolder()).invalidStacks;

            for (ItemStack invalidStack : invalidStacks) {
                if (entity instanceof ServerPlayer serverPlayer) {
                    AccessoriesInternals.giveItemToPlayer(serverPlayer, invalidStack);
                }
            }

            invalidStacks.clear();
        }
    }

    public static void addTooltipInfo(LivingEntity entity, ItemStack stack, List<Component> tooltip) {
        var accessory = AccessoriesAPI.getOrDefaultAccessory(stack);

        if (accessory == null) return;

        // TODO: MAYBE DEPENDING ON ENTITY OR SOMETHING SHOW ALL VALID SLOTS BUT COLOR CODE THEM IF NOT VALID FOR ENTITY?
        var slotTypes = new HashSet<>(AccessoriesAPI.getValidSlotTypes(entity, stack));
        var allSlotTypes = SlotTypeLoader.getSlotTypes(entity.level()).values()
                .stream()
                .filter(slotType -> slotType.amount() > 0 && !slotType.uniqueSlot())
                .collect(Collectors.toSet());

        if (slotTypes.isEmpty()) return;

        var capability = AccessoriesCapability.get(entity);

        if (capability == null) return;

        var slotInfoComponent = Component.literal("");

        var slotsComponent = Component.literal("");
        boolean allSlots = false;

        if (slotTypes.containsAll(allSlotTypes)) {
            slotsComponent.append(Component.translatable(Accessories.translation("slot.any")));
            allSlots = true;
        } else {
            var slotTypesList = List.copyOf(slotTypes);

            for (int i = 0; i < slotTypesList.size(); i++) {
                var type = slotTypesList.get(i);

                slotsComponent.append(Component.translatable(type.translation()));

                if (i + 1 != slotTypesList.size()) {
                    slotsComponent.append(Component.literal(", ").withStyle(ChatFormatting.GRAY));
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
            var uuid = AccessoriesAPI.getOrCreateSlotUUID(slotType, 0);

            var slotModifiers = AccessoriesAPI.getAttributeModifiers(stack, reference, uuid);

            slotSpecificModifiers.put(slotType, slotModifiers);

            if (defaultModifiers == null) {
                defaultModifiers = slotModifiers;
            } else if (allDuplicates) {
                // TODO: ! WARNING ! THIS MAY NOT WORK?
                allDuplicates = defaultModifiers.equals(slotModifiers);
            }
        }

        Map<SlotType, List<Component>> slotTypeToTooltipInfo = new HashMap<>();

        if (allDuplicates) {
            if (!defaultModifiers.isEmpty()) {
                var attributeTooltip = new ArrayList<Component>();

                addAttributeTooltip(defaultModifiers, attributeTooltip);

                slotTypeToTooltipInfo.put(null, attributeTooltip);
            }
        } else {
            for (var slotModifiers : slotSpecificModifiers.entrySet()) {
                var slotType = slotModifiers.getKey();
                var modifiers = slotModifiers.getValue();

                if (modifiers.isEmpty()) continue;

                var attributeTooltip = new ArrayList<Component>();

                addAttributeTooltip(modifiers, attributeTooltip);

                slotTypeToTooltipInfo.put(slotType, attributeTooltip);
            }
        }

        Map<SlotType, List<Component>> extraAttributeTooltips = new HashMap<>();
        List<Component> defaultExtraAttributeTooltip = null;

        boolean allDuplicatesExtras = true;

        for (SlotType slotType : slotTypes) {
            var extraAttributeTooltip = new ArrayList<Component>();
            accessory.getAttributesTooltip(stack, slotType, extraAttributeTooltip);

            extraAttributeTooltips.put(slotType, extraAttributeTooltip);

            if (defaultExtraAttributeTooltip == null) {
                defaultExtraAttributeTooltip = extraAttributeTooltip;
            } else if (allDuplicatesExtras) {
                allDuplicatesExtras = extraAttributeTooltip.equals(defaultExtraAttributeTooltip);
            }
        }

        if (allDuplicatesExtras) {
            slotTypeToTooltipInfo.computeIfAbsent(null, s -> new ArrayList<>())
                    .addAll(defaultExtraAttributeTooltip);
        } else {
            extraAttributeTooltips.forEach((slotType, components) -> {
                slotTypeToTooltipInfo.computeIfAbsent(slotType, s -> new ArrayList<>())
                        .addAll(components);
            });
        }

        if(slotTypeToTooltipInfo.containsKey(null)) {
            var anyTooltipInfo = slotTypeToTooltipInfo.get(null);

            if (anyTooltipInfo.size() > 0) {
                tooltip.add(CommonComponents.EMPTY);

                tooltip.add(
                        Component.translatable(Accessories.translation("tooltip.attributes.any"))
                                .withStyle(ChatFormatting.GRAY)
                );

                tooltip.addAll(anyTooltipInfo);
            }

            slotTypeToTooltipInfo.remove(null);
        }

        if(!slotTypeToTooltipInfo.isEmpty()) {
            for (var entry : slotTypeToTooltipInfo.entrySet()) {
                var tooltipData = entry.getValue();

                if(tooltipData.size() == 0) continue;

                tooltip.add(CommonComponents.EMPTY);

                tooltip.add(
                        Component.translatable(
                                Accessories.translation("tooltip.attributes.slot"),
                                Component.translatable(entry.getKey().translation()).withStyle(ChatFormatting.BLUE)
                        ).withStyle(ChatFormatting.GRAY)
                );

                tooltip.addAll(entry.getValue());
            }
        }

        accessory.getExtraTooltip(stack, tooltip);
    }

    private static void addAttributeTooltip(Multimap<Attribute, AttributeModifier> multimap, List<Component> tooltip) {
        if (multimap.isEmpty()) return;

        for (Map.Entry<Attribute, AttributeModifier> entry : multimap.entries()) {
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

    public static void onDeath(LivingEntity entity, DamageSource source) {
        var capability = AccessoriesCapability.get(entity);

        if (capability == null) return;

        var shouldDrop = AccessoriesEvents.ON_DEATH_EVENT.invoker().shouldDrop(entity, capability);

        if (!shouldDrop.orElse(true)) return;

        for (var containerEntry : capability.getContainers().entrySet()) {
            var slotType = containerEntry.getValue().slotType();

            var slotDropRule = slotType != null ? slotType.dropRule() : DropRule.DEFAULT;

            var container = containerEntry.getValue();

            var stacks = container.getAccessories();
            var cosmeticStacks = container.getCosmeticAccessories();

            for (int i = 0; i < container.getSize(); i++) {
                var reference = new SlotReference(container.getSlotName(), entity, i);

                dropStack(slotDropRule, entity, stacks, reference, source);
                dropStack(slotDropRule, entity, cosmeticStacks, reference, source);
            }
        }
    }

    private static void dropStack(DropRule dropRule, LivingEntity entity, Container container, SlotReference reference, DamageSource source) {
        var stack = container.getItem(reference.slot());
        var accessory = AccessoriesAPI.getAccessory(stack);

        if (accessory != null && dropRule == DropRule.DEFAULT) {
            dropRule = accessory.getDropRule(stack, reference, source);
        }

        if (accessory instanceof AccessoryNest holdable) {
            var dropRules = holdable.getDropRules(stack, reference, source);

            for (int i = 0; i < dropRules.size(); i++) {
                var rulePair = dropRules.get(i);

                var rule = rulePair.left();
                var innerStack = rulePair.right();

                rule = AccessoriesEvents.ON_DROP_EVENT.invoker().onDrop(rule, innerStack, reference);

                var breakInnerStack = (rule == DropRule.DEFAULT && EnchantmentHelper.hasVanishingCurse(innerStack))
                        || (rule == DropRule.DESTROY);

                if (breakInnerStack) {
                    holdable.setInnerStack(stack, i, ItemStack.EMPTY);
                    // TODO: Do we call break here for the accessory?

                    container.setItem(reference.slot(), stack);
                }
            }
        }

        dropRule = AccessoriesEvents.ON_DROP_EVENT.invoker().onDrop(dropRule, stack, reference);

        boolean dropStack = true;

        if (dropRule == DropRule.DESTROY) {
            container.setItem(reference.slot(), ItemStack.EMPTY);
            dropStack = false;
            // TODO: Do we call break here for the accessory?
        } else if (dropRule == DropRule.DEFAULT) {
            if (entity.level().getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).get()) {
                dropStack = true;
            } else if (EnchantmentHelper.hasVanishingCurse(stack)) {
                container.setItem(reference.slot(), ItemStack.EMPTY);
                dropStack = false;
                // TODO: Do we call break here for the accessory?
            }
        } else if (dropRule == DropRule.KEEP) {
            dropStack = false;
        }

        if (!dropStack) return;

        container.setItem(reference.slot(), ItemStack.EMPTY);

        if (entity instanceof Player player) {
            player.drop(stack, true);
        } else {
            entity.spawnAtLocation(stack);
        }
    }

    public static InteractionResultHolder<ItemStack> attemptEquipFromUse(Player player, Level world, InteractionHand hand) {
        var stack = player.getItemInHand(hand);

        if (!stack.isEmpty() && !player.isSpectator() && player.isShiftKeyDown()) {
            var accessory = AccessoriesAPI.getOrDefaultAccessory(stack);

            var capability = AccessoriesCapability.get(player);

            if (capability != null) {
                var unequippedReference = capability.equipAccessory(stack, true, Accessory::canEquipFromUse);

                if (unequippedReference != null) {
                    accessory.onEquipFromUse(stack, unequippedReference.reference());

                    player.setItemInHand(hand, unequippedReference.stack());

                    return InteractionResultHolder.success(unequippedReference.stack());
                }
            }
        }

        return InteractionResultHolder.pass(stack);
    }
}