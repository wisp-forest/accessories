package io.wispforest.accessories.api.menu;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.accessories.impl.ExpandedSimpleContainer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Optional;


public class AccessoriesBasedSlot extends Slot {

    private static final Logger LOGGER = LogUtils.getLogger();

    public final LivingEntity entity;
    public final AccessoriesContainer accessoriesContainer;

    protected AccessoriesBasedSlot(AccessoriesContainer accessoriesContainer, ExpandedSimpleContainer container, int slot, int x, int y) {
        super(container, slot, x, y);

        this.accessoriesContainer = accessoriesContainer;
        this.entity = accessoriesContainer.capability().getEntity();
    }

    @Nullable
    public static AccessoriesBasedSlot of(LivingEntity livingEntity, SlotType slotType, int x, int y) {
        return of(livingEntity, slotType, 0, x, y);
    }

    @Nullable
    public static AccessoriesBasedSlot of(LivingEntity livingEntity, SlotType slotType, int slot, int x, int y) {
        var capability = livingEntity.accessoriesCapability().orElse(null);

        if(capability == null) {
            LOGGER.error("Unable to locate a capability for the given livingEntity meaning such dose not have a valid Accessory Inventory [EntityType: " + livingEntity.getType() + "]");

            return null;
        }

        var validEntitySlots = EntitySlotLoader.getEntitySlots(livingEntity);

        if(!validEntitySlots.containsKey(slotType.name())) {
            LOGGER.error("Unable to create Accessory Slot due to the given LivingEntity not having the given SlotType bound to such! [EntityType: " + livingEntity.getType() + ", SlotType: " + slotType.name() + "]");

            return null;
        }

        var container = capability.tryAndGetContainer(slotType).orElse(null);

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
        return AccessoriesAPI.canInsertIntoSlot(stack, new SlotReference(this.accessoriesContainer.getSlotName(), this.entity, this.getContainerSlot()));
    }

    @Override
    public boolean mayPickup(Player player) {
        return AccessoriesAPI.canUnequip(this.getItem(), new SlotReference(this.accessoriesContainer.getSlotName(), this.entity, this.getContainerSlot()));
    }

    protected ResourceLocation icon(){
        return this.accessoriesContainer.slotType().map(SlotType::icon).orElse(SlotType.EMPTY_SLOT_LOCATION);
    }

    @Nullable
    @Override
    public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
        // Thanks to mojang you can not access the GUI atlas from this call and you must use Atlases from ModelManager.
        // )::::::::::::::::::::::::::::::

        return new Pair<>(new ResourceLocation("textures/atlas/blocks.png"), icon());
    }
}
