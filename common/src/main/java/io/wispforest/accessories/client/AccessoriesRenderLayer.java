package io.wispforest.accessories.client;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import com.mojang.logging.LogUtils;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import io.wispforest.accessories.api.slot.SlotPath;
import io.wispforest.accessories.client.gui.AccessoriesScreenBase;
import io.wispforest.accessories.menu.AccessoriesInternalSlot;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.event.WindowResizeCallback;
import io.wispforest.owo.ui.util.ScissorStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;


/**
 * Render layer used to render equipped Accessories for a given {@link LivingEntity}.
 * This is only applied to {@link LivingEntityRenderer} that have a model that
 * extends {@link HumanoidModel}
 */
public class AccessoriesRenderLayer<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<S>> extends RenderLayer<S, M> {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static TextureTarget BUFFER;
    public static boolean overrideRenderTarget = false;
    private static Color shaderColor = null;
    private static final RenderType RENDER_TYPE = RenderType.create(
        "dawg",
        786432,
        RenderPipelines.GUI_TEXTURED_OVERLAY,
        RenderType.CompositeState.builder().setTextureState(new RenderStateShard.EmptyTextureStateShard(
            () -> {
                RenderSystem.setShaderTexture(0, BUFFER.getColorTexture());
                if (shaderColor != null) RenderSystem.setShaderColor(shaderColor.red(), shaderColor.green(), shaderColor.blue(), shaderColor.alpha());
            },
            () -> {
                if (shaderColor != null) {
                    RenderSystem.setShaderColor(1, 1, 1, 1);
                    shaderColor = null;
                }
            }
        )).createCompositeState(false)
    );

    private static final float increment = 0.1f;

    private static final Map<String, Float> brightnessMap = new HashMap<>();
    private static final Map<String, Float> opacityMap = new HashMap<>();

    private static long lastUpdated20th = 0;

    public AccessoriesRenderLayer(RenderLayerParent<S, M> renderLayerParent) {
        super(renderLayerParent);
    }

    @ApiStatus.Internal
    public static void initialize(Minecraft client) {
        var window = client.getWindow();
        BUFFER = new TextureTarget("accessories_buffer_thingy", window.getWidth(), window.getHeight(), true);
        WindowResizeCallback.EVENT.register((innerClient, innerWindow) -> {
            if (BUFFER == null) return;
            BUFFER.resize(innerWindow.getWidth(), innerWindow.getHeight());
        });
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
        var entity = entityRenderState.getEntityForState();
        var uuid = entityRenderState.getEntityUUIDForState();

        if (storageLookup == null) return;

        var partialTicks = client.getDeltaTracker()
            .getGameTimeDeltaPartialTick(!entity.map(entity1 -> entity1.level().tickRateManager().isEntityFrozen(entity1)).orElse(true));

        var containers = storageLookup.getContainers();

        if (containers.isEmpty()) return;

        var renderingLines = AccessoriesFunkyRenderingState.isCollectAccessoryPositions();

        if (!renderingLines && !AccessoriesFunkyRenderingState.getNotVeryNicePositions().isEmpty()) {
            AccessoriesFunkyRenderingState.getNotVeryNicePositions().clear();
        }

        var useCustomerBuffer = AccessoriesFunkyRenderingState.isIsRenderingUiEntity();

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

                if (!AccessoriesFunkyRenderingState.isIsRenderingUiEntity() || isSelected || selected == null || unHoveredOptions.renderUnHovered()) {
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


                if (useCustomerBuffer && bufferedGrabbedFlag.getValue()) {
                    if (multiBufferSource instanceof MultiBufferSource.BufferSource bufferSource) {
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
                            encoder.copyTextureToTexture(main.getDepthTexture(), BUFFER.getDepthTexture(), 0, 0, 0, 0, 0, BUFFER.width, BUFFER.height);
                            encoder.clearColorTexture(BUFFER.getColorTexture(), 0);
                            overrideRenderTarget = true;
                            try {
                                bufferSource.endBatch();
                            } finally {
                                overrideRenderTarget = false;
                            }

                            blit(bufferSource);
                        }
                        bufferSource.endBatch();
                    }
                }

                if (renderingLines && AccessoriesFunkyRenderingState.isIsRenderingLineTarget()) {
                    AccessoriesFunkyRenderingState.getNotVeryNicePositions().put(container.getSlotName() + i, mpoatv.meanPos());
                }
            }
        }
    }

    private void blit(MultiBufferSource bufferSource) {
        var client = Minecraft.getInstance();
        var window = client.getWindow();
        var x2 = window.getGuiScaledWidth();
        var y2 = window.getGuiScaledHeight();
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RENDER_TYPE);
        vertexConsumer.addVertex(0, 0, 0).setUv(0, 1).setColor(0xffffffff);
        vertexConsumer.addVertex(0, y2, 0).setUv(0, 0).setColor(0xffffffff);
        vertexConsumer.addVertex(x2, y2, 0).setUv(1, 0).setColor(0xffffffff);
        vertexConsumer.addVertex(x2, 0, 0).setUv(1, 1).setColor(0xffffffff);
    }
}
