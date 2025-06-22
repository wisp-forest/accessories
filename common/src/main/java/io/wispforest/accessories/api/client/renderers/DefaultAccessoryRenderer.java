package io.wispforest.accessories.api.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.core.Accessory;
import io.wispforest.accessories.api.client.rendering.Side;
import io.wispforest.accessories.api.client.rendering.ModelTransformOps;
import io.wispforest.accessories.api.components.AccessoriesDataComponents;
import io.wispforest.accessories.api.components.AccessoryCustomRendererComponent;
import io.wispforest.accessories.api.slot.SlotPath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Default Renderer for any {@link Accessory} that doesn't have a renderer registered.
 */
public class DefaultAccessoryRenderer implements AccessoryRenderer {

    private static final Logger LOGGER = LogUtils.getLogger();

    //--

    public static final DefaultAccessoryRenderer INSTANCE;

    private final Map<String, RenderHelper> slotToHelpers = new HashMap<>();

    public DefaultAccessoryRenderer(){
        slotToHelpers.putAll(DEFAULT_HELPERS);
    }

    /**
     * Registers a {@link RenderHelper} for a given slot name if not already registered
     */
    public static void registerHelper(String slotType, RenderHelper helper){
        var helpers = INSTANCE.slotToHelpers;

        if(!helpers.containsKey(slotType)){
            helpers.put(slotType, helper);
        } else {
            LOGGER.warn("[DefaultAccessoryRenderer] Unable to add to the main renderer instance due to a duplicate helper already exists!");
        }
    }

    @Override
    public <S extends LivingEntityRenderState> void render(ItemStack stack, SlotPath path, PoseStack matrices, EntityModel<S> model, S renderState, MultiBufferSource multiBufferSource, int light, float partialTicks) {
        if (!(model instanceof HumanoidModel<? extends HumanoidRenderState> humanoidModel)) return;

        var disabledTargetType = Accessories.config().clientOptions.disabledDefaultRenders();

        for (var target : disabledTargetType) {
            if(path.slotName().equals(target.slotType) && target.targetType.isValid(stack.getItem())) return;
        }

        Consumer<PoseStack> renderCall = (poseStack) -> Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, poseStack, multiBufferSource, null, 0);

        var translationData = stack.getOrDefault(AccessoriesDataComponents.CUSTOM_RENDERER, AccessoryCustomRendererComponent.EMPTY);

