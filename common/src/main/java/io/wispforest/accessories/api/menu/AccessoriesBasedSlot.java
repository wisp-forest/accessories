package io.wispforest.accessories.api.menu;

import com.mojang.logging.LogUtils;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.api.action.ActionResponseBuffer;
import io.wispforest.accessories.api.core.AccessoryRegistry;
import io.wispforest.accessories.api.events.v2.AllowEntityModificationCallback;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.api.slot.validator.SlotValidatorRegistry;
import io.wispforest.accessories.api.tooltip.TooltipAdder;
import io.wispforest.accessories.api.tooltip.impl.TooltipEntry;
import io.wispforest.accessories.api.tooltip.TooltipInfoProvider;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.function.Supplier;

/**
 * Base slot class implementation for Accessories with static methods that force checks if
 * the passed entity and type can be found. Primarily used with internal screen and
 * with the {@link AccessoriesSlotGenerator} for unique slots API
 */
public class AccessoriesBasedSlot extends Slot implements SlotTypeAccessible, TooltipInfoProvider<TooltipAdder> {

    private static final Logger LOGGER = LogUtils.getLogger();

    public final LivingEntity entity;
    public final AccessoriesContainer accessoriesContainer;
    public final boolean isCosmetic;

    private Supplier<@Nullable Player> ownerPlayer = () -> null;

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

    public AccessoriesBasedSlot ownerPlayer(Supplier<@Nullable Player> ownerPlayer) {
        this.ownerPlayer = ownerPlayer;

        return this;
    }

    @Override
    public boolean isCosmeticSlot() {
        return this.isCosmetic;
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
        return canEquipSlotResponse(this.isCosmeticSlot(), ownerPlayer.get(), stack, this.slotReference(), new ActionResponseBuffer(true))
            .canPerformAction()
            .isValid();
    }

    @Override
    public boolean mayPickup(Player player) {
        return canUnequipSlotResponse(this.isCosmeticSlot(), player, this.getItem(), this.slotReference(), new ActionResponseBuffer(true))
            .canPerformAction()
            .isValid(true);
    }

    @Override
    public ResourceLocation getNoItemIcon(){
        var slotType = this.accessoriesContainer.slotType();

        return slotType != null ? slotType.icon() : SlotType.EMPTY_SLOT_ICON;
    }

    @Deprecated(forRemoval = true)
    public List<Component> getTooltipData() {
        return TooltipInfoProvider.gatherInfo(this, TooltipEntry.of(), Item.TooltipContext.EMPTY, TooltipFlag.NORMAL).entries();
    }

    @Override
    public void addInfo(TooltipAdder adder, Item.TooltipContext ctx, TooltipFlag type) {
        var slotType = this.accessoriesContainer.slotType();

        adder.add(
            Component.translatable(Accessories.translationKey( "slot.tooltip.singular"))
            .withStyle(ChatFormatting.GRAY)
            .append(Component.translatable(slotType.translation()).withStyle(ChatFormatting.BLUE))
        );
    }

    public ActionResponseBuffer checkInsertion(ItemStack stack) {
        return canEquipSlotResponse(this.isCosmeticSlot(), ownerPlayer.get(), stack, this.slotReference(), new ActionResponseBuffer(false));
    }

    public ActionResponseBuffer checkExtraction() {
        return canUnequipSlotResponse(this.isCosmeticSlot(), ownerPlayer.get(), this.getItem(), this.slotReference(), new ActionResponseBuffer(false));
    }

    public static ActionResponseBuffer canUnequipSlotResponse(boolean isCosmetic, @Nullable Player player, ItemStack stack, SlotReference ref, ActionResponseBuffer buffer){
        var ownerEntity = ref.entity();

        if (stack.isEmpty()) return buffer;

        if (player != null && !ownerEntity.equals(player)) AllowEntityModificationCallback.EVENT.invoker().allowModifications(ownerEntity, player, ref, buffer);
        if (!buffer.shouldReturnEarly() && !isCosmetic) AccessoryRegistry.canUnequipResponse(stack, ref, buffer);

        return buffer;
    }

    public static ActionResponseBuffer canEquipSlotResponse(boolean isCosmetic, @Nullable Player player, ItemStack stack, SlotReference ref, ActionResponseBuffer buffer){
        var ownerEntity = ref.entity();

        if (stack.isEmpty()) return buffer;

        if (player != null && !ownerEntity.equals(player)) AllowEntityModificationCallback.EVENT.invoker().allowModifications(ownerEntity, player, ref, buffer);
        if (!buffer.shouldReturnEarly()) {
            if (isCosmetic) {
                var type = ref.type();
                SlotValidatorRegistry.getPredicateResponse(type.validators(), ownerEntity.level(), ownerEntity, type, ref.index(), stack, buffer);
            } else {
                SlotValidatorRegistry.canInsertIntoSlotResponse(stack, ref, buffer);
            }
        }

        return buffer;
    }
}
