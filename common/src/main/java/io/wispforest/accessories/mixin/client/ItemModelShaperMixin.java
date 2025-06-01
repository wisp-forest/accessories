package io.wispforest.accessories.mixin.client;

import io.wispforest.accessories.api.components.AccessoriesDataComponents;
import net.minecraft.client.renderer.ItemModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemModelShaper.class)
public abstract class ItemModelShaperMixin {
    @Shadow @Final private ModelManager modelManager;

    @Inject(method = "getItemModel(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/client/resources/model/BakedModel;", at = @At("HEAD"), cancellable = true)
    private void accessories$adjustModelFromComponent(ItemStack stack, CallbackInfoReturnable<BakedModel> cir) {
        if (!stack.has(AccessoriesDataComponents.ITEM_MODEL_OVERRIDE)) return;

        var model = this.modelManager.getModel(ModelResourceLocation.inventory(stack.get(AccessoriesDataComponents.ITEM_MODEL_OVERRIDE).id()));

        cir.setReturnValue(model);
    }
}
