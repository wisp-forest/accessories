package io.wispforest.accessories.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import io.wispforest.accessories.api.caching.ItemStackBasedPredicate;
import io.wispforest.accessories.pond.LivingEntityRenderStateExtension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemDisplayContext;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(CustomHeadLayer.class)
public abstract class CustomHeadLayerMixin<S extends LivingEntityRenderState, M extends EntityModel<S> & HeadedModel> {

    @Unique private static final Logger LOGGER = LogUtils.getLogger();

    @Unique private boolean hasPrintedError = false;

    //TODO: FIGURE OUT WHY ARCH LOOM DON'T REMAP WRAP METHOD
    @WrapMethod(method = {
            "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V",        // Mojmap
            "method_17159(Lnet/minecraft/class_4587;Lnet/minecraft/class_4597;ILnet/minecraft/class_10042;FF)V",                                                                             // Yarn Interm.
            "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/LivingEntityRenderState;FF)V" // Yarn
    })
    private void accessories$adjustHeadItem(PoseStack poseStack, MultiBufferSource multiBufferSource, int i, S livingEntityRenderState, float f, float g, Operation<Void> original) {
        ItemStackRenderState prevState = null;

        if (livingEntityRenderState instanceof LivingEntityRenderStateExtension extension) {
            var entity = extension.accessories$getEntity();

            if (entity.isPresent()) {
                var capability = entity.get().accessoriesCapability();

                if (capability != null) {
                    var ref = capability.getEquipped(ItemStackBasedPredicate.ofClass(BannerItem.class))
                            .stream()
                            .filter(slotEntryReference -> slotEntryReference.reference().slotName().equals("hat"))
                            .findFirst()
                            .orElse(null);

                    if (ref != null) {
                        var stack = ref.stack();
                        prevState = livingEntityRenderState.headItem;

                        var alternativeRenderState = new ItemStackRenderState();

                        Minecraft.getInstance().getItemModelResolver()
                                .updateForLiving(alternativeRenderState, stack, ItemDisplayContext.HEAD, false, entity.get());

                        ((LivingEntityRenderStateAccessor) livingEntityRenderState).accessories$headItem(alternativeRenderState);
                    }
                }
            } else {
                if (!hasPrintedError) {
                    LOGGER.error("Unable to get the required Living Entity instance from the given LivingEntityRenderState meaning Accessories may not render!");
                    hasPrintedError = true;
                }
            }
        }

        original.call(poseStack, multiBufferSource, i, livingEntityRenderState, f, g);

        if (prevState != null) ((LivingEntityRenderStateAccessor) livingEntityRenderState).accessories$headItem(prevState);
    }
}
