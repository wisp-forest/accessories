package io.wispforest.accessories.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.wispforest.accessories.menu.variants.AccessoriesMenuBase;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
    @Accessor("player")
    public abstract ServerPlayer accessories$player();

    @WrapMethod(
            method = {
                    "handlePlayerCommand",                                                               // Mojmap
                    "method_12045(Lnet/minecraft/class_2641;)V",                                         // Yarn Interm.
                    "onClientCommand(Lnet/minecraft/network/packet/c2s/play/ClientCommandC2SPacket;)V",  // Yarn
            }
    )
    private void accessories$transferStack(ServerboundPlayerCommandPacket packet, Operation<Void> original) {
        PacketUtils.ensureRunningOnSameThread(packet, (ServerGamePacketListenerImpl) (Object) this, this.accessories$player().level());

        var currentContainerMenu = this.accessories$player().containerMenu;

        if (packet.getAction().equals(ServerboundPlayerCommandPacket.Action.OPEN_INVENTORY) && currentContainerMenu instanceof AccessoriesMenuBase) {
            var carriedStack = currentContainerMenu.getCarried();

            currentContainerMenu.setCarried(ItemStack.EMPTY);

            original.call(packet);

            var newMenu = this.accessories$player().containerMenu;

            if (newMenu != currentContainerMenu) {
                if (newMenu.getCarried().isEmpty()) {
                    newMenu.setCarried(carriedStack);
                } else {
                    this.accessories$player().handleExtraItemsCreatedOnUse(carriedStack);
                }
            } else {
                currentContainerMenu.setCarried(carriedStack);
            }
        } else {
            original.call(packet);
        }
    }
}
