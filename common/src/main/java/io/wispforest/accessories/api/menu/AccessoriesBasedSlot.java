package io.wispforest.accessories.api.menu;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.api.events.AccessoriesEvents;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.impl.ExpandedSimpleContainer;
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
import java.util.Optional;

/**
 * Base slot class implementation for Accessories with static methods that force checks if
 * such passed entity and type can be found. Primarily used with internal screen and such
 * within {@link AccessoriesSlotGenerator} for unique slots API
 */
public class AccessoriesBasedSlot extends Slot {

    private static final Logger LOGGER = LogUtils.getLogger();

    public final LivingEntity entity;
    public final AccessoriesContainer accessoriesContainer;

    public AccessoriesBasedSlot(AccessoriesContainer accessoriesContainer, ExpandedSimpleContainer container, int slot, int x, int y) {
        super(container, slot, x, y);

        this.accessoriesContainer = accessoriesContainer;
        this.entity = accessoriesContainer.capability().entity();
    }

    @Nullable
    public static AccessoriesBasedSlot of(LivingEntity livingEntity, SlotType slotType, int x, int y) {
        return of(livingEntity, slotType, 0, x, y);
    }

    @Nullable
    public static AccessoriesBasedSlot of(LivingEntity livingEntity, SlotType slotType, int slot, int x, int y) {
        var capability = livingEntity.accessoriesCapability();

        if(capability == null) {
            LOGGER.error("Unable to locate a capability for the given livingEntity meaning such dose not have a valid Accessory Inventory [EntityType: " + livingEntity.getType() + "]");

            return null;
        }

        var validEntitySlots = EntitySlotLoader.getEntitySlots(livingEntity);

        if(!validEntitySlots.containsKey(slotType.name())) {
            LOGGER.error("Unable to create Accessory Slot due to the given LivingEntity not having the given SlotType bound to such! [EntityType: " + livingEntity.getType() + ", SlotType: " + slotType.name() + "]");

            return null;
        }

        var container = capability.getContainer(slotType);

        if(container == null){
            LOGGER.error("Unable to locate the given container for the passed slotType. [SlotType:" + slotType.name() + "]");

            return null;
        }

        return new AccessoriesBasedSlot(container, container.getAccessories(), slot, x, y);
    }

    @Override
    @Deprecated
    public int getMaxStackSize() {
        // TODO: API TO LIMIT IDK
        return super.getMaxStackSize();
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        var accessory = AccessoriesAPI.getOrDefaultAccessory(stack);

        return accessory.maxStackSize(stack);
    }

    @Override
    public void set(ItemStack stack) {
        super.set(stack);

        this.accessoriesContainer.markChanged();
        this.accessoriesContainer.update();
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return AccessoriesAPI.canInsertIntoSlot(stack, SlotReference.of(this.entity, this.accessoriesContainer.getSlotName(), this.getContainerSlot()));
    }

    @Override
    public boolean mayPickup(Player player) {
        if(!this.entity.equals(player)/*this.entity != player*/) {
            var ref = this.accessoriesContainer.createReference(this.getContainerSlot());

            var result = AccessoriesEvents.ENTITY_MODIFICATION_CHECK.invoker().checkModifiability(this.entity, player, ref);

            if(!result.orElse(false)) return false;
        }

        return AccessoriesAPI.canUnequip(this.getItem(), SlotReference.of(this.entity, this.accessoriesContainer.getSlotName(), this.getContainerSlot()));
    }

    protected ResourceLocation icon(){
        var slotType = this.accessoriesContainer.slotType();

        return slotType != null ? slotType.icon() : SlotType.EMPTY_SLOT_LOCATION;
    }

    public List<Component> getTooltipData() {
        var tooltipData = new ArrayList<Component>();

        var slotType = this.accessoriesContainer.slotType();

        tooltipData.add(Component.translatable(Accessories.translation( "slot.tooltip.singular"))
                .withStyle(ChatFormatting.GRAY)
                .append(Component.translatable(slotType.translation()).withStyle(ChatFormatting.BLUE)));

        return tooltipData;
    }

    @Nullable
    @Override
    public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
        // Thanks to mojang you can not access the GUI atlas from this call and you must use Atlases from ModelManager.
        // )::::::::::::::::::::::::::::::

        return new Pair<>(new ResourceLocation("textures/atlas/blocks.png"), icon());
    }
}
