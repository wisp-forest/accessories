package io.wispforest.accessories.client.gui;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.menu.AccessoriesBasedSlot;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class AccessoriesInternalSlot extends AccessoriesBasedSlot {

    public final int menuIndex;

    public final boolean isCosmetic;

    private Function<AccessoriesInternalSlot, Boolean> isActive = (slot) -> true;
    private Function<AccessoriesInternalSlot, Boolean> isAccessible = (slot) -> true;

    public AccessoriesInternalSlot(int menuIndex, AccessoriesContainer container, boolean isCosmetic, int slot, int x, int y) {
        super(container, isCosmetic ? container.getCosmeticAccessories() : container.getAccessories(), slot, x, y);

        this.menuIndex = menuIndex;

        this.isCosmetic = isCosmetic;
    }

    public AccessoriesInternalSlot isActive(Function<AccessoriesInternalSlot, Boolean> isActive){
        this.isActive = isActive;

        return this;
    }

    public AccessoriesInternalSlot isAccessible(Function<AccessoriesInternalSlot, Boolean> isAccessible){
        this.isAccessible = isAccessible;

        return this;
    }

    @Override
    protected ResourceLocation icon() {
        return (this.isCosmetic) ? Accessories.of("gui/slot/cosmetic") : super.icon();
    }

    public List<Component> getTooltipData() {
        List<Component> tooltipData = new ArrayList<>();

        var key = this.isCosmetic ? "cosmetic_" : "";

        var slotType = this.accessoriesContainer.slotType();

        tooltipData.add(Component.translatable(Accessories.translation(key + "slot.tooltip.singular"))
                .withStyle(ChatFormatting.GRAY)
                .append(Component.translatable(slotType.translation()).withStyle(ChatFormatting.BLUE)));

        return tooltipData;
    }

    @Override
    public void set(ItemStack stack) {
        var prevStack = this.getItem();

        super.set(stack);

        // TODO: SHOULD THIS BE HERE?
//        if(isCosmetic) {
//            var reference = new SlotReference(container.getSlotName(), entity, getContainerSlot());
//
//            AccessoriesAPI.getAccessory(prevStack)
//                    .ifPresent(prevAccessory1 -> prevAccessory1.onUnequip(prevStack, reference));
//
//            AccessoriesAPI.getAccessory(stack)
//                    .ifPresent(accessory1 -> accessory1.onEquip(stack, reference));
//        }
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return this.isAccessible.apply(this) && super.mayPlace(stack);
    }

    @Override
    public boolean mayPickup(Player player) {
        return this.isAccessible.apply(this) && (isCosmetic || super.mayPickup(player));
    }

    @Override
    public boolean allowModification(Player player) {
        return this.isAccessible.apply(this) && super.allowModification(player);
    }

    @Override
    public boolean isActive() {
        return this.isActive.apply(this);
    }
}
