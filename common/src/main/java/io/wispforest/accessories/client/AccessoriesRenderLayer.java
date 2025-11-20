package io.wispforest.accessories.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.client.AccessoriesRenderStateKeys;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import io.wispforest.accessories.api.menu.AccessoriesBasedSlot;
import io.wispforest.accessories.api.slot.SlotPath;
import io.wispforest.accessories.client.gui.AccessoriesScreenBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;


/**
 * Render layer used to render equipped Accessories for a given {@link LivingEntity}.
 * This is only applied to {@link LivingEntityRenderer} that have a model that
 * extends {@link HumanoidModel}
 */
public class AccessoriesRenderLayer<S extends LivingEntityRenderState, M extends EntityModel<S>> extends RenderLayer<S, M> {

    private static final float increment = 0.1f;

    private static final Map<SlotPath, Float> brightnessMap = new HashMap<>();
    private static final Map<SlotPath, Float> opacityMap = new HashMap<>();

    private static long lastUpdated20th = 0;

    public AccessoriesRenderLayer(RenderLayerParent<S, M> renderLayerParent) {
        super(renderLayerParent);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light, S entityState, float f, float g) {
        var client = Minecraft.getInstance();

        var states = entityState.getStateData(AccessoriesRenderStateKeys.ACCESSORY_RENDER_STATES);

        if (states == null) return;

        entityState.setStateData(AccessoriesRenderStateKeys.LIGHT, light);

        var funkyRenderState = AccessoriesFunkyRenderingState.INSTANCE;
        
        var isRenderingLineTarget = funkyRenderState.isIsRenderingLineTarget();

        var renderingLines = funkyRenderState.isCollectAccessoryPositions();
        var positions = funkyRenderState.getNotVeryNicePositions();

        if (!renderingLines && !positions.isEmpty()) {
            positions.clear();
        }
        
//        var useCustomerBuffer = funkyRenderState.isIsRenderingUiEntity();
//
//        if (useCustomerBuffer && multiBufferSource instanceof MultiBufferSource.BufferSource bufferSource) {
//            bufferSource.endBatch();
//        }

        var scale = (float) (1 + (0.5 * (0.75 + (Math.sin((System.currentTimeMillis()) / 250d)))));

        var calendar = Calendar.getInstance();

        var current20th = calendar.getTimeInMillis() / 50;

        var shouldUpdate = lastUpdated20th != current20th;
        if (shouldUpdate) lastUpdated20th = current20th;

        var selected = (client.screen instanceof AccessoriesScreenBase<?> screenBase)
            ? screenBase.getSelectedSlotIf(AccessoriesBasedSlot.class)
            : null;

        boolean preventHovering = selected != null && selected.getItem().isEmpty();

        var unHoveredOptions = Accessories.config().screenOptions.unHoveredOptions;
        var hoveredOptions = Accessories.config().screenOptions.hoveredOptions;

        var isFunnyDate = calendar.get(Calendar.MONTH) + 1 == 5 && calendar.get(Calendar.DATE) == 16;

        var selectedPath = selected != null ? selected.slotPath() : null;

        states.forEach((path, accessoryRenderState) -> {
            var isSelected = path.equals(selectedPath);

            if (shouldUpdate) {
                var currentBrightness = brightnessMap.getOrDefault(path, 1f);
                var currentOpacity = opacityMap.getOrDefault(path, 1f);

                if (selectedPath != null && !isSelected && !preventHovering) {
                    brightnessMap.put(path, Math.max(unHoveredOptions.darkenedBrightness(), currentBrightness - increment));
                    opacityMap.put(path, Math.max(unHoveredOptions.darkenedOpacity(), currentOpacity - increment));
                } else {
                    brightnessMap.put(path, Math.min(1, currentBrightness + increment));
                    opacityMap.put(path, Math.min(1, currentOpacity + increment));
                }
            }

            var stack = accessoryRenderState.getStateData(AccessoriesRenderStateKeys.ITEM_STACK);
            var renderer = AccessoriesRendererRegistry.getRenderer(stack);

//            var mpoatv = new MPOATVConstructingVertexConsumer();
//
//            var bufferedGrabbedFlag = new MutableBoolean(false);
//
//            MultiBufferSource innerBufferSource = (renderType) -> {
//                bufferedGrabbedFlag.setValue(true);
//
//                return useCustomerBuffer ?
//                    VertexMultiConsumer.create(multiBufferSource.getBuffer(renderType), mpoatv) :
//                    multiBufferSource.getBuffer(renderType);
//            };

            if (/*!useCustomerBuffer || */isSelected || selectedPath == null || unHoveredOptions.renderUnHovered()) {
                poseStack.pushPose();

                try {
                    renderer.render(accessoryRenderState, entityState, getParentModel(), poseStack, submitNodeCollector);
                } catch (Throwable e) {
                    AccessoryRendererErrorCache.logIfTimeAllotted(entityState.getEntityUUIDForState(), stack, renderer, e);
                }

                poseStack.popPose();
            }

            // Code area for handling the hovering effect that makes such items on the entity glow if within a screen for such
//            if (useCustomerBuffer && bufferedGrabbedFlag.getValue()) {
//                if (multiBufferSource instanceof MultiBufferSource.BufferSource bufferSource) {
//                    Color shaderColor = null;
//
//                    if (hoveredOptions.brightenHovered() && isSelected) {
//                        if (isFunnyDate) {
//                            var hue = (float) ((System.currentTimeMillis() / 20d % 360d) / 360d);
//                            shaderColor = Color.ofHsv(hue, 1, 1);
//                        } else {
//                            var mul = hoveredOptions.cycleBrightness() ? scale : 1.5f;
//                            shaderColor = new Color(mul, mul, mul, 1);
//                        }
//                    } else if (unHoveredOptions.darkenUnHovered()) {
//                        var darkness = brightnessMap.getOrDefault(mapKey, 1f);
//
//                        shaderColor = new Color(darkness, darkness, darkness, opacityMap.getOrDefault(mapKey, 1f));
//                    }
//
//                    if (shaderColor != null) {
//                        var encoder = RenderSystem.getDevice().createCommandEncoder();
//                        var main = client.getMainRenderTarget();
//                        var buffer = AccessoriesPipelines.getOrCreateBuffer();
//
//                        encoder.copyTextureToTexture(main.getDepthTexture(), buffer.getDepthTexture(), 0, 0, 0, 0, 0, buffer.width, buffer.height);
//                        encoder.clearColorTexture(buffer.getColorTexture(), 0);
//
//                        funkyRenderState.wrapBufferManipulation(bufferSource::endBatch);
//
//                        var window = client.getWindow();
//
//                        var x2 = window.getGuiScaledWidth();
//                        var y2 = window.getGuiScaledHeight();
//
////                            bufferSource.getBuffer(AccessoriesPipelines.setupHoverEffect(shaderColor))
////                                .addVertex(0, 0, 0).setUv(0, 1).setColor(0xffffffff)
////                                .addVertex(0, y2, 0).setUv(0, 0).setColor(0xffffffff)
////                                .addVertex(x2, y2, 0).setUv(1, 0).setColor(0xffffffff)
////                                .addVertex(x2, 0, 0).setUv(1, 1).setColor(0xffffffff);
//                    }
//
//                    bufferSource.endBatch();
//                }
//            }

//            if (renderingLines && isRenderingLineTarget) {
//                var pos = mpoatv.meanPos();
//
//                if (pos != null) positions.put(path, pos);
//            }
        });
    }

