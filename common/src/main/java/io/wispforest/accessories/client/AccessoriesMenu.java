package io.wispforest.accessories.client;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.mixin.AbstractContainerMenuAccessor;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;

public class AccessoriesMenu extends InventoryMenu {

    public AccessoriesMenu(int containerId, Inventory inventory) {
        super(inventory, inventory.player.level().isClientSide, inventory.player);

        var accessor = (AbstractContainerMenuAccessor) this;

        accessor.accessories$setMenuType(Accessories.ACCESSORIES_MENU_TYPE);
        accessor.accessories$setContainerId(containerId);


    }
}
