package io.wispforest.accessories.menu;

import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.pond.ArmorSlotExtension;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.inventory.ArmorSlot;
import org.jetbrains.annotations.Nullable;

public class AccessoriesArmorSlot extends ArmorSlot implements SlotTypeAccessible, ArmorSlotExtension {

    public final AccessoriesContainer accessoriesContainer;

    public AccessoriesArmorSlot(AccessoriesContainer accessoriesContainer, Container container, LivingEntity livingEntity, EquipmentSlot equipmentSlot, int i, int j, int k, @Nullable ResourceLocation resourceLocation) {
        super(container, livingEntity, equipmentSlot, i, j, k, resourceLocation);

        this.accessoriesContainer = accessoriesContainer;
    }

    @Override
    public String slotName() {
        return accessoriesContainer.getSlotName();
    }

    @Override
    public SlotType slotType() {
        return accessoriesContainer.slotType();
    }
}
