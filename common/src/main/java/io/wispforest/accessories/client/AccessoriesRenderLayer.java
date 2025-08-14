package io.wispforest.accessories.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import io.wispforest.accessories.api.slot.SlotPath;
import io.wispforest.accessories.client.gui.AccessoriesScreenBase;
import io.wispforest.accessories.menu.AccessoriesInternalSlot;
import io.wispforest.owo.ui.core.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.NotNull;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;


/**
 * Render layer used to render equipped Accessories for a given {@link LivingEntity}.
 * This is only applied to {@link LivingEntityRenderer} that have a model that
 * extends {@link HumanoidModel}
 */
public class AccessoriesRenderLayer<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<S>> extends RenderLayer<S, M> {

    private static final float increment = 0.1f;

    private static final Map<String, Float> brightnessMap = new HashMap<>();
    private static final Map<String, Float> opacityMap = new HashMap<>();

    private static long lastUpdated20th = 0;

    public AccessoriesRenderLayer(RenderLayerParent<S, M> renderLayerParent) {
        super(renderLayerParent);
    }

    @SuppressWarnings("DataFlowIssue")
    @Override
    public void render(
        @NotNull PoseStack poseStack,
        @NotNull MultiBufferSource multiBufferSource,
        int light,
        S entityRenderState,
        float f,
        float g
    ) {
        var client = Minecraft.getInstance();

        var storageLookup = entityRenderState.getStorageLookup();

        if (storageLookup == null) return;

        var containers = storageLookup.getContainers();

        if (containers.isEmpty()) return;

        var uuid = entityRenderState.getEntityUUIDForState();
        var partialTicks = entityRenderState.getEntityPartialTicksForState();

        var funkyRenderState = AccessoriesFunkyRenderingState.INSTANCE;
        
        var isRenderingLineTarget = funkyRenderState.isIsRenderingLineTarget();

        var renderingLines = funkyRenderState.isCollectAccessoryPositions();
        var positions = funkyRenderState.getNotVeryNicePositions();

        if (!renderingLines && !positions.isEmpty()) {
            positions.clear();
        }
        
        var useCustomerBuffer = funkyRenderState.isIsRenderingUiEntity();

        if (useCustomerBuffer && multiBufferSource instanceof MultiBufferSource.BufferSource bufferSource) {
            bufferSource.endBatch();
        }

        var scale = (float) (1 + (0.5 * (0.75 + (Math.sin((System.currentTimeMillis()) / 250d)))));

        var calendar = Calendar.getInstance();

        var current20th = calendar.getTimeInMillis() / 50;

        var shouldUpdate = lastUpdated20th != current20th;
        if (shouldUpdate) lastUpdated20th = current20th;

        AccessoriesInternalSlot selected = null;

        if (client.screen instanceof AccessoriesScreenBase<?> screenBase && screenBase.getHoveredSlot() instanceof AccessoriesInternalSlot slot) {
            selected = slot;
        }

        boolean preventHovering = selected != null && selected.getItem().isEmpty();

        var unHoveredOptions = Accessories.config().screenOptions.unHoveredOptions;
        var hoveredOptions = Accessories.config().screenOptions.hoveredOptions;

        var isFunnyDate = calendar.get(Calendar.MONTH) + 1 == 5 && calendar.get(Calendar.DATE) == 16;

        for (var entry : containers.entrySet()) {
            var container = entry.getValue();

            var accessories = container.getAccessories();
            var cosmetics = container.getCosmeticAccessories();

            var containerSelected = selected != null && selected.accessoriesContainer.slotType() == container.slotType();

            for (int i = 0; i < accessories.getContainerSize(); i++) {
                var isSelected = containerSelected && selected.getContainerSlot() == i;

                var stack = accessories.getItem(i);
                var cosmeticStack = cosmetics.getItem(i);

                if (!cosmeticStack.isEmpty() && Accessories.config().clientOptions.showCosmeticAccessories()) stack = cosmeticStack;

                // No stack to renderer so no need to run any code
                if (stack.isEmpty()) continue;

                var renderer = AccessoriesRendererRegistry.getRenderer(stack);

                // No Renderer to render meaning no need to run any code
                if (renderer.isEmpty() || !renderer.shouldRender(container.shouldRender(i))) continue;

                var mapKey = entry.getKey() + i;

                if (shouldUpdate) {
                    var currentBrightness = brightnessMap.getOrDefault(mapKey, 1f);
                    var currentOpacity = opacityMap.getOrDefault(mapKey, 1f);

                    if (selected != null && !isSelected && !preventHovering) {
                        brightnessMap.put(mapKey, Math.max(unHoveredOptions.darkenedBrightness(), currentBrightness - increment));
                        opacityMap.put(mapKey, Math.max(unHoveredOptions.darkenedOpacity(), currentOpacity - increment));
                    } else {
                        brightnessMap.put(mapKey, Math.min(1, currentBrightness + increment));
                        opacityMap.put(mapKey, Math.min(1, currentOpacity + increment));
                    }
                }

                var mpoatv = new MPOATVConstructingVertexConsumer();

                var bufferedGrabbedFlag = new MutableBoolean(false);

                MultiBufferSource innerBufferSource = (renderType) -> {
                    bufferedGrabbedFlag.setValue(true);

                    return useCustomerBuffer ?
                        VertexMultiConsumer.create(multiBufferSource.getBuffer(renderType), mpoatv) :
                        multiBufferSource.getBuffer(renderType);
                };

                if (!useCustomerBuffer || isSelected || selected == null || unHoveredOptions.renderUnHovered()) {
                    poseStack.pushPose();

                    try {
                        renderer.render(
                            stack,
                            SlotPath.of(container.getSlotName(), i),
                            poseStack,
                            getParentModel(),
                            entityRenderState,
                            innerBufferSource,
                            light,
                            partialTicks
                        );
                    } catch (Throwable e) {
                        AccessoryRendererErrorCache.logIfTimeAllotted(uuid, stack, renderer, e);
                    }

                    poseStack.popPose();
                }

                // Code area for handling the hovering effect that makes such items on the entity glow if within a screen for such
                if (useCustomerBuffer && bufferedGrabbedFlag.getValue()) {
                    if (multiBufferSource instanceof MultiBufferSource.BufferSource bufferSource) {
                        Color shaderColor = null;

                        if (hoveredOptions.brightenHovered() && isSelected) {
                            if (isFunnyDate) {
                                var hue = (float) ((System.currentTimeMillis() / 20d % 360d) / 360d);
                                shaderColor = Color.ofHsv(hue, 1, 1);
                            } else {
                                var mul = hoveredOptions.cycleBrightness() ? scale : 1.5f;
                                shaderColor = new Color(mul, mul, mul, 1);
                            }
                        } else if (unHoveredOptions.darkenUnHovered()) {
                            var darkness = brightnessMap.getOrDefault(mapKey, 1f);

                            shaderColor = new Color(darkness, darkness, darkness, opacityMap.getOrDefault(mapKey, 1f));
                        }

                        if (shaderColor != null) {
                            var encoder = RenderSystem.getDevice().createCommandEncoder();
                            var main = client.getMainRenderTarget();
                            var buffer = AccessoriesPipelines.getOrCreateBuffer();

                            encoder.copyTextureToTexture(main.getDepthTexture(), buffer.getDepthTexture(), 0, 0, 0, 0, 0, buffer.width, buffer.height);
                            encoder.clearColorTexture(buffer.getColorTexture(), 0);

                            funkyRenderState.wrapBufferManipulation(bufferSource::endBatch);

                            var window = client.getWindow();

                            var x2 = window.getGuiScaledWidth();
                            var y2 = window.getGuiScaledHeight();

                            bufferSource.getBuffer(AccessoriesPipelines.setupHoverEffect(shaderColor))
                                .addVertex(0, 0, 0).setUv(0, 1).setColor(0xffffffff)
                                .addVertex(0, y2, 0).setUv(0, 0).setColor(0xffffffff)
                                .addVertex(x2, y2, 0).setUv(1, 0).setColor(0xffffffff)
                                .addVertex(x2, 0, 0).setUv(1, 1).setColor(0xffffffff);
                        }

                        bufferSource.endBatch();
                    }
                }

                if (renderingLines && isRenderingLineTarget) {
                    var pos = mpoatv.meanPos();

                    if (pos != null) positions.put(container.getSlotName() + i, pos);
                }
            }
        }
    }

}
