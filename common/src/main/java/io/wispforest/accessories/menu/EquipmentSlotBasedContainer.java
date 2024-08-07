package io.wispforest.accessories.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface EquipmentSlotBasedContainer extends Container {

    static Container of(EquipmentSlot equipmentSlot, LivingEntity livingEntity) {
        if(livingEntity instanceof Player player) {
            return ofPlayer(equipmentSlot, player);
        } else {
            return ofLiving(equipmentSlot, livingEntity);
        }
    }

    static Container ofLiving(EquipmentSlot equipmentSlot, LivingEntity livingEntity) {
        var slotAccess = SlotAccess.of(
                () -> livingEntity.getItemBySlot(equipmentSlot),
                stack -> livingEntity.setItemSlot(equipmentSlot, stack)
        );

        return new SlotAccessContainer(slotAccess);
    }

    static Container ofPlayer(EquipmentSlot equipmentSlot, Player player) {
        var slotAccess = SlotAccess.of(
                () -> {
                    var inv = player.getInventory();

                    var index = switch (equipmentSlot) {
                        case HEAD -> 3;
                        case CHEST -> 2;
                        case LEGS -> 1;
                        case FEET -> 0;
                        default -> -1;
                    };

                    if(index == -1) return ItemStack.EMPTY;

                    return inv.getArmor(index);
                },
                stack -> {
                    var inv = player.getInventory();

                    var index = switch (equipmentSlot) {
                        case HEAD -> 0;
                        case CHEST -> 1;
                        case LEGS -> 2;
                        case FEET -> 3;
                        default -> -1;
                    };

                    if(index == -1) return;


                    inv.setItem(39 - index, stack);
                }
        );

        return new SlotAccessContainer(slotAccess);
    }

    EquipmentSlot equipmentSlot();

    ItemStack getEquipmentStack(EquipmentSlot equipmentSlot);

    void setEquipmentStack(EquipmentSlot equipmentSlot, ItemStack stack);

    @Override
    default int getContainerSize() {
        return 1;
    }

    @Override
    default boolean isEmpty() {
        return getItem(0).isEmpty();
    }

    @Override
    default ItemStack getItem(int slot) {
        return this.getEquipmentStack(equipmentSlot());
    }

    @Override
    default ItemStack removeItem(int slot, int amount) {
        var stack = this.getItem(0).copy();

        var removedStack = stack.split(amount);

        this.setEquipmentStack(equipmentSlot(), stack);

        return removedStack;
    }

    @Override
    default ItemStack removeItemNoUpdate(int slot) {
        var stack = this.getItem(0).copy();

        this.setEquipmentStack(equipmentSlot(), ItemStack.EMPTY);

        return stack;
    }

    @Override
    default void setItem(int slot, ItemStack stack) {
        this.setEquipmentStack(equipmentSlot(), stack);
    }

    @Override
    default void setChanged() {}

    @Override
    default boolean stillValid(Player player) {
        return true;
    }

    @Override
    default void clearContent() {}
}
