package io.wispforest.accessories.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import io.wispforest.accessories.AccessoriesAccess;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.SlotReference;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistery;
import io.wispforest.accessories.api.client.MPOATVConstructingVertexConsumer;
import io.wispforest.accessories.client.gui.AccessoriesScreen;
import io.wispforest.accessories.mixin.RenderLayerAccessor;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.IntIntPair;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4f;

/**
 * Render layer specific for Accessories Rendering inwhich are only applied
 * onto any model extending {@link HumanoidModel} with the requirement for
 * such to be registered by others if not such a model
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