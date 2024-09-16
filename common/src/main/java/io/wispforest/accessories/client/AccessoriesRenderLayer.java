package io.wispforest.accessories.client;

import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import io.wispforest.accessories.client.gui.AccessoriesScreen;
import io.wispforest.accessories.client.gui.AccessoriesScreenBase;
import io.wispforest.accessories.menu.AccessoriesInternalSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.lwjgl.opengl.GL30;

import java.awt.*;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static io.wispforest.accessories.client.gui.AccessoriesScreen.*;


/**
 * Render layer used to render equipped Accessories for a given {@link LivingEntity}.
 * This is only applied to {@link LivingEntityRenderer} that have a model that
 * extends {@link HumanoidModel}
 */
public class AccessoriesRenderLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {

    private static final PostEffectBuffer BUFFER = new PostEffectBuffer();

    private static final float increment = 0.1f;

    private static Map<String, Float> brightnessMap = new HashMap<>();
    private static Map<String, Float> opacityMap = new HashMap<>();

    private static long lastUpdated20th = 0;

    public AccessoriesRenderLayer(RenderLayerParent<T, M> renderLayerParent) {
        super(renderLayerParent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource multiBufferSource, int light, T entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        var highlightOptions = Accessories.getConfig().clientData.highlightOptions;

        var capability = AccessoriesCapability.get(entity);

        if (capability == null) return;

        var calendar = Calendar.getInstance();

        float scale = (float) (1 + (0.5 * (0.75 + (Math.sin((System.currentTimeMillis()) / 250d)))));

        var renderingLines = AccessoriesScreen.HOLD_LINE_INFO.getValue();

        var useCustomerBuffer = AccessoriesScreenBase.IS_RENDERING_UI_ENTITY.getValue();

        if (!renderingLines && !AccessoriesScreen.NOT_VERY_NICE_POSITIONS.isEmpty()) {
            AccessoriesScreen.NOT_VERY_NICE_POSITIONS.clear();
        }

        if (multiBufferSource instanceof MultiBufferSource.BufferSource bufferSource) {
            bufferSource.endBatch();
        }

        var current20th = calendar.getTimeInMillis() / 50;
        var shouldUpdate = lastUpdated20th != current20th;

        if (shouldUpdate) lastUpdated20th = current20th;

        var screen = Minecraft.getInstance().screen;

        AccessoriesInternalSlot selected = null;

        if (screen instanceof AccessoriesScreenBase screenBase && screenBase.getHoveredSlot() instanceof AccessoriesInternalSlot slot) {
            selected = slot;
        }

        for (var entry : capability.getContainers().entrySet()) {

            var container = entry.getValue();

            var accessories = container.getAccessories();
            var cosmetics = container.getCosmeticAccessories();

            var containerSelected = selected != null && selected.accessoriesContainer.slotType() == container.slotType();

            for (int i = 0; i < accessories.getContainerSize(); i++) {

                var isSelected = containerSelected && selected.getContainerSlot() == i;

                if (shouldUpdate) {
                    var currentBrightness = brightnessMap.getOrDefault(entry.getKey(), 1f);
                    var currentOpacity = opacityMap.getOrDefault(entry.getKey(), 1f);

                    if (selected != null && !isSelected) {
                        brightnessMap.put(entry.getKey(), Math.max(highlightOptions.unselectedOptions.darkenOptions.darkenedBrightness, currentBrightness - increment));
                        opacityMap.put(entry.getKey(), Math.max(highlightOptions.unselectedOptions.darkenOptions.darkenedOpacity, currentOpacity - increment));
                    } else {
                        brightnessMap.put(entry.getKey(), Math.min(1, currentBrightness + increment));
                        opacityMap.put(entry.getKey(), Math.min(1, currentOpacity + increment));
                    }
                }

                var stack = accessories.getItem(i);
                var cosmeticStack = cosmetics.getItem(i);

                if (!cosmeticStack.isEmpty()) stack = cosmeticStack;

                if (stack.isEmpty()) continue;

                var renderer = AccessoriesRendererRegistry.getRender(stack);

                if (renderer == null || !renderer.shouldRender(container.shouldRender(i))) continue;

                poseStack.pushPose();

                var mpoatv = new MPOATVConstructingVertexConsumer();

                var bufferedGrabbedFlag = new MutableBoolean(false);

                MultiBufferSource innerBufferSource = (renderType) -> {
                    bufferedGrabbedFlag.setValue(true);

                    return useCustomerBuffer ?
                            VertexMultiConsumer.create(multiBufferSource.getBuffer(renderType), mpoatv) :
                            multiBufferSource.getBuffer(renderType);
                };

                if (!AccessoriesScreenBase.IS_RENDERING_UI_ENTITY.getValue() || isSelected || selected == null || highlightOptions.unselectedOptions.renderUnselected) {
                    renderer.render(
                            stack,
                            SlotReference.of(entity, container.getSlotName(), i),
                            poseStack,
                            getParentModel(),
                            innerBufferSource,
                            light,
                            limbSwing,
                            limbSwingAmount,
                            partialTicks,
                            ageInTicks,
                            netHeadYaw,
                            headPitch
                    );
                }

                float[] colorValues = null;

                if (useCustomerBuffer && bufferedGrabbedFlag.getValue()) {
                    if (multiBufferSource instanceof MultiBufferSource.BufferSource bufferSource) {
                        if (highlightOptions.highlightHovered && isSelected) {
                            if (calendar.get(Calendar.MONTH) + 1 == 5 && calendar.get(Calendar.DATE) == 16) {
                                var hue = (float) ((System.currentTimeMillis() / 20d % 360d) / 360d);

                                var color = new Color(Mth.hsvToRgb(hue, 1, 1));

                                colorValues = new float[]{color.getRed() / 128f, color.getGreen() / 128f, color.getBlue() / 128f, 1};
                            } else {
                                colorValues = new float[]{scale, scale, scale, 1};
                            }
                        } else if (highlightOptions.unselectedOptions.darkenOptions.darkenUnselected) {
                            var darkness = brightnessMap.getOrDefault(entry.getKey(), 1f);
                            colorValues = new float[]{darkness, darkness, darkness, opacityMap.getOrDefault(entry.getKey(), 1f)};
                        }

                        if (colorValues != null) {
                            BUFFER.beginWrite(true, GL30.GL_DEPTH_BUFFER_BIT);
                            bufferSource.endBatch();
                            BUFFER.endWrite();

                            BUFFER.draw(colorValues);

                            var frameBuffer = BUFFER.buffer();

                            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, frameBuffer.frameBufferId);
                            GL30.glBlitFramebuffer(
                                    0,
                                    0,
                                    frameBuffer.width,
                                    frameBuffer.height,
                                    0,
                                    0,
                                    frameBuffer.width,
                                    frameBuffer.height,
                                    GL30.GL_DEPTH_BUFFER_BIT,
                                    GL30.GL_NEAREST
                            );
                            Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
                        } else {
                            bufferSource.endBatch();
                        }
                    }

                    if (renderingLines && AccessoriesScreen.IS_RENDERING_LINE_TARGET.getValue()) {
                        AccessoriesScreen.NOT_VERY_NICE_POSITIONS.put(container.getSlotName() + i, mpoatv.meanPos());
                    }
                }

                poseStack.popPose();
            }
        }
    }
}
