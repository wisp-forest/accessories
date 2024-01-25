package io.wispforest.accessories.client;

import com.mojang.datafixers.util.Pair;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.AccessoriesAccess;
import io.wispforest.accessories.api.SlotGroup;
import io.wispforest.accessories.api.SlotReference;
import io.wispforest.accessories.data.SlotGroupLoader;
import io.wispforest.accessories.mixin.AbstractContainerMenuAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Map;

public class AccessoriesMenu extends InventoryMenu {

    public AccessoriesMenu(int containerId, Inventory inventory) {
        super(inventory, inventory.player.level().isClientSide, inventory.player);

        var accessor = (AbstractContainerMenuAccessor) this;

        accessor.accessories$setMenuType(Accessories.ACCESSORIES_MENU_TYPE);
        accessor.accessories$setContainerId(containerId);

        var groups = SlotGroupLoader.INSTANCE.getGroups(inventory.player.level().isClientSide);

        var api = AccessoriesAccess.getAPI();

        var player = inventory.player;
        var capability = api.getCapability(player);

        if(capability.isPresent()) {
            var containers = capability.get().getContainers();

            int currentY = 40;
            for (var group : groups.values().stream().sorted(Comparator.comparingInt(SlotGroup::order).reversed()).toList()) {
                int currentX = -130;
                for (String slot : group.slots()) {
                    var accessoryContainer = containers.get(slot);

                    if(accessoryContainer == null) continue;

                    var slotType = accessoryContainer.slotType();

                    if(slotType.isEmpty()) continue;

                    var accessories = accessoryContainer.getAccessories();

                    for (int i = 0; i < accessories.getContainerSize(); i++) {
                        var reference = new SlotReference(slot, player, i);

                        this.addSlot(
                                new Slot(accessories, i, currentX, currentY){
                                    @Override
                                    public void set(ItemStack stack) {
                                        var prevStack = this.getItem();

                                        api.getAccessory(prevStack)
                                                .ifPresent(prevAccessory1 -> prevAccessory1.onUnequip(prevStack, reference));

                                        super.set(stack);

                                        api.getAccessory(stack)
                                                .ifPresent(accessory1 -> accessory1.onEquip(stack, reference));

                                        accessoryContainer.markChanged();
                                        accessoryContainer.update();
                                    }

                                    @Override
                                    public int getMaxStackSize() {
                                        // TODO: API TO LIMIT IDK
                                        return super.getMaxStackSize();
                                    }

                                    @Override
                                    public boolean mayPlace(ItemStack stack) {
                                        return api.canInsertIntoSlot(player, reference, stack);
                                    }

                                    @Override
                                    public boolean mayPickup(Player player) {
                                        var stack = this.getItem();
                                        var accessory = api.getAccessory(stack);

                                        return accessory.map(value -> value.canUnequip(stack, reference))
                                                .orElseGet(() -> super.mayPickup(player));
                                    }

                                    @Nullable
                                    @Override
                                    public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                                        // Thanks to mojang you can not access the GUI atlas from this call and you must use Atlases from ModelManager.
                                        // )::::::::::::::::::::::::::::::
                                        return new Pair<>(new ResourceLocation("textures/atlas/blocks.png"), slotType.get().icon());
                                    }

                                    @Override
                                    public boolean allowModification(Player player) {
                                        return true;
                                    }
                                }
                        );

                        currentX += 18;
                    }
                }
                currentY += 18;
            }
        }
    }
}
