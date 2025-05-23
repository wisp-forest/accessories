package io.wispforest.accessories.menu;

import io.wispforest.accessories.menu.variants.AccessoriesMenuBase;
import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record AccessoriesMenuData(Optional<Integer> targetEntityId, int slotAmountAdded, @Nullable ItemStack carriedStack) {
    public static final Endec<AccessoriesMenuData> ENDEC = StructEndecBuilder.of(
            Endec.VAR_INT.optionalOf().fieldOf("targetEntityId", AccessoriesMenuData::targetEntityId),
            Endec.VAR_INT.fieldOf("slotAmountAdded", AccessoriesMenuData::slotAmountAdded),
            MinecraftEndecs.ITEM_STACK.nullableOf().fieldOf("carriedStack", AccessoriesMenuData::carriedStack),
            AccessoriesMenuData::new
    );

    public static AccessoriesMenuData of(@Nullable LivingEntity livingEntity, AccessoriesMenuBase base) {
        return new AccessoriesMenuData(Optional.ofNullable(livingEntity != null ? livingEntity.getId() : null), base.slotAmountAdded(), base.getCarried());
    }
}