    public static <A extends Avatar & ClientAvatarEntity> void submitFirstPersonAsClientPlayer(AvatarRenderer<A> avatarRender, HumanoidModel<AvatarRenderState> model, PoseStack matrices, int combinedLight, SubmitNodeCollector submitNodeCollector, HumanoidArm arm) {
        var player = Minecraft.getInstance().player;

        submitFirstPerson(player, (AvatarRenderer) avatarRender, model, matrices, combinedLight, submitNodeCollector, arm);
    }

    public static <A extends Avatar & ClientAvatarEntity> void submitFirstPerson(A entity, AvatarRenderer<A> avatarRender, HumanoidModel<AvatarRenderState> model, PoseStack matrices, int combinedLight, SubmitNodeCollector submitNodeCollector, HumanoidArm arm) {
        var level = entity.level();

        var partialTicks = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(!level.tickRateManager().isEntityFrozen(entity));

        var entityState = avatarRender.createRenderState(entity, partialTicks);

        entityState.setStateData(AccessoriesRenderStateKeys.LIGHT, combinedLight);

        AccessoriesRenderStateKeys.setupStateForAccessories(entityState, entity, partialTicks, arm);

        var states = entityState.getStateData(AccessoriesRenderStateKeys.ACCESSORY_RENDER_STATES);

        if (states == null) return;

        states.forEach((slotPath, accessoryRenderState) -> {
            matrices.pushPose();

            var stack = accessoryRenderState.getStateData(AccessoriesRenderStateKeys.ITEM_STACK);
            var renderer = AccessoriesRendererRegistry.getRenderer(stack);

            try {
                renderer.render(accessoryRenderState, entityState, model, matrices, submitNodeCollector);
            } catch (Throwable e) {
                AccessoryRendererErrorCache.logIfTimeAllotted(entityState.getEntityUUIDForState(), stack, renderer, e);
            }

            matrices.popPose();
        });
    }

}
