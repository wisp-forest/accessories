package io.wispforest.accessories.menu;

import io.wispforest.accessories.pond.ItemBasedSteerable;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;

public final class SlotAccessContainer implements Container {

    private final SlotAccess slotAccess;

    public SlotAccessContainer(SlotAccess slotAccess) {
        this.slotAccess = slotAccess;
    }

    public static Container ofArmor(EquipmentSlot equipmentSlot, LivingEntity livingEntity) {
        if(livingEntity instanceof Player player) {
            return ofPlayerArmor(equipmentSlot, player);
        } else {
            return ofGenericArmor(equipmentSlot, livingEntity);
        }
    }

    public static Container ofGenericArmor(EquipmentSlot equipmentSlot, LivingEntity livingEntity) {
        return new SlotAccessContainer(SlotAccess.forEquipmentSlot(livingEntity, equipmentSlot));
    }

    @Nullable
    public static Container ofPlayerArmor(EquipmentSlot equipmentSlot, Player player) {
        var index = 39 - switch (equipmentSlot) {
            case HEAD -> 0;
            case CHEST -> 1;
            case LEGS -> 2;
            case FEET -> 3;
            default -> -1;
        };

        if(index == 40) return null;

        return new SlotAccessContainer(SlotAccess.forContainer(player.getInventory(), index));
    }

    //--

    @Nullable
    public static Container ofSaddleSlot(LivingEntity living) {
        if(living instanceof AbstractHorse abstractHorse) {
            return ofHorseSaddle(abstractHorse);
        } else if(living instanceof ItemBasedSteerable saddleable) {
            return ofGenericSaddle(saddleable);
        }

        return null;
    }

    public static Container ofHorseSaddle(AbstractHorse abstractHorse) {
        return new SlotAccessContainer(abstractHorse.getSlot(400));
    }

    public static Container ofGenericSaddle(ItemBasedSteerable saddleable) {
        return new SlotAccessContainer(
                SlotAccess.of(
                        () -> saddleable.isSaddled() ? Items.SADDLE.getDefaultInstance() : ItemStack.EMPTY,
                        stack -> {
                            if(stack.isEmpty()) {
                                saddleable.getInstance().setSaddle(false);
                            } else {
                                saddleable.equipSaddle(stack, null);
                            }
                        }
                )
        );
    }

    //--

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return getItem(0).isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.slotAccess.get();
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        var stack = this.getItem(0).copy();

        var removedStack = stack.split(amount);

        this.slotAccess.set(stack);

        return removedStack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        var stack = this.getItem(0).copy();

        this.slotAccess.set(ItemStack.EMPTY);

        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.slotAccess.set(stack);
    }

    @Override public void setChanged() {}
    @Override public boolean stillValid(Player player) { return true; }
    @Override public void clearContent() {}
}
