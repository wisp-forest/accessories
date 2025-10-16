package io.wispforest.accessories.pond;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.item.ItemStack;

public interface WingsLayerExtension<S extends HumanoidRenderState> {
    void renderStack(ItemStack stack, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int i, S humanoidRenderState);
}
