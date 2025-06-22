package io.wispforest.testccessories.neoforge.accessories;

import com.mojang.blaze3d.vertex.PoseStack;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.core.Accessory;
import io.wispforest.accessories.api.core.AccessoryRegistry;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import io.wispforest.accessories.api.client.renderers.AccessoryRenderer;
import io.wispforest.accessories.api.client.renderers.SimpleAccessoryRenderer;
import io.wispforest.accessories.api.client.rendering.Side;
import io.wispforest.accessories.api.slot.SlotPath;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.testccessories.neoforge.Testccessories;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class AppleAccessory implements Accessory {

    @OnlyIn(Dist.CLIENT)
    public static void clientInit(){
        AccessoriesRendererRegistry.bindItemToRenderer(Items.APPLE, Testccessories.of("apple_renderer"), Renderer::new);
    }

    public static void init(){
        AccessoryRegistry.register(Items.APPLE, new AppleAccessory());
    }

    @Override
    public void tick(ItemStack stack, SlotReference reference) {
        if (!(reference.entity() instanceof ServerPlayer player)) return;

        if (player.getFoodData().getFoodLevel() > 16) return;


        if (!AccessoriesCapability.get(player).isEquipped(Items.APPLE)) return;

        var foodProperties = stack.get(DataComponents.FOOD);
        player.getFoodData().eat(foodProperties);
        stack.shrink(1);

        player.playNotifySound(SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 1, 1);
    }

    @OnlyIn(Dist.CLIENT)
    public static class Renderer implements SimpleAccessoryRenderer {

        @Override
        public <S extends LivingEntityRenderState> void align(ItemStack stack, SlotPath path, EntityModel<S> model, S renderState, PoseStack matrices) {
            if (!(model instanceof HumanoidModel<? extends HumanoidRenderState> humanoidModel)) return;

            AccessoryRenderer.transformToFace(matrices, humanoidModel.head, Side.FRONT);
        }

        @Override
        public <S extends LivingEntityRenderState> void render(ItemStack stack, SlotPath path, PoseStack matrices, EntityModel<S> model, S renderState, MultiBufferSource multiBufferSource, int light, float partialTicks) {
            align(stack, path, model, renderState, matrices);

            for (int i = 0; i < stack.getCount(); i++) {
                Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, matrices, multiBufferSource, Minecraft.getInstance().level, 0);
                matrices.translate(0, 0, 1/16f);
            }
        }
    }
}