        if(!translationData.disableDefaultTranslations()) {
            var helper = slotToHelpers.get(path.slotName());

            if (helper != null) {
                helper.render(stack, path, matrices, humanoidModel, renderState, renderCall);
            }
        } else {
            renderCall.accept(matrices);
        }
    }

    @Override
    public <S extends LivingEntityRenderState> boolean shouldRenderInFirstPerson(HumanoidArm arm, ItemStack stack, SlotPath path, S renderState) {
        var slotName = path.slotName();

        return (slotName.equals("hand") || slotName.equals("wrist") || slotName.equals("ring")) && (path.index() % 2 == 0 ? arm == HumanoidArm.RIGHT : arm == HumanoidArm.LEFT);
    }

    public interface RenderHelper {
        <S extends LivingEntityRenderState> void render(ItemStack stack, SlotPath path, PoseStack matrices, HumanoidModel<? extends HumanoidRenderState> humanoidModel, S renderState, Consumer<PoseStack> renderCall);
    }

    //--

    private static final Map<String, RenderHelper> DEFAULT_HELPERS;

    static {
        DEFAULT_HELPERS = Map.ofEntries(
                Map.entry("face", new RenderHelper() {
                    @Override
                    public <S extends LivingEntityRenderState> void render(ItemStack stack, SlotPath path, PoseStack matrices, HumanoidModel<? extends HumanoidRenderState> humanoidModel, S renderState, Consumer<PoseStack> renderCall) {
                        ModelTransformOps.transformToFace(matrices, renderState, humanoidModel, "head", Side.FRONT);
                        renderCall.accept(matrices);
                    }
                }),
                Map.entry("hat", new RenderHelper() {
                    @Override
                    public <S extends LivingEntityRenderState> void render(ItemStack stack, SlotPath path, PoseStack matrices, HumanoidModel<? extends HumanoidRenderState> humanoidModel, S renderState, Consumer<PoseStack> renderCall) {
                        ModelTransformOps.transformToFace(matrices, renderState, humanoidModel, "head", Side.TOP);
                        matrices.translate(0, 0.25, 0);
                        for (int i = 0; i < stack.getCount(); i++) {
                            renderCall.accept(matrices);
                            matrices.translate(0, 0.5, 0);
                        }
                    }
                }),
                Map.entry("back", new RenderHelper() {
                    @Override
                    public <S extends LivingEntityRenderState> void render(ItemStack stack, SlotPath path, PoseStack matrices, HumanoidModel<? extends HumanoidRenderState> humanoidModel, S renderState, Consumer<PoseStack> renderCall) {
                        ModelTransformOps.transformToFace(matrices, renderState, humanoidModel, "body", Side.BACK);
                        matrices.scale(1.5f, 1.5f, 1.5f);
                        renderCall.accept(matrices);
                    }
                }),
                Map.entry("necklace", new RenderHelper() {
                    @Override
                    public <S extends LivingEntityRenderState> void render(ItemStack stack, SlotPath path, PoseStack matrices, HumanoidModel<? extends HumanoidRenderState> humanoidModel, S renderState, Consumer<PoseStack> renderCall) {
                        ModelTransformOps.transformToModelPart(matrices, renderState, humanoidModel, "body", 0, 1, 1);
                        matrices.translate(0, -0.25, 0);
                        renderCall.accept(matrices);
                    }
                }),
                Map.entry("cape", new RenderHelper() {
                    @Override
                    public <S extends LivingEntityRenderState> void render(ItemStack stack, SlotPath path, PoseStack matrices, HumanoidModel<? extends HumanoidRenderState> humanoidModel, S renderState, Consumer<PoseStack> renderCall) {
                        ModelTransformOps.transformToModelPart(matrices, renderState, humanoidModel, "body", 0, 1, -1);
                        matrices.translate(0, -0.25, 0);
                        renderCall.accept(matrices);
                    }
                }),
                Map.entry("ring", new RenderHelper() {
                    @Override
                    public <S extends LivingEntityRenderState> void render(ItemStack stack, SlotPath path, PoseStack matrices, HumanoidModel<? extends HumanoidRenderState> humanoidModel, S renderState, Consumer<PoseStack> renderCall) {
                        var modelTarget = path.index() % 2 == 0 ? "right_arm" : "left_arm";
                        var xPercent = path.index() % 2 == 0 ? 1 : -1;

                        ModelTransformOps.transformToModelPart(
                                matrices,
                                renderState,
                                humanoidModel,
                                modelTarget,
                                xPercent,
                                -1,
                                0
                        );
                        var offset = path.index() / 2;
                        matrices.translate(
                                (path.index() % 2 == 0 ? -1 : 1) * offset * -0.0001,
                                0.25 * (offset + 1),
                                0
                        );
                        matrices.scale(0.5f, 0.5f, 0.5f);
                        matrices.mulPose(Axis.YP.rotationDegrees(90));
                        for (int i = 0; i < stack.getCount(); i++) {
                            renderCall.accept(matrices);
                            matrices.translate(
                                    0,
                                    0,
                                    path.index() % 2 == 0 ? -0.5 : 0.5
                            );
                        }
                    }
                }),
                Map.entry("wrist", new RenderHelper() {
                    @Override
                    public <S extends LivingEntityRenderState> void render(ItemStack stack, SlotPath path, PoseStack matrices, HumanoidModel<? extends HumanoidRenderState> humanoidModel, S renderState, Consumer<PoseStack> renderCall) {
                        var modelTarget = path.index() % 2 == 0 ? "right_arm" : "left_arm";
                        ModelTransformOps.transformToModelPart(matrices, renderState, humanoidModel, modelTarget, 0, -0.5, 0);
                        matrices.scale(1.01f, 1.01f, 1.01f);
                        matrices.mulPose(Axis.YP.rotationDegrees(90));
                        renderCall.accept(matrices);
                    }
                }),
                Map.entry("hand", new RenderHelper() {
                    @Override
                    public <S extends LivingEntityRenderState> void render(ItemStack stack, SlotPath path, PoseStack matrices, HumanoidModel<? extends HumanoidRenderState> humanoidModel, S renderState, Consumer<PoseStack> renderCall) {
                        var modelTarget = path.index() % 2 == 0 ? "right_arm" : "left_arm";
                        ModelTransformOps.transformToFace(matrices, renderState, humanoidModel, modelTarget, Side.BOTTOM);
                        matrices.translate(0, 0.25, 0);
                        matrices.scale(1.02f, 1.02f, 1.02f);
                        matrices.mulPose(Axis.YP.rotationDegrees(90));
                        renderCall.accept(matrices);
                    }
                }),
                Map.entry("belt", new RenderHelper() {
                    @Override
                    public <S extends LivingEntityRenderState> void render(ItemStack stack, SlotPath path, PoseStack matrices, HumanoidModel<? extends HumanoidRenderState> humanoidModel, S renderState, Consumer<PoseStack> renderCall) {
                        ModelTransformOps.transformToFace(matrices, renderState, humanoidModel, "body", Side.BOTTOM);
                        matrices.scale(1.01f, 1.01f, 1.01f);
                        renderCall.accept(matrices);
                    }
                }),
                Map.entry("anklet", new RenderHelper() {
                    @Override
                    public <S extends LivingEntityRenderState> void render(ItemStack stack, SlotPath path, PoseStack matrices, HumanoidModel<? extends HumanoidRenderState> humanoidModel, S renderState, Consumer<PoseStack> renderCall) {
                        var modelTarget = path.index() % 2 == 0 ? "right_leg" : "left_leg";
                        ModelTransformOps.transformToModelPart(matrices, renderState, humanoidModel, modelTarget, 0, -0.5, 0);
                        matrices.scale(1.01f, 1.01f, 1.01f);
                        renderCall.accept(matrices);
                    }
                }),
                Map.entry("shoes", new RenderHelper() {
                    @Override
                    public <S extends LivingEntityRenderState> void render(ItemStack stack, SlotPath path, PoseStack matrices, HumanoidModel<? extends HumanoidRenderState> humanoidModel, S renderState, Consumer<PoseStack> renderCall) {
                        matrices.pushPose();
                        ModelTransformOps.transformToFace(matrices, renderState, humanoidModel, "right_leg", Side.BOTTOM);
                        matrices.translate(0, 0.25, 0);
                        matrices.scale(1.02f, 1.02f, 1.02f);
                        renderCall.accept(matrices);
                        matrices.popPose();
                        matrices.pushPose();
                        ModelTransformOps.transformToFace(matrices, renderState, humanoidModel, "left_leg", Side.BOTTOM);
                        matrices.translate(0, 0.25, 0);
                        matrices.scale(1.02f, 1.02f, 1.02f);
                        renderCall.accept(matrices);
                        matrices.popPose();
                    }
                })
        );

        INSTANCE = new DefaultAccessoryRenderer();
    }
}
