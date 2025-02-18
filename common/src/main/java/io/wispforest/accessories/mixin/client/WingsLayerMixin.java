package io.wispforest.accessories.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import io.wispforest.accessories.api.equip.EquipmentChecking;
import io.wispforest.accessories.pond.LivingEntityRenderStateExtension;
import io.wispforest.accessories.pond.WingsLayerExtension;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(WingsLayer.class)
public abstract class WingsLayerMixin<S extends HumanoidRenderState, M extends EntityModel<S>> implements WingsLayerExtension<S> {

    @Unique
    private static final Logger LOGGER = LogUtils.getLogger();

    @Unique private boolean hasPrintedError = false;

    @Shadow public abstract void render(PoseStack poseStack, MultiBufferSource multiBufferSource, int i, S humanoidRenderState, float f, float g);

    @Shadow @Final private EquipmentLayerRenderer equipmentRenderer;

    @Override
    public void renderStack(ItemStack stack, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, S humanoidRenderState) {
        var prevItem = humanoidRenderState.chestEquipment;

        humanoidRenderState.chestEquipment = stack;

        this.render(poseStack, multiBufferSource, i, humanoidRenderState, humanoidRenderState.yRot, humanoidRenderState.xRot);

        humanoidRenderState.chestEquipment = prevItem;
    }

    @WrapOperation(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;get(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;"))
    private Object accessories$adjustGliderItemstack(ItemStack instance, DataComponentType dataComponentType, Operation<Object> original, @Local(argsOnly = true) S humanoidRenderState, @Local(ordinal = 0) LocalRef<ItemStack> stack) {
        if (humanoidRenderState instanceof LivingEntityRenderStateExtension extension) {
            var entity = extension.accessories$getEntity();

            if (entity.isPresent()) {
                var capability = entity.get().accessoriesCapability();

                if (capability != null) {
                    var gliderItem = capability.getFirstEquipped(stack1 -> {
                        var equippable = stack1.get(DataComponents.EQUIPPABLE);

                        if (equippable != null && equippable.assetId().isPresent()) {
                            var list = ((EquipmentLayerRendererAccessor) this.equipmentRenderer).accessories$equipmentAssetManager()
                                    .get(equippable.assetId().get())
                                    .getLayers(EquipmentClientInfo.LayerType.WINGS);

                            return !list.isEmpty();
                        }

                        return false;
                    }, EquipmentChecking.COSMETICALLY_OVERRIDABLE);

                    if (gliderItem != null) {
                        stack.set(gliderItem.stack());

                        instance = gliderItem.stack();
                    }
                }
            } else {
                if (!hasPrintedError) {
                    LOGGER.error("Unable to get the required Living Entity instance from the given LivingEntityRenderState meaning Accessories may not render!");
                    hasPrintedError = true;
                }
            }
        }

        return original.call(instance, dataComponentType);
    }
}
