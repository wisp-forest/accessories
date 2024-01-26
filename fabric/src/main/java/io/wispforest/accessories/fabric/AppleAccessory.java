package io.wispforest.accessories.fabric;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.wispforest.accessories.AccessoriesAccess;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.Accessory;
import io.wispforest.accessories.api.SlotReference;
import io.wispforest.accessories.api.client.AccessoryRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class AppleAccessory implements Accessory {

    @Override
    public void tick(ItemStack stack, SlotReference reference) {
        if (!(reference.entity() instanceof ServerPlayer player)) return;

        if (player.getFoodData().getFoodLevel() > 16) return;


        if (!AccessoriesAPI.getCapability(player).get().isEquipped(Items.APPLE)) return;

        player.getFoodData().eat(Items.APPLE, stack);
        stack.shrink(1);

        player.playNotifySound(SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 1, 1);
    }

    @Environment(EnvType.CLIENT)
    public static class Renderer implements SimpleAccessoryRenderer {

        @Override
        public <T extends LivingEntity, M extends EntityModel<T>> void align(LivingEntity entity, M model, PoseStack matrices, float netHeadYaw, float headPitch) {
            if(!(model instanceof HumanoidModel<? extends LivingEntity> humanoidModel)) return;

            AccessoryRenderer.translateToFace(matrices, (HumanoidModel<LivingEntity>) humanoidModel, entity, netHeadYaw, headPitch);

            matrices.mulPose(Axis.XP.rotationDegrees(180));
            matrices.translate(0, -.1, -.03);
        }
    }
}
