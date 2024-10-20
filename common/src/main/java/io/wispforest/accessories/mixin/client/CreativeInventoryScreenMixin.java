package io.wispforest.accessories.mixin.client;

import io.wispforest.accessories.client.gui.components.ComponentUtils;
import io.wispforest.accessories.networking.AccessoriesNetworking;
import io.wispforest.accessories.networking.server.NukeAccessories;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeInventoryScreenMixin extends EffectRenderingInventoryScreen<CreativeModeInventoryScreen.ItemPickerMenu> implements ComponentUtils.CreativeScreenExtension {

    @Shadow private static CreativeModeTab selectedTab;

    public CreativeInventoryScreenMixin(CreativeModeInventoryScreen.ItemPickerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Unique private int nukeCoolDown = 0;

    @Inject(method = "containerTick", at = @At("HEAD"))
    private void nukeCooldown(CallbackInfo ci){
        if(this.nukeCoolDown > 0) this.nukeCoolDown--;
    }

    @Inject(method = "selectTab", at = @At(value = "TAIL"))
    private void adjustAccessoryButton(CreativeModeTab tab, CallbackInfo ci){
        getEvent().invoker().onTabChange(tab);
    }

    @Inject(method = "slotClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/NonNullList;size()I", ordinal = 0, shift = At.Shift.BEFORE))
    private void clearAccessoriesWithClearSlot(Slot slot, int slotId, int mouseButton, ClickType type, CallbackInfo ci) {
        if(this.nukeCoolDown <= 0) {
            AccessoriesNetworking.sendToServer(new NukeAccessories());

            this.nukeCoolDown = 10;
        }
    }

    //--

    @Unique
    private final Event<ComponentUtils.OnCreativeTabChange> onTabChangeEvent = EventFactory.createArrayBacked(ComponentUtils.OnCreativeTabChange.class, invokers -> (tab) -> {
        for (var invoker : invokers) invoker.onTabChange(tab);
    });

    @Override
    public Event<ComponentUtils.OnCreativeTabChange> getEvent() {
        return this.onTabChangeEvent;
    }

    @Override
    public CreativeModeTab getTab() {
        return selectedTab;
    }
}
