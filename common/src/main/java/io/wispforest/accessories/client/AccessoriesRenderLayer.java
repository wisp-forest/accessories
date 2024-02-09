package io.wispforest.accessories.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.SlotReference;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistery;
import io.wispforest.accessories.client.gui.AccessoriesScreen;
import io.wispforest.accessories.mixin.RenderLayerAccessor;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Render layer used to render equipped Accessories for a given {@link LivingEntity}.
 * Such is only applied to {@link LivingEntityRenderer} that have a model that
 * extends {@link HumanoidModel}
 */
public class AccessoriesRenderLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    public AccessoriesRenderLayer(RenderLayerParent<T, M> renderLayerParent) {
        super(renderLayerParent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource multiBufferSource, int light, T entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        var capability = AccessoriesAPI.getCapability(entity);

        if (capability.isEmpty()) return;

        for (var entry : capability.get().getContainers().entrySet()) {
            var container = entry.getValue();

            var accessories = container.getAccessories();
            var cosmetics = container.getCosmeticAccessories();

            for (int i = 0; i < accessories.getContainerSize(); i++) {
                var stack = accessories.getItem(i);
                var cosmeticStack = cosmetics.getItem(i);

                if (!cosmeticStack.isEmpty()) stack = cosmeticStack;

                var renderer = AccessoriesRendererRegistery.getRender(stack.getItem());

                if (renderer.isEmpty()) {
                    AccessoriesScreen.NOT_VERY_NICE_POSITIONS.remove(container.getSlotName() + i);
                    continue;
                }

                poseStack.pushPose();

                var rendering = container.shouldRender(i);

                var mpoatv = new MPOATVConstructingVertexConsumer();

                renderer.get()
                        .render(
                                rendering,
                                stack,
                                new SlotReference(container.getSlotName(), entity, i),
                                poseStack,
                                getRenderLayerParent(),
                                renderType -> VertexMultiConsumer.create(multiBufferSource.getBuffer(renderType), mpoatv),
                                light,
                                limbSwing,
                                limbSwingAmount,
                                partialTicks,
                                netHeadYaw,
                                headPitch
                        );

                if (rendering) {
                    if (AccessoriesClient.renderingPlayerModelInAccessoriesScreen) {
                        var meanPos = mpoatv.meanPos;
                        AccessoriesScreen.NOT_VERY_NICE_POSITIONS.put(container.getSlotName() + i, meanPos);
                    }
                } else {
                    AccessoriesScreen.NOT_VERY_NICE_POSITIONS.remove(container.getSlotName() + i);
                }

                poseStack.popPose();
            }
        }
    }

    protected RenderLayerParent<T, M> getRenderLayerParent() {
        return ((RenderLayerAccessor) this).accessories$getRenderer();
    }
}