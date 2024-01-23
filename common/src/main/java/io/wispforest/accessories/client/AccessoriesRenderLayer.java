package io.wispforest.accessories.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.wispforest.accessories.AccessoriesAccess;
import io.wispforest.accessories.api.SlotReference;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistery;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;

public class AccessoriesRenderLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    public AccessoriesRenderLayer(RenderLayerParent<T, M> renderLayerParent) {
        super(renderLayerParent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource multiBufferSource, int light, T entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        var api = AccessoriesAccess.getAPI();

        var capability = api.getCapability(entity);

        if(capability.isEmpty()) return;

        for (var entry : capability.get().getContainers().entrySet()) {
            var accessories = entry.getValue().getAccessories();
            var cosmetics = entry.getValue().getCosmeticAccessories();

            for (int i = 0; i < accessories.getContainerSize(); i++) {
                var stack = accessories.getItem(i);
                var cosmeticStack = cosmetics.getItem(i);

                if(!cosmeticStack.isEmpty()) stack = cosmeticStack;

                var renderer = AccessoriesRendererRegistery.getRender(stack.getItem());

                if(renderer.isEmpty()) continue;

                poseStack.pushPose();

                var rendering = entry.getValue().renderOptions().get(i);

                renderer.get()
                        .render(
                                rendering,
                                stack,
                                new SlotReference(entry.getValue().slotType(), entity, i),
                                poseStack,
                                getRenderLayerParent(),
                                multiBufferSource,
                                light,
                                limbSwing,
                                limbSwingAmount,
                                partialTicks,
                                netHeadYaw,
                                headPitch
                        );

                poseStack.popPose();
            }
        }
    }

    protected RenderLayerParent<T, M> getRenderLayerParent(){
        return this.renderer;
    }
}
