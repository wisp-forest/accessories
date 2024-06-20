package io.wispforest.accessories.neoforge.mixin;

import io.wispforest.accessories.client.AccessoriesRenderLayer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> {

//    @Shadow public abstract boolean addLayer(RenderLayer<T, M> layer);
//
//    @Inject(method = "<init>", at = @At("TAIL"))
//    private void attemptToAddLayer(EntityRendererProvider.Context context, EntityModel model, float shadowRadius, CallbackInfo ci){
//        if(model instanceof HumanoidModel) this.addLayer(new AccessoriesRenderLayer<>((LivingEntityRenderer<T, M>)(Object)this));
//    }
}
