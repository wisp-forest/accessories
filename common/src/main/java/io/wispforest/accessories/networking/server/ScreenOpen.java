package io.wispforest.accessories.networking.server;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.menu.AccessoriesMenuVariant;
import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public record ScreenOpen(int entityId, boolean targetLookEntity, AccessoriesMenuVariant variant, @Nullable ItemStack creativeCarriedStack) {

    public static final StructEndec<ScreenOpen> ENDEC = StructEndecBuilder.of(
            Endec.VAR_INT.fieldOf("entityId", ScreenOpen::entityId),
            Endec.BOOLEAN.fieldOf("targetLookEntity", ScreenOpen::targetLookEntity),
            Endec.forEnum(AccessoriesMenuVariant.class).fieldOf("screenType", ScreenOpen::variant),
            MinecraftEndecs.ITEM_STACK.nullableOf().fieldOf("creativeCarriedStack", ScreenOpen::creativeCarriedStack),
            ScreenOpen::new
    );

    public static ScreenOpen of(@Nullable LivingEntity livingEntity, AccessoriesMenuVariant variant){
        return of(livingEntity, variant, null);
    }

    public static ScreenOpen of(@Nullable LivingEntity livingEntity, AccessoriesMenuVariant variant, @Nullable ItemStack creativeCarriedStack){
        return new ScreenOpen(livingEntity != null ? livingEntity.getId() : -1, false, variant, creativeCarriedStack);
    }

    public static void handlePacket(ScreenOpen packet, Player player) {
        LivingEntity livingEntity = null;

        if(packet.entityId() != -1) {
            var entity = player.level().getEntity(packet.entityId());

            if(entity instanceof LivingEntity living) {
                livingEntity = living;

                var bl = !player.equals(livingEntity)
                        && player.getPermissionLevel() == 0
                        && player.entityInteractionRange() < player.distanceTo(livingEntity);

                // Prevent people without op perms to have the ability to open inv from any distance
                if(bl) return;
            }
        } else if(packet.targetLookEntity()) {
            Accessories.attemptOpenScreenPlayer((ServerPlayer) player, packet.variant());

            return;
        }

        ItemStack carriedStack = null;

        if (packet.creativeCarriedStack != null && player.isCreative()) {
            carriedStack = packet.creativeCarriedStack;
        } else if(player.containerMenu instanceof AbstractContainerMenu oldMenu) {
            var currentCarriedStack = oldMenu.getCarried();

            if(!currentCarriedStack.isEmpty()) {
                carriedStack = currentCarriedStack;

                oldMenu.setCarried(ItemStack.EMPTY);
            }
        }

        Accessories.openAccessoriesMenu(player, packet.variant(), livingEntity, carriedStack);
    }
}