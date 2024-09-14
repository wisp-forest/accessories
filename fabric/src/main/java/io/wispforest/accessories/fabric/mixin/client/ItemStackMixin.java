package io.wispforest.accessories.fabric.mixin.client;

import io.wispforest.accessories.impl.AccessoriesEventHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = ItemStack.class, priority = 1001)
public abstract class ItemStackMixin {

    @Inject(method = "getTooltipLines", at = @At(value = "RETURN", ordinal = 1))
    private void getTooltip(Item.TooltipContext tooltipContext, @Nullable Player player, TooltipFlag tooltipType, CallbackInfoReturnable<List<Component>> info) {
        var tooltipData = new ArrayList<Component>();

        AccessoriesEventHandler.getTooltipData(player, (ItemStack) (Object) this, tooltipData, tooltipContext, tooltipType);

        if(!tooltipData.isEmpty()) info.getReturnValue().addAll(1, tooltipData);
    }
}
