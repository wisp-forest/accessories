package io.wispforest.accessories.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import io.wispforest.accessories.client.AccessoriesRenderLayer;
import io.wispforest.accessories.pond.CosmeticArmorLookupTogglable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin<A extends Avatar & ClientAvatarEntity> extends LivingEntityRenderer<A, AvatarRenderState, PlayerModel> {

    public AvatarRendererMixin(EntityRendererProvider.Context context, PlayerModel model, float shadowRadius) {
        super(context, model, shadowRadius);
    }

//    @WrapWithCondition(method = "renderHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V"))
//    private boolean accessories$fixOverridenInvisibility(ModelPart instance, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay) {
//        var returned = AccessoriesClient.IS_PLAYER_INVISIBLE;
//        AccessoriesClient.IS_PLAYER_INVISIBLE = false;
//        return returned;
//    }

    @WrapMethod(method = "renderHand")
    public void accessories$cosmeticallyAdjustFirstPersonView(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int i, ResourceLocation resourceLocation, ModelPart modelPart, boolean bl, Operation<Void> original) {
        CosmeticArmorLookupTogglable.runWithLookupToggle(Minecraft.getInstance().player, () -> original.call(poseStack, submitNodeCollector, i, resourceLocation, modelPart, bl));
    }

    @Inject(method = "renderHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModelPart(Lnet/minecraft/client/model/geom/ModelPart;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/RenderType;IILnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V", shift = At.Shift.AFTER))
    private void accessories$firstPersonAccessories(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int combinedLight, ResourceLocation resourceLocation, ModelPart rendererArm, boolean bl, CallbackInfo ci, @Local PlayerModel playerModel) {
        AccessoriesRenderLayer.submitFirstPersonAsClientPlayer((AvatarRenderer<A>) (Object) this, playerModel, poseStack, combinedLight, submitNodeCollector, rendererArm == this.model.leftArm ? HumanoidArm.LEFT : HumanoidArm.RIGHT);
    }
}