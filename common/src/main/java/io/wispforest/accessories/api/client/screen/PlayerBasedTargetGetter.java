package io.wispforest.accessories.api.client.screen;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public interface PlayerBasedTargetGetter {
    @Nullable LivingEntity getTarget(LocalPlayer player);
}
