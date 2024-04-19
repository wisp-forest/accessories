package io.wispforest.accessories.networking.server;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.client.AccessoriesMenu;
import io.wispforest.accessories.networking.AccessoriesPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.Nullable;

public class ScreenOpen extends AccessoriesPacket {

    private int entityId = -1;

    private boolean targetLookEntity = false;

    public ScreenOpen(){
        super(false);
    }

    public ScreenOpen(@Nullable LivingEntity livingEntity){
        this();

        if(livingEntity != null) this.entityId = livingEntity.getId();
    }

    public ScreenOpen(boolean targetLookEntity) {
        this();

        this.targetLookEntity = targetLookEntity;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(this.entityId);
        buf.writeBoolean(this.targetLookEntity);
    }

    @Override
    protected void read(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.targetLookEntity = buf.readBoolean();
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

        if(player.containerMenu instanceof AccessoriesMenu oldMenu) {
            var currentCarriedStack = oldMenu.getCarried();

            if(!currentCarriedStack.isEmpty()) {
                carriedStack = currentCarriedStack;

                oldMenu.setCarried(ItemStack.EMPTY);
            }
        }

        Accessories.openAccessoriesMenu(player, livingEntity, carriedStack);
    }
}