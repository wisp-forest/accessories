package io.wispforest.accessories.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import io.wispforest.accessories.api.caching.ItemStackBasedPredicate;
import io.wispforest.accessories.api.data.AccessoriesBaseData;
import io.wispforest.accessories.pond.AccessoriesRenderStateAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(CustomHeadLayer.class)
public abstract class CustomHeadLayerMixin<S extends LivingEntityRenderState, M extends EntityModel<S> & HeadedModel> {

    //TODO: FIGURE OUT WHY ARCH LOOM DON'T REMAP WRAP METHOD
    @WrapMethod(method = {
            "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V",      // Mojmap
            "method_17159(Lnet/minecraft/class_4587;Lnet/minecraft/class_11659;ILnet/minecraft/class_10042;FF)V",                                                                             // Yarn Interm.
            "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/client/render/entity/state/LivingEntityRenderState;FF)V" // Yarn
    })
    private void accessories$adjustHeadItem(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int i, S livingEntityRenderState, float f, float g, Operation<Void> original) {
        ItemStackRenderState prevState = null;

        if (livingEntityRenderState instanceof AccessoriesRenderStateAPI extension) {
            var lookup = extension.getStorageLookup();

            if (lookup != null) {
                var ref = lookup.getEquipped(ItemStackBasedPredicate.ofClass(BannerItem.class))
                        .stream()
                        .filter(entry -> entry.path().slotName().equals(AccessoriesBaseData.HAT_SLOT))
                        .findFirst()
                        .orElse(null);

                if (ref != null) {
                    var stack = ref.stack();
                    prevState = livingEntityRenderState.headItem;

                    var alternativeRenderState = new ItemStackRenderState();

                    Minecraft.getInstance().getItemModelResolver()
                            .updateForTopItem(alternativeRenderState, stack, ItemDisplayContext.HEAD, Minecraft.getInstance().level, null, extension.getEntityUUIDForState().hashCode() + 5);

                    ((LivingEntityRenderStateAccessor) livingEntityRenderState).accessories$headItem(alternativeRenderState);
                }
            }
        }

        original.call(poseStack, submitNodeCollector, i, livingEntityRenderState, f, g);

        if (prevState != null) ((LivingEntityRenderStateAccessor) livingEntityRenderState).accessories$headItem(prevState);
    }
}
