package io.wispforest.accessories.mixin;

import io.wispforest.accessories.api.events.extra.ExtraEventHandler;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderMan.class)
public abstract class EndermanMixin {

    @Inject(method = "isLookingAtMe", at = @At("HEAD"), cancellable = true)
    private void isEndermanMaskAccessory(Player player, CallbackInfoReturnable<Boolean> cir){
        var state = ExtraEventHandler.isEndermanMask(player, (EnderMan) (Object) this);

        if(state != TriState.DEFAULT) cir.setReturnValue(state.orElse(false));
    }
}
