package io.wispforest.accessories.client;

import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.StructEndecBuilder;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record AccessoriesMenuData(Optional<Integer> targetEntityId) {
    public static final Endec<AccessoriesMenuData> ENDEC = StructEndecBuilder.of(
            Endec.VAR_INT.optionalOf().fieldOf("targetEntityId", AccessoriesMenuData::targetEntityId),
            AccessoriesMenuData::new
    );

    public static AccessoriesMenuData of(@Nullable LivingEntity livingEntity) {
        return new AccessoriesMenuData(Optional.ofNullable(livingEntity != null ? livingEntity.getId() : null));
    }
}
