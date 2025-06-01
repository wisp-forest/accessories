package io.wispforest.accessories.menu;

import io.wispforest.accessories.menu.variants.AccessoriesMenuBase;
import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.StructEndecBuilder;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record AccessoriesMenuData(Optional<Integer> targetEntityId, int slotAmountAdded) {
    public static final Endec<AccessoriesMenuData> ENDEC = StructEndecBuilder.of(
            Endec.VAR_INT.optionalOf().fieldOf("targetEntityId", AccessoriesMenuData::targetEntityId),
            Endec.VAR_INT.fieldOf("slotAmountAdded", AccessoriesMenuData::slotAmountAdded),
            AccessoriesMenuData::new
    );

    public static AccessoriesMenuData of(@Nullable LivingEntity livingEntity, AccessoriesMenuBase base) {
        return new AccessoriesMenuData(Optional.ofNullable(livingEntity != null ? livingEntity.getId() : null), base.slotAmountAdded());
    }
}
