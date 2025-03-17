package io.wispforest.accessories.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import software.bernie.geckolib.util.InternalUtil;

import java.util.function.BiConsumer;

@ApiStatus.Internal
public class GeckoLibCompat {
    public static <T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>> boolean renderGeckoArmor(PoseStack poseStack, MultiBufferSource bufferSource, T entity, ItemStack stack, EquipmentSlot equipmentSlot, M parentModel, A baseModel, float partialTicks, int light, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, BiConsumer<A, EquipmentSlot> partVisibilitySetter) {
        return InternalUtil.tryRenderGeoArmorPiece(poseStack, bufferSource, entity, stack, equipmentSlot, parentModel, baseModel, partialTicks, light, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, partVisibilitySetter);
    }
}
