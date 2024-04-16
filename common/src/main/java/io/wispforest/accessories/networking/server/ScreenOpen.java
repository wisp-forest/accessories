package io.wispforest.accessories.networking.server;

import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.client.AccessoriesMenu;
import io.wispforest.accessories.networking.AccessoriesPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class ScreenOpen extends AccessoriesPacket {

    private int entityId;

    public ScreenOpen(@Nullable LivingEntity livingEntity){
        super(false);

        this.entityId = livingEntity != null ? livingEntity.getId() : -1;
    }

    public ScreenOpen(){
        super(false);

        this.entityId = -1;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(this.entityId);
    }

    @Override
    protected void read(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
    }

    @Override
    public void handle(Player player) {
        LivingEntity livingEntity = null;

        if(this.entityId != -1) {
            var entity = player.level().getEntity(this.entityId);

            if(entity instanceof LivingEntity living) livingEntity = living;
        }

        ItemStack carriedStack = null;

        if(player.containerMenu instanceof AccessoriesMenu oldMenu) {
            var currentCarriedStack = oldMenu.getCarried();

            if(!currentCarriedStack.isEmpty()) {
                carriedStack = currentCarriedStack;

                oldMenu.setCarried(ItemStack.EMPTY);
            }
        }

        AccessoriesInternals.openAccessoriesMenu(player, livingEntity, carriedStack);
    }
}