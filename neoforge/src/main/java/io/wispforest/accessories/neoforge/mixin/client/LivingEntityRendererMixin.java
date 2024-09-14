package io.wispforest.accessories.neoforge.mixin.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> {

//    @Shadow public abstract boolean addLayer(RenderLayer<T, M> layer);
//
//    @Inject(method = "<init>", at = @At("TAIL"))
//    private void attemptToAddLayer(EntityRendererProvider.Context context, EntityModel model, float shadowRadius, CallbackInfo ci){
//        if(model instanceof HumanoidModel) this.addLayer(new AccessoriesRenderLayer<>((LivingEntityRenderer<T, M>)(Object)this));
//    }
}
