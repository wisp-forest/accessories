package io.wispforest.accessories.client;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.AccessoriesAccess;
import io.wispforest.accessories.api.*;
import io.wispforest.accessories.client.gui.AccessoriesSlot;
import io.wispforest.accessories.data.SlotGroupLoader;
import io.wispforest.accessories.mixin.AbstractContainerMenuAccessor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.SimpleContainerData;

import java.util.*;

public class AccessoriesMenu extends InventoryMenu {

    public int totalSlots = 0;
    public boolean overMaxVisibleSlots = false;

    private final ContainerData syncedData;

    public AccessoriesMenu(int containerId, Inventory inventory) {
        super(inventory, inventory.player.level().isClientSide, inventory.player);

        var accessor = (AbstractContainerMenuAccessor) this;

        accessor.accessories$setMenuType(Accessories.ACCESSORIES_MENU_TYPE);
        accessor.accessories$setContainerId(containerId);

        syncedData = new SimpleContainerData(2);

        this.addDataSlots(syncedData);

        var groups = SlotGroupLoader.INSTANCE.getGroups(inventory.player.level().isClientSide);

        var player = inventory.player;
        var capability = AccessoriesAPI.getCapability(player);

        var entitySlotTypes = AccessoriesAPI.getEntitySlots(player);

        int slotScale = 18;

        int minX = -46;
        int maxX = 60;
        int minY = 8;
        int maxY = 152;

        int cosmeticPadding = 2;

        int yOffset = 8;

        Map<SlotType, Set<Accessory>> sortAccessories = new HashMap<>();

        for (var item : BuiltInRegistries.ITEM) {
            var accessory = AccessoriesAPI.getAccessory(item);

            if(accessory.isEmpty()) continue;

//            if(item == Items.APPLE){
//                System.out.println("APPLE");
//            }

            for (var value : entitySlotTypes.values()) {
                if(AccessoriesAPI.canInsertIntoSlot(player, new SlotReference(value.name(), player, 0), item.getDefaultInstance())){
                    sortAccessories.computeIfAbsent(value, s -> new HashSet<>()).add(accessory.get());
                }
            }
        }

        syncedData.set(0, AccessoriesAccess.getHolder(player).cosmeticsShown() ? 1 : 0);

        syncedData.set(1, AccessoriesAccess.getHolder(player).scrolledSlot());

        if(capability.isEmpty()) return;

        var containers = capability.get().getContainers();

        int yIndex = 0;

        groupLoop: for (var group : groups.values().stream().sorted(Comparator.comparingInt(SlotGroup::order).reversed()).toList()) {
            var slotNames = group.slots();

            var slotTypes = slotNames.stream()
                    .flatMap(s -> AccessoriesAPI.getSlotType(player.level(), s).stream())
                    .sorted(Comparator.comparingInt(SlotType::order).reversed())
                    .toList();

            for (SlotType slot : slotTypes) {
                var accessoryContainer = containers.get(slot.name());

                if(accessoryContainer == null) continue;

                var slotType = accessoryContainer.slotType();

                if(slotType.isEmpty()) continue;

                var size = accessoryContainer.getSize();

                if(!overMaxVisibleSlots) {
                    for (int i = 0; i < size; i++) {
                        int currentY = (yIndex * Math.max(18, slotScale)) + minY + yOffset;

                        int currentX = 0 + minX;

                        this.addSlot(
                                new AccessoriesSlot(player, accessoryContainer, true, i, currentX, currentY)
                                        .isActive(this::isCosmeticsOpen)
                        );

                        currentX += slotScale + cosmeticPadding;

                        this.addSlot(
                                new AccessoriesSlot(player, accessoryContainer, false, i, currentX, currentY)
                        );

                        yIndex++;

                        if (currentY + Math.max(18, slotScale) > maxY) {
                            overMaxVisibleSlots = true;
                            break groupLoop;
                        }
                    }
                } else {
                    yIndex += size;
                }
            }
        }

        totalSlots = yIndex;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if(player.level().isClientSide) return true;

        if(id == 0){
            syncedData.set(0, (syncedData.get(0) == 0 ? 1 : 0));

            AccessoriesAccess.modifyHolder(player, holder -> {
                return holder.cosmeticsShown(isCosmeticsOpen());
            });

            return true;
        }

        if(slots.get(id) instanceof AccessoriesSlot slot){
            var renderOptions = slot.container.renderOptions();
            renderOptions.set(slot.getContainerSlot(), !slot.container.shouldRender(slot.getContainerSlot()));
            slot.container.markChanged();
        }

        return super.clickMenuButton(player, id);
    }

    public boolean isCosmeticsOpen(){
        return syncedData.get(0) > 0;
    }
}