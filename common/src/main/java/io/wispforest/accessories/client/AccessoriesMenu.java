package io.wispforest.accessories.client;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;

public class AccessoriesMenu extends InventoryMenu {
    public AccessoriesMenu(Inventory playerInventory, boolean active, Player owner) {
        super(playerInventory, active, owner);
    }
}
