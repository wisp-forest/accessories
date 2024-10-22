package io.wispforest.accessories.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import software.bernie.geckolib.util.InternalUtil;

import java.util.function.BiConsumer;

@ApiStatus.Internal
public class GeckoLibCompat {
    public static <S extends HumanoidRenderState, M extends HumanoidModel<S>, A extends HumanoidModel<S>> boolean renderGeckoArmor(PoseStack poseStack, MultiBufferSource bufferSource, S renderState, ItemStack stack, EquipmentSlot equipmentSlot, M parentModel, A baseModel, float partialTicks, int light, BiConsumer<A, EquipmentSlot> partVisibilitySetter) {
        return false;
        // TODO: RE-ENABLE WHEN POSSIBLE
        //return InternalUtil.tryRenderGeoArmorPiece(poseStack, bufferSource, renderState, stack, equipmentSlot, parentModel, baseModel, partialTicks, light, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, partVisibilitySetter);
    }
}
