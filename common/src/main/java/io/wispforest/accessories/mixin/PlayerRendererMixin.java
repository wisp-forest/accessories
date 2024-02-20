package io.wispforest.accessories.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistery;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {

    @Shadow public abstract void render(AbstractClientPlayer entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight);

    @Unique
    private static HumanoidArm currentArm = null;

//    @WrapWithCondition(method = "renderHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V"))
//    private boolean accessories$fixOverridenInvisibility(ModelPart instance, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay) {
//        var returned = AccessoriesClient.IS_PLAYER_INVISIBLE;
//        AccessoriesClient.IS_PLAYER_INVISIBLE = false;
//        return returned;
//    }

    @Inject(method = "renderHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V", ordinal = 1, shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILHARD)
    private void accessories$firstPersonAccessories(PoseStack poseStack, MultiBufferSource buffer, int combinedLight, AbstractClientPlayer player, ModelPart rendererArm, ModelPart rendererArmwear, CallbackInfo ci, PlayerModel playerModel, ResourceLocation resourceLocation) {
        if (currentArm != null) {
            var capability = AccessoriesCapability.get(player);

            if (capability.isEmpty()) return;

            for (var entry : capability.get().getContainers().entrySet()) {
                var container = entry.getValue();

                var accessories = container.getAccessories();
                var cosmetics = container.getCosmeticAccessories();

                for (int i = 0; i < accessories.getContainerSize(); i++) {
                    var stack = accessories.getItem(i);
                    var cosmeticStack = cosmetics.getItem(i);

                    if (!cosmeticStack.isEmpty()) stack = cosmeticStack;

                    var renderer = AccessoriesRendererRegistery.getOrDefaulted(stack.getItem());

//                    if (renderer.isEmpty()) renderer = Optional.of(new DefaultAccessoryRenderer());

                    poseStack.pushPose();

                    var rendering = container.shouldRender(i);

                    if (currentArm == HumanoidArm.LEFT) {
                        renderer.renderOnFirstPersonLeftArm(
                                rendering,
                                stack,
                                new SlotReference(container.getSlotName(), player, i),
                                poseStack,
                                playerModel,
                                buffer,
                                combinedLight
                        );
                    } else {
                        renderer.renderOnFirstPersonRightArm(
                                rendering,
                                stack,
                                new SlotReference(container.getSlotName(), player, i),
                                poseStack,
                                playerModel,
                                buffer,
                                combinedLight
                        );
                    }
                    poseStack.popPose();
                }
            }
        }
        currentArm = null;
    }

    @Inject(method = "renderRightHand", at = @At("HEAD"))
    private void accessories$firstPersonRightAccessories(PoseStack poseStack, MultiBufferSource buffer, int combinedLight, AbstractClientPlayer player, CallbackInfo ci) {
        currentArm = HumanoidArm.RIGHT;
    }

    @Inject(method = "renderLeftHand", at = @At("HEAD"))
    private void accessories$firstPersonLeftAccessories(PoseStack poseStack, MultiBufferSource buffer, int combinedLight, AbstractClientPlayer player, CallbackInfo ci) {
        currentArm = HumanoidArm.LEFT;
    }
}