package io.wispforest.accessories.api.events.extra;

import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DeathProtection;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Experimental
public interface OnTotemActivate {

    OnTotemActivate DEFAULT_BEHAVIOR = (currentProtection, slotReference, triggeredStack, damageSource) -> currentProtection;

    @Nullable DeathProtection onActivation(DeathProtection currentProtection, SlotReference slotReference, ItemStack triggeredStack, DamageSource damageSource);
}
