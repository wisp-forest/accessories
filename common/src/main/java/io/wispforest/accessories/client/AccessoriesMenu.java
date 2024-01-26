package io.wispforest.accessories.client;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.AccessoriesAccess;
import io.wispforest.accessories.api.SlotGroup;
import io.wispforest.accessories.api.SlotType;
import io.wispforest.accessories.client.gui.AccessoriesSlot;
import io.wispforest.accessories.data.SlotGroupLoader;
import io.wispforest.accessories.mixin.AbstractContainerMenuAccessor;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;

import java.util.Comparator;
import java.util.Optional;

public class AccessoriesMenu extends InventoryMenu {

    public boolean isCosmeticsOpen = false;

    public AccessoriesMenu(int containerId, Inventory inventory) {
        super(inventory, inventory.player.level().isClientSide, inventory.player);

        var accessor = (AbstractContainerMenuAccessor) this;

        accessor.accessories$setMenuType(Accessories.ACCESSORIES_MENU_TYPE);
        accessor.accessories$setContainerId(containerId);

        var groups = SlotGroupLoader.INSTANCE.getGroups(inventory.player.level().isClientSide);

        var api = AccessoriesAccess.getAPI();

        var player = inventory.player;
        var capability = api.getCapability(player);

        int slotScale = 18;

        int groupPadding = 4;

        int minX = -46;
        int maxX = 60;
        int minY = 8;
        int maxY = 152;

        if(capability.isPresent()) {
            var containers = capability.get().getContainers();

            int currentY = 0 + 8;
            for (var group : groups.values().stream().sorted(Comparator.comparingInt(SlotGroup::order).reversed()).toList()) {
                var slotNames = group.slots();

                var slotTypes = slotNames.stream()
                        .map(s -> api.getSlotType(player.level(), s))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .sorted(Comparator.comparingInt(SlotType::order).reversed())
                        .toList();

                for (SlotType slot : slotTypes) {
                    var accessoryContainer = containers.get(slot.name());

                    if(accessoryContainer == null) continue;

                    var slotType = accessoryContainer.slotType();

                    if(slotType.isEmpty()) continue;

                    var size = accessoryContainer.getSize();

                    for (int i = 0; i < size; i++) {
                        if ((currentY + Math.max(18, slotScale)) > maxY) {
                            break;
                        }

                        int currentX = 0;

                        this.addSlot(
                                new AccessoriesSlot(player, accessoryContainer, true, i, (currentX) + minX, (currentY) + minY)
                        );

                        currentX += slotScale + 2;

                        this.addSlot(
                                new AccessoriesSlot(player, accessoryContainer, false, i, (currentX) + minX, (currentY) + minY)
                        );

                        currentY += slotScale;
                    }
                }

                if ((currentY + Math.max(18, slotScale)) > maxY) {
                    break;
                } /*else {
                    currentY += groupPadding;
                }*/
            }
        }
    }
}