package io.wispforest.accessories.client.gui;

import io.wispforest.accessories.impl.option.PlayerOption;
import io.wispforest.accessories.menu.SlotTypeAccessible;
import io.wispforest.accessories.menu.variants.AccessoriesMenuBase;
import io.wispforest.accessories.mixin.client.DefaultTooltipPositionerMixin;
import io.wispforest.accessories.networking.AccessoriesNetworking;
import io.wispforest.accessories.networking.server.ContainerClose;
import io.wispforest.accessories.pond.CloseContainerTransfer;
import io.wispforest.accessories.pond.DefaultTooltipPositionerExt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.world.entity.LivingEntity;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;

import java.util.function.UnaryOperator;

public interface AccessoriesScreenBase<M extends AccessoriesMenuBase> extends MenuAccess<M> {

    //--

    MutableObject<DefaultTooltipPositionerExt.@Nullable PositionAdjuster> ALTERATIVE_POSITIONER = new MutableObject<>();

    static void setPositioner(boolean forceLeftTooltip) {
        var positioner = ALTERATIVE_POSITIONER.getValue();

        if (forceLeftTooltip && positioner == null) {
            ALTERATIVE_POSITIONER.setValue((screenWidth, tooltipPos, tooltipWidth) -> {
                if (tooltipPos.x + tooltipWidth <= screenWidth) tooltipPos.x = Math.max(tooltipPos.x - 24 - tooltipWidth, 4);
            });
        } else if (positioner != null) {
            ALTERATIVE_POSITIONER.setValue(null);
        }
    }

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
