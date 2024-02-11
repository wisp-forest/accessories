package io.wispforest.accessories.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.SlotReference;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistery;
import io.wispforest.accessories.client.gui.AccessoriesScreen;
import io.wispforest.accessories.mixin.RenderLayerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.lwjgl.opengl.GL30;

import java.awt.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.function.Consumer;
import java.util.function.Supplier;


/**
 * Render layer used to render equipped Accessories for a given {@link LivingEntity}.
 * Such is only applied to {@link LivingEntityRenderer} that have a model that
 * extends {@link HumanoidModel}
 */
public class AccessoriesRenderLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {

    private static PostEffectBuffer buffer = new PostEffectBuffer();

    public AccessoriesRenderLayer(RenderLayerParent<T, M> renderLayerParent) {
        super(renderLayerParent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource multiBufferSource, int light, T entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        var capability = AccessoriesAPI.getCapability(entity);
        Calendar calendar = Calendar.getInstance();

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

                float scale = (float) (1 + (0.5 * (0.75 + (Math.sin(System.currentTimeMillis() / 300d)))));

//                var color = RenderSystem.getShaderColor();
//                RenderSystem.setShaderColor(scale, scale, scale, 1);
//                AccessoriesClient.preventSettingShaderColor = true;

                renderer.get()
                        .render(
                                rendering,
                                stack,
                                new SlotReference(container.getSlotName(), entity, i),
                                poseStack,
                                getRenderLayerParent().getModel(),
                                renderType ->
                                        AccessoriesClient.renderingPlayerModelInAccessoriesScreen ?
                                                VertexMultiConsumer.create(multiBufferSource.getBuffer(renderType), mpoatv) :
                                                multiBufferSource.getBuffer(renderType),
                                light,
                                limbSwing,
                                limbSwingAmount,
                                partialTicks,
                                netHeadYaw,
                                headPitch
                        );


                if (multiBufferSource instanceof MultiBufferSource.BufferSource) {
                    buffer.beginWrite(true, GL30.GL_DEPTH_BUFFER_BIT);
                    ((MultiBufferSource.BufferSource) multiBufferSource).endBatch();
                    buffer.endWrite();
                    if (AccessoriesClient.currentSlot != null && AccessoriesClient.currentSlot.equals(container.getSlotName() + i)) {
                        if (calendar.get(Calendar.MONTH) + 1 == 5 && calendar.get(Calendar.DATE) == 16) {
                            var color = new Color(Mth.hsvToRgb(
                                    (float) ((System.currentTimeMillis() / 20d % 360d) / 360d), 1, 1
                            ));
                            buffer.draw(new float[]{color.getRed() / 128f, color.getGreen() / 128f, color.getBlue() / 128f, 1});
                        } else {
                            buffer.draw(new float[]{scale, scale, scale, 1});
                        }
                    } else {
                        buffer.draw(new float[]{1, 1, 1, 1});
                    }
                    GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, buffer.buffer().frameBufferId);
                    GL30.glBlitFramebuffer(0, 0, buffer.buffer().width, buffer.buffer().height, 0, 0, buffer.buffer().width, buffer.buffer().height, GL30.GL_DEPTH_BUFFER_BIT, GL30.GL_NEAREST);
                    Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
                }

//                AccessoriesClient.preventSettingShaderColor = false;
//                RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);

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