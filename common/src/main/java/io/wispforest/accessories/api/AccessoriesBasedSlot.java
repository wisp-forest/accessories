package io.wispforest.accessories.api;

import com.mojang.datafixers.util.Pair;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.impl.ExpandedSimpleContainer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;


public class AccessoriesBasedSlot extends Slot {

    public final LivingEntity entity;
    public final AccessoriesContainer accessoriesContainer;

    protected AccessoriesBasedSlot(AccessoriesContainer accessoriesContainer, ExpandedSimpleContainer container, int slot, int x, int y) {
        super(container, slot, x, y);

        this.accessoriesContainer = accessoriesContainer;
        this.entity = accessoriesContainer.capability().getEntity();
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
