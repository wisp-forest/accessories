package io.wispforest.accessories.mixin.client.cosmetic;

import io.wispforest.accessories.pond.CosmeticArmorLookupTogglable;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity {

    protected PlayerMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "getItemBySlot", at = @At("HEAD"), cancellable = true)
    private void accessories$getCosmeticAlternative(EquipmentSlot slot, CallbackInfoReturnable<ItemStack> cir) {
        CosmeticArmorLookupTogglable.getAlternativeStack(this, slot, cir::setReturnValue);
    }
}
