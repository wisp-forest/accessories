package io.wispforest.cclayer.mixin.compat;

import com.aetherteam.aether.inventory.menu.AccessoriesMenu;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

@Pseudo
@Mixin(AccessoriesMenu.class)
public class AetherAccessoriesMenuMixin {
    @ModifyExpressionValue(method = "lambda$new$0", at = @At(value = "INVOKE", target = "Ltop/theillusivec4/curios/api/type/inventory/ICurioStacksHandler;isVisible()Z"), require = 0, expect = 1)
    private boolean fixEvaluation(boolean value){
        return false;
    }
}
