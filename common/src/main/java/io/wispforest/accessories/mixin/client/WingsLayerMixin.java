package io.wispforest.accessories.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.vertex.PoseStack;
import io.wispforest.accessories.api.equip.EquipmentChecking;
import io.wispforest.accessories.pond.AccessoriesRenderStateAPI;
import io.wispforest.accessories.pond.WingsLayerExtension;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(WingsLayer.class)
public abstract class WingsLayerMixin<S extends HumanoidRenderState, M extends EntityModel<S>> implements WingsLayerExtension<S> {
    @Accessor("equipmentRenderer")
    public abstract EquipmentLayerRenderer accessories$equipmentRenderer();

    @Invoker("submit")
    public abstract void accessories$submit(PoseStack arg, SubmitNodeCollector arg2, int i, S arg3, float f, float g);

    @Override
    public void renderStack(ItemStack stack, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int i, S humanoidRenderState) {
        var prevItem = humanoidRenderState.chestEquipment;

        humanoidRenderState.chestEquipment = stack;

        this.accessories$submit(poseStack, submitNodeCollector, i, humanoidRenderState, humanoidRenderState.yRot, humanoidRenderState.xRot);

        humanoidRenderState.chestEquipment = prevItem;
    }

    @WrapOperation(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;get(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;"))
    private Object accessories$adjustGliderItemstack(ItemStack instance, DataComponentType dataComponentType, Operation<Object> original, @Local(argsOnly = true) S humanoidRenderState, @Local(ordinal = 0) LocalRef<ItemStack> stack) {
        if (humanoidRenderState instanceof AccessoriesRenderStateAPI extension) {
            var lookup = extension.getStorageLookup();

            if (lookup != null) {
                var gliderItem = lookup.getFirstEquipped(stack1 -> {
                    var equippable = stack1.get(DataComponents.EQUIPPABLE);

                    if (equippable != null && equippable.assetId().isPresent()) {
                        var list = ((EquipmentLayerRendererAccessor) this.accessories$equipmentRenderer()).accessories$equipmentAssetManager()
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
        }

        return original.call(instance, dataComponentType);
    }
}
