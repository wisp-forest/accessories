package io.wispforest.accessories.networking.server;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.client.AccessoriesMenu;
import io.wispforest.accessories.networking.base.HandledPacketPayload;
import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.StructEndecBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public record ScreenOpen(int entityId, boolean targetLookEntity) implements HandledPacketPayload {

    public static final Endec<ScreenOpen> ENDEC = StructEndecBuilder.of(
            Endec.VAR_INT.fieldOf("entityId", ScreenOpen::entityId),
            Endec.BOOLEAN.fieldOf("targetLookEntity", ScreenOpen::targetLookEntity),
            ScreenOpen::new
    );

    public static ScreenOpen of(@Nullable LivingEntity livingEntity){
        return new ScreenOpen(livingEntity != null ? livingEntity.getId() : -1, false);
    }

    public static ScreenOpen of(boolean targetLookEntity){
        return new ScreenOpen(-1, targetLookEntity);
    }

    @Override
    public void handle(Player player) {
        LivingEntity livingEntity = null;

        if(this.entityId != -1) {
            var entity = player.level().getEntity(this.entityId);

            if(entity instanceof LivingEntity living) livingEntity = living;
        } else if(this.targetLookEntity) {
            Accessories.attemptOpenScreenPlayer((ServerPlayer) player);

            return;
        }

        ItemStack carriedStack = null;

        if(player.containerMenu instanceof AbstractContainerMenu oldMenu) {
            var currentCarriedStack = oldMenu.getCarried();

            if(!currentCarriedStack.isEmpty()) {
                carriedStack = currentCarriedStack;

                oldMenu.setCarried(ItemStack.EMPTY);
            }
        }

        Accessories.openAccessoriesMenu(player, livingEntity, carriedStack);
    }
}