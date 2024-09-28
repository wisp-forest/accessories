package io.wispforest.accessories.networking.server;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.menu.AccessoriesMenuVariant;
import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public record ScreenOpen(int entityId, boolean targetLookEntity, AccessoriesMenuVariant variant) {

    public static final StructEndec<ScreenOpen> ENDEC = StructEndecBuilder.of(
            Endec.VAR_INT.fieldOf("entityId", ScreenOpen::entityId),
            Endec.BOOLEAN.fieldOf("targetLookEntity", ScreenOpen::targetLookEntity),
            Endec.forEnum(AccessoriesMenuVariant.class).fieldOf("screenType", ScreenOpen::variant),
            ScreenOpen::new
    );

    public static ScreenOpen of(@Nullable LivingEntity livingEntity, AccessoriesMenuVariant variant){
        return new ScreenOpen(livingEntity != null ? livingEntity.getId() : -1, false, variant);
    }

    public static ScreenOpen of(boolean targetLookEntity, AccessoriesMenuVariant variant){
        return new ScreenOpen(-1, targetLookEntity, variant);
    }

    public static void handlePacket(ScreenOpen packet, Player player) {
        LivingEntity livingEntity = null;

        if(packet.entityId() != -1) {
            var entity = player.level().getEntity(packet.entityId());

            if(entity instanceof LivingEntity living) livingEntity = living;
        } else if(packet.targetLookEntity()) {
            Accessories.attemptOpenScreenPlayer((ServerPlayer) player, packet.variant());

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

        Accessories.openAccessoriesMenu(player, packet.variant(), livingEntity, carriedStack);
    }
}