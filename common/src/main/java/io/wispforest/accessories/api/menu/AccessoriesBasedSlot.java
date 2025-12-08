package io.wispforest.accessories.api.menu;

import com.mojang.logging.LogUtils;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.api.core.AccessoryRegistry;
import io.wispforest.accessories.api.events.AllowEntityModificationCallback;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.api.slot.validator.SlotValidatorRegistry;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.impl.core.ExpandedContainer;
import io.wispforest.accessories.menu.SlotTypeAccessible;
import io.wispforest.accessories.pond.AccessoriesLivingEntityExtension;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Base slot class implementation for Accessories with static methods that force checks if
 * the passed entity and type can be found. Primarily used with internal screen and
 * with the {@link AccessoriesSlotGenerator} for unique slots API
 */
public class AccessoriesBasedSlot extends Slot implements SlotTypeAccessible {

    private static final Logger LOGGER = LogUtils.getLogger();

    public final LivingEntity entity;
    public final AccessoriesContainer accessoriesContainer;
    public final boolean isCosmetic;

    public AccessoriesBasedSlot(AccessoriesContainer accessoriesContainer, ExpandedContainer container, int slot, int x, int y) {
        this(accessoriesContainer, container, accessoriesContainer.getCosmeticAccessories() == container, slot, x, y);
    }

    private AccessoriesBasedSlot(AccessoriesContainer accessoriesContainer, ExpandedContainer container, boolean isCosmetic, int slot, int x, int y) {
        super(container, slot, x, y);

        this.accessoriesContainer = accessoriesContainer;
        this.entity = accessoriesContainer.capability().entity();
        this.isCosmetic = isCosmetic;
    }

    @Nullable
    @Deprecated(forRemoval = true)
    public static AccessoriesBasedSlot of(LivingEntity livingEntity, SlotType slotType, int x, int y) {
        return of(livingEntity, slotType, 0, x, y);
    }

    @Nullable
    public static AccessoriesBasedSlot of(LivingEntity livingEntity, SlotType slotType, int slot, int x, int y) {
        return of(livingEntity, slotType, false, slot, x, y);
    }

    @Nullable
    public static AccessoriesBasedSlot of(LivingEntity livingEntity, SlotType slotType, boolean isCosmetic, int slot, int x, int y) {
        var capability = livingEntity.accessoriesCapability();

        if(capability == null) {
            LOGGER.error("Unable to locate a capability for the given livingEntity meaning it does not have a valid Accessory Inventory [EntityType: {}]", livingEntity.getType());

            return null;
        }

        var validEntitySlots = EntitySlotLoader.getEntitySlots(livingEntity);

        if(!validEntitySlots.containsKey(slotType.name())) {
            LOGGER.error("Unable to create Accessory Slot due to the given LivingEntity not having the given SlotType bound to it! [EntityType: {}, SlotType: {}]", livingEntity.getType(), slotType.name());

            return null;
        }

        var container = capability.getContainer(slotType);

        if(container == null){
            LOGGER.error("Unable to locate the given container for the passed slotType. [SlotType:{}]", slotType.name());

            return null;
        }

        return new AccessoriesBasedSlot(container, isCosmetic ? container.getAccessories() : container.getCosmeticAccessories(), slot, x, y);
    }

    @Override
    public AccessoriesContainer getContainer() {
        return accessoriesContainer;
    }

    @Override
    public int index() {
        return this.index;
    }

    @Override
    @Deprecated
    public int getMaxStackSize() {
        // TODO: API TO LIMIT IDK
        return super.getMaxStackSize();
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        var accessory = AccessoryRegistry.getAccessoryOrDefault(stack);

        return accessory.maxStackSize(stack);
    }

    @Override
    public void set(ItemStack stack) {
        super.set(stack);
    }

    @Override
    public void setByPlayer(ItemStack newStack, ItemStack oldStack) {
        ((AccessoriesLivingEntityExtension) this.entity).onEquipItem(accessoriesContainer.createReference(this.getContainerSlot()), oldStack, newStack);

        super.setByPlayer(newStack, oldStack);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        // Cosmetic slots do not run the canEquip for the given accessory stack
        if (this.isCosmeticSlot()) {
            var slotType = this.accessoriesContainer.slotType();

            return SlotValidatorRegistry.getPredicateResults(slotType.validators(), this.entity.level(), this.entity, slotType, this.getContainerSlot(), stack);
        }

        return SlotValidatorRegistry.canInsertIntoSlot(stack, SlotReference.of(this.entity, this.accessoriesContainer.getSlotName(), this.getContainerSlot()));
    }

    @Override
    public boolean mayPickup(Player player) {
        if(!this.entity.equals(player)/*this.entity != player*/) {
            var ref = this.accessoriesContainer.createReference(this.getContainerSlot());

            var result = AllowEntityModificationCallback.EVENT.invoker().allowModifications(this.entity, player, ref);

            if(!result.orElse(false)) return false;
        }

        // Cosmetic slots do not run the canUnequip for the given accessory stack
        return isCosmetic || AccessoryRegistry.canUnequip(this.getItem(), SlotReference.of(this.entity, this.accessoriesContainer.getSlotName(), this.getContainerSlot()));
    }

    @Override
    public ResourceLocation getNoItemIcon(){
        var slotType = this.accessoriesContainer.slotType();

        return slotType != null ? slotType.icon() : SlotType.EMPTY_SLOT_ICON;
    }

    public List<Component> getTooltipData() {
        var tooltipData = new ArrayList<Component>();

        var slotType = this.accessoriesContainer.slotType();

        tooltipData.add(Component.translatable(Accessories.translationKey( "slot.tooltip.singular"))
                .withStyle(ChatFormatting.GRAY)
                .append(Component.translatable(slotType.translation()).withStyle(ChatFormatting.BLUE)));

        return tooltipData;
    }
}
