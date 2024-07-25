package io.wispforest.accessories.client;

import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Copy of 1.21 Vanilla class backported to 1.20.1
 */
@ApiStatus.Internal
@Deprecated
public class ArmorSlot extends Slot {
    private final LivingEntity owner;
    private final EquipmentSlot slot;
    @Nullable
    private final ResourceLocation emptyIcon;

    public ArmorSlot(
            Container container, LivingEntity livingEntity, EquipmentSlot equipmentSlot, int i, int j, int k, @Nullable ResourceLocation resourceLocation
    ) {
        super(container, i, j, k);
        this.owner = livingEntity;
        this.slot = equipmentSlot;
        this.emptyIcon = resourceLocation;
    }

    @Override
    public void setByPlayer(ItemStack oldStack) {
        var newItem = this.getItem();
        var equipable = Equipable.get(newItem);

        if (equipable != null) owner.onEquipItem(slot, oldStack, newItem);

        super.setByPlayer(oldStack);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return this.slot == this.owner.getEquipmentSlotForItem(stack);
    }

    @Override
    public boolean mayPickup(Player player) {
        ItemStack itemStack = this.getItem();
        return !itemStack.isEmpty() && !player.isCreative() && EnchantmentHelper.hasBindingCurse(itemStack)
                ? false
                : super.mayPickup(player);
    }

    @Override
    public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
        return this.emptyIcon != null ? Pair.of(InventoryMenu.BLOCK_ATLAS, this.emptyIcon) : super.getNoItemIcon();
    }
}
