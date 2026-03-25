package io.wispforest.accessories.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import io.wispforest.accessories.api.caching.ItemStackBasedPredicate;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

    @ModifyReturnValue(method = "findTotem", at = @At(value = "RETURN", ordinal = 1))
    private static ItemStack accessories$findPossibleTotem(ItemStack original, @Local(argsOnly = true) Player player) {
        var capability = ((io.wispforest.accessories.pond.AccessoriesAPIAccess) player).accessoriesCapability();

        if (capability != null) {
            var totem = capability.getFirstEquipped(ItemStackBasedPredicate.ofComponents("totem_check", DataComponents.DEATH_PROTECTION));

            if (totem != null) {
                original = totem.stack();
            }
        }

        return original;
    }
}
