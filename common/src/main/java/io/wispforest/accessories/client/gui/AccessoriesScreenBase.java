package io.wispforest.accessories.client.gui;

import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.menu.SlotTypeAccessible;
import io.wispforest.accessories.menu.variants.AccessoriesMenuBase;
import io.wispforest.accessories.networking.AccessoriesNetworking;
import io.wispforest.accessories.impl.option.PlayerOption;
import io.wispforest.accessories.networking.server.ContainerClose;
import io.wispforest.accessories.pond.CloseContainerTransfer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.Slot;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.Nullable;

public interface AccessoriesScreenBase<M extends AccessoriesMenuBase> extends MenuAccess<M> {

    //--

    MutableBoolean FORCE_TOOLTIP_LEFT = new MutableBoolean(false);

    //--

    void onHolderChange(PlayerOption<?> option);

    @Nullable
    SlotTypeAccessible getSelectedSlot();

    @Nullable
    default <S extends SlotTypeAccessible> S getSelectedSlotIf(Class<S> clazz){
        var slot = getSelectedSlot();

        if (slot == null) return null;

        return clazz.isInstance(slot) ? (S) slot : null;
    }

    default LivingEntity targetEntityDefaulted() {
        var targetEntity = this.getMenu().targetEntity();

        return (targetEntity != null) ? targetEntity : Minecraft.getInstance().player;
    }

    default void switchToBaseInventory() {
        this.getMenu().transferAndClose(() -> {
            var player = Minecraft.getInstance().player;

            ((CloseContainerTransfer) player).accessories$setScreenTransfer(new InventoryScreen(player));
            player.closeContainer();
            AccessoriesNetworking.sendToServer(new ContainerClose());
        });
    }
}
