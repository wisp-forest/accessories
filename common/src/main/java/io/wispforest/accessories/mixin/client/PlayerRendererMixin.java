package io.wispforest.accessories.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
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
public abstract class PlayerRendererMixin extends LivingEntityRenderer<AbstractClientPlayer, PlayerRenderState, PlayerModel> {

    public PlayerRendererMixin(EntityRendererProvider.Context context, PlayerModel model, float shadowRadius) {
        super(context, model, shadowRadius);
    }

    @Unique
    private static HumanoidArm currentArm = null;

//    @WrapWithCondition(method = "renderHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V"))
//    private boolean accessories$fixOverridenInvisibility(ModelPart instance, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay) {
//        var returned = AccessoriesClient.IS_PLAYER_INVISIBLE;
//        AccessoriesClient.IS_PLAYER_INVISIBLE = false;
//        return returned;
//    }

    @Inject(method = "renderHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V", ordinal = 0, shift = At.Shift.AFTER))
    private void accessories$firstPersonAccessories(PoseStack poseStack, MultiBufferSource buffer, int combinedLight, ResourceLocation resourceLocation, ModelPart rendererArm, boolean bl, CallbackInfo ci, @Local PlayerModel playerModel) {
        var player = Minecraft.getInstance().player;
        var level = Minecraft.getInstance().player.level();

        var partialTicks = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(!level.tickRateManager().isEntityFrozen(player));

        var state = this.createRenderState(player, partialTicks);

        if (currentArm != null) {
            var capability = AccessoriesCapability.get(player);

            if (capability == null) return;

            for (var entry : capability.getContainers().entrySet()) {
                var container = entry.getValue();

                var accessories = container.getAccessories();
                var cosmetics = container.getCosmeticAccessories();

                for (int i = 0; i < accessories.getContainerSize(); i++) {
                    var stack = accessories.getItem(i);
                    var cosmeticStack = cosmetics.getItem(i);

                    if (!cosmeticStack.isEmpty() && Accessories.config().clientOptions.showCosmeticAccessories()) stack = cosmeticStack;

                    if (stack.isEmpty()) continue;

                    var renderer = AccessoriesRendererRegistry.getRender(stack);

                    if(renderer == null || !renderer.shouldRender(container.shouldRender(i))) continue;

                    poseStack.pushPose();

                    renderer.renderOnFirstPerson(
                        currentArm,
                        stack,
                        SlotReference.of(player, container.getSlotName(), i),
                        poseStack,
                        playerModel,
                        state,
                        buffer,
                        combinedLight,
                        partialTicks
                    );

                    poseStack.popPose();
                }
            }
        }
        currentArm = null;
    }

    @Inject(method = "renderRightHand", at = @At("HEAD"))
    private void accessories$firstPersonRightAccessories(PoseStack poseStack, MultiBufferSource buffer, int combinedLight, ResourceLocation resourceLocation, boolean bl, CallbackInfo ci) {
        currentArm = HumanoidArm.RIGHT;
    }

    @Inject(method = "renderLeftHand", at = @At("HEAD"))
    private void accessories$firstPersonLeftAccessories(PoseStack poseStack, MultiBufferSource buffer, int combinedLight, ResourceLocation resourceLocation, boolean bl, CallbackInfo ci) {
        currentArm = HumanoidArm.LEFT;
    }
}