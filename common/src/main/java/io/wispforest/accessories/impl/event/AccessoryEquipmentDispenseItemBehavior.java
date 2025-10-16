package io.wispforest.accessories.impl.event;

import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.core.AccessoryRegistry;
import io.wispforest.accessories.api.data.AccessoriesTags;
import io.wispforest.accessories.mixin.DefaultDispenseItemBehaviorAccessor;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.core.dispenser.EquipmentDispenseItemBehavior;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.AABB;

public final class AccessoryEquipmentDispenseItemBehavior extends DefaultDispenseItemBehavior {
    public static final EquipmentDispenseItemBehavior INSTANCE = new EquipmentDispenseItemBehavior();
    private final DispenseItemBehavior wrappedBehavior;

    public AccessoryEquipmentDispenseItemBehavior(DispenseItemBehavior wrappedBehavior) {
        this.wrappedBehavior = wrappedBehavior;
    }

    @Override
    public ItemStack execute(BlockSource blockSource, ItemStack item) {
        if (dispenseEquipment(blockSource, item)) return item;

        if (wrappedBehavior instanceof DefaultDispenseItemBehavior defaultBehavior) {
            return ((DefaultDispenseItemBehaviorAccessor) defaultBehavior).accessories$execute(blockSource, item);
        }

        return this.wrappedBehavior.dispense(blockSource, item);
    }

    public static boolean dispenseEquipment(BlockSource blockSource, ItemStack stack) {
        var blockPos = blockSource.pos().relative(blockSource.state().getValue(DispenserBlock.FACING));

        var list = blockSource.level()
                .getEntitiesOfClass(LivingEntity.class, new AABB(blockPos), livingEntityx -> canEquipWithDispenser(livingEntityx, stack));

        if (list.isEmpty()) return false;

        var accessory = AccessoryRegistry.getAccessoryOrDefault(stack);
        var equipReference = AccessoriesCapability.get(list.getFirst())
                .canEquipAccessory(stack, false);

        if (!stack.isEmpty()) {
            accessory.onEquipFromUse(stack, equipReference.left());

            equipReference.second().equipStack(stack);
        }

        return true;
    }

    public static boolean canEquipWithDispenser(LivingEntity entity, ItemStack stack) {
        var targetCapability = AccessoriesCapability.get(entity);

        if (targetCapability != null) {
            var type = entity.getType();

            if (type.is(AccessoriesTags.MODIFIABLE_ENTITY_WHITELIST) || !type.is(AccessoriesTags.MODIFIABLE_ENTITY_BLACKLIST)) {
                var accessory = AccessoryRegistry.getAccessoryOrDefault(stack);
                var equipReference = targetCapability.canEquipAccessory(stack, false);

                return equipReference != null && accessory.canEquipFromDispenser(stack, equipReference.left());
            }
        }

        return false;
    }
}
