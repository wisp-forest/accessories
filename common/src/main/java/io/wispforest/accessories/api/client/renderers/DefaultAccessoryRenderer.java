package io.wispforest.accessories.api.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.Accessory;
import io.wispforest.accessories.api.client.rendering.Side;
import io.wispforest.accessories.api.client.rendering.ModelTransformOps;
import io.wispforest.accessories.api.components.AccessoriesDataComponents;
import io.wispforest.accessories.api.components.AccessoryCustomRendererComponent;
import io.wispforest.accessories.api.slot.SlotReference;
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
    public <S extends LivingEntityRenderState> void render(ItemStack stack, SlotReference reference, PoseStack matrices, EntityModel<S> model, S renderState, MultiBufferSource multiBufferSource, int light, float partialTicks) {
        if (!(model instanceof HumanoidModel<? extends HumanoidRenderState> humanoidModel)) return;

        var disabledTargetType = Accessories.config().clientOptions.disabledDefaultRenders();

        for (var target : disabledTargetType) {
            if(reference.slotName().equals(target.slotType) && target.targetType.isValid(stack.getItem())) return;
        }

        Consumer<PoseStack> render = (poseStack) -> Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, poseStack, multiBufferSource, reference.entity().level(), 0);

        var translationData = stack.getOrDefault(AccessoriesDataComponents.CUSTOM_RENDERER, AccessoryCustomRendererComponent.EMPTY);

        if(!translationData.disableDefaultTranslations()) {
            var helper = slotToHelpers.get(reference.slotName());

            if (helper != null) {
                helper.render(render, matrices, humanoidModel, renderState, reference);
            }
        } else {
            render.accept(matrices);
        }
    }

    @Override
    public boolean shouldRenderInFirstPerson(HumanoidArm arm, ItemStack stack, SlotReference reference) {
        var slotName = reference.slotName();

        return (slotName.equals("hand") || slotName.equals("wrist") || slotName.equals("ring")) && (reference.slot() % 2 == 0 ? arm == HumanoidArm.RIGHT : arm == HumanoidArm.LEFT);
    }

    public interface RenderHelper {
        <S extends LivingEntityRenderState> void render(Consumer<PoseStack> renderCall, PoseStack matrices, HumanoidModel<? extends HumanoidRenderState> humanoidModel, S renderState, SlotReference reference);
    }

    //--

    private static final Map<String, RenderHelper> DEFAULT_HELPERS;

    static {
        DEFAULT_HELPERS = Map.ofEntries(
                Map.entry("face", new RenderHelper() {
                    @Override
                    public <S extends LivingEntityRenderState> void render(Consumer<PoseStack> renderCall, PoseStack matrices, HumanoidModel<? extends HumanoidRenderState> humanoidModel, S renderState, SlotReference reference) {
                        ModelTransformOps.transformToFace(matrices, reference.entity(), humanoidModel, "head", Side.FRONT);
                        renderCall.accept(matrices);
                    }
                }),
                Map.entry("hat", new RenderHelper() {
                    @Override
                    public <S extends LivingEntityRenderState> void render(Consumer<PoseStack> renderCall, PoseStack matrices, HumanoidModel<? extends HumanoidRenderState> humanoidModel, S renderState, SlotReference reference) {
                        ModelTransformOps.transformToFace(matrices, reference.entity(), humanoidModel, "head", Side.TOP);
                        matrices.translate(0, 0.25, 0);
                        for (int i = 0; i < reference.getStack().getCount(); i++) {
                            renderCall.accept(matrices);
                            matrices.translate(0, 0.5, 0);
                        }
                    }
                }),
                Map.entry("back", new RenderHelper() {
                    @Override
                    public <S extends LivingEntityRenderState> void render(Consumer<PoseStack> renderCall, PoseStack matrices, HumanoidModel<? extends HumanoidRenderState> humanoidModel, S renderState, SlotReference reference) {
                        ModelTransformOps.transformToFace(matrices, reference.entity(), humanoidModel, "body", Side.BACK);
                        matrices.scale(1.5f, 1.5f, 1.5f);
                        renderCall.accept(matrices);
                    }
                }),
                Map.entry("necklace", new RenderHelper() {
                    @Override
                    public <S extends LivingEntityRenderState> void render(Consumer<PoseStack> renderCall, PoseStack matrices, HumanoidModel<? extends HumanoidRenderState> humanoidModel, S renderState, SlotReference reference) {
                        ModelTransformOps.transformToModelPart(matrices, reference.entity(), humanoidModel, "body", 0, 1, 1);
                        matrices.translate(0, -0.25, 0);
                        renderCall.accept(matrices);
                    }
                }),
                Map.entry("cape", new RenderHelper() {
                    @Override
                    public <S extends LivingEntityRenderState> void render(Consumer<PoseStack> renderCall, PoseStack matrices, HumanoidModel<? extends HumanoidRenderState> humanoidModel, S renderState, SlotReference reference) {
                        ModelTransformOps.transformToModelPart(matrices, reference.entity(), humanoidModel, "body", 0, 1, -1);
                        matrices.translate(0, -0.25, 0);
                        renderCall.accept(matrices);
                    }
                }),
                Map.entry("ring", new RenderHelper() {
                    @Override
                    public <S extends LivingEntityRenderState> void render(Consumer<PoseStack> renderCall, PoseStack matrices, HumanoidModel<? extends HumanoidRenderState> humanoidModel, S renderState, SlotReference reference) {
                        var modelTarget = reference.slot() % 2 == 0 ? "right_arm" : "left_arm";
                        var xPercent = reference.slot() % 2 == 0 ? 1 : -1;

                        ModelTransformOps.transformToModelPart(
                                matrices,
                                reference.entity(),
                                humanoidModel,
                                modelTarget,
                                xPercent,
                                -1,
                                0
                        );
                        var offset = reference.slot() / 2;
                        matrices.translate(
                                (reference.slot() % 2 == 0 ? -1 : 1) * offset * -0.0001,
                                0.25 * (offset + 1),
                                0
                        );
                        matrices.scale(0.5f, 0.5f, 0.5f);
                        matrices.mulPose(Axis.YP.rotationDegrees(90));
                        for (int i = 0; i < reference.getStack().getCount(); i++) {
                            renderCall.accept(matrices);
                            matrices.translate(
                                    0,
                                    0,
                                    reference.slot() % 2 == 0 ? -0.5 : 0.5
                            );
                        }
                    }
                }),
                Map.entry("wrist", new RenderHelper() {
                    @Override
                    public <S extends LivingEntityRenderState> void render(Consumer<PoseStack> renderCall, PoseStack matrices, HumanoidModel<? extends HumanoidRenderState> humanoidModel, S renderState, SlotReference reference) {
                        var modelTarget = reference.slot() % 2 == 0 ? "right_arm" : "left_arm";
                        ModelTransformOps.transformToModelPart(matrices, reference.entity(), humanoidModel, modelTarget, 0, -0.5, 0);
                        matrices.scale(1.01f, 1.01f, 1.01f);
                        matrices.mulPose(Axis.YP.rotationDegrees(90));
                        renderCall.accept(matrices);
                    }
                }),
                Map.entry("hand", new RenderHelper() {
                    @Override
                    public <S extends LivingEntityRenderState> void render(Consumer<PoseStack> renderCall, PoseStack matrices, HumanoidModel<? extends HumanoidRenderState> humanoidModel, S renderState, SlotReference reference) {
                        var modelTarget = reference.slot() % 2 == 0 ? "right_arm" : "left_arm";
                        ModelTransformOps.transformToFace(matrices, reference.entity(), humanoidModel, modelTarget, Side.BOTTOM);
                        matrices.translate(0, 0.25, 0);
                        matrices.scale(1.02f, 1.02f, 1.02f);
                        matrices.mulPose(Axis.YP.rotationDegrees(90));
                        renderCall.accept(matrices);
                    }
                }),
                Map.entry("belt", new RenderHelper() {
                    @Override
                    public <S extends LivingEntityRenderState> void render(Consumer<PoseStack> renderCall, PoseStack matrices, HumanoidModel<? extends HumanoidRenderState> humanoidModel, S renderState, SlotReference reference) {
                        ModelTransformOps.transformToFace(matrices, reference.entity(), humanoidModel, "body", Side.BOTTOM);
                        matrices.scale(1.01f, 1.01f, 1.01f);
                        renderCall.accept(matrices);
                    }
                }),
                Map.entry("anklet", new RenderHelper() {
                    @Override
                    public <S extends LivingEntityRenderState> void render(Consumer<PoseStack> renderCall, PoseStack matrices, HumanoidModel<? extends HumanoidRenderState> humanoidModel, S renderState, SlotReference reference) {
                        var modelTarget = reference.slot() % 2 == 0 ? "right_leg" : "left_leg";
                        ModelTransformOps.transformToModelPart(matrices, reference.entity(), humanoidModel, modelTarget, 0, -0.5, 0);
                        matrices.scale(1.01f, 1.01f, 1.01f);
                        renderCall.accept(matrices);
                    }
                }),
                Map.entry("shoes", new RenderHelper() {
                    @Override
                    public <S extends LivingEntityRenderState> void render(Consumer<PoseStack> renderCall, PoseStack matrices, HumanoidModel<? extends HumanoidRenderState> humanoidModel, S renderState, SlotReference reference) {
                        matrices.pushPose();
                        ModelTransformOps.transformToFace(matrices, reference.entity(), humanoidModel, "right_leg", Side.BOTTOM);
                        matrices.translate(0, 0.25, 0);
                        matrices.scale(1.02f, 1.02f, 1.02f);
                        renderCall.accept(matrices);
                        matrices.popPose();
                        matrices.pushPose();
                        ModelTransformOps.transformToFace(matrices, reference.entity(), humanoidModel, "left_leg", Side.BOTTOM);
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
