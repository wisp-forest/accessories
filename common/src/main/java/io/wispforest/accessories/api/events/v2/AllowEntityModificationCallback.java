package io.wispforest.accessories.api.events.v2;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.action.ActionResponseBuffer;
import io.wispforest.accessories.api.menu.AccessoriesBasedSlot;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.menu.AccessoriesMenuVariant;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/// Event callback used to allow or denied the ability to adjust a given entities accessories or prevent the Accessory
/// screen being open by the given player.
///
/// Fired in [AccessoriesBasedSlot#mayPickup]
/// and in [Accessories#openAccessoriesMenu(Player, AccessoriesMenuVariant, LivingEntity,ItemStack)]
///
public interface AllowEntityModificationCallback {

    Event<AllowEntityModificationCallback> EVENT = EventFactory.createArrayBacked(AllowEntityModificationCallback.class,
        (invokers) -> (targetEntity, player, reference, buffer) -> {
            for (var invoker : invokers) {
                invoker.allowModifications(targetEntity, player, reference, buffer);

                if(buffer.shouldReturnEarly()) return;
            }
        }
    );

    ///
    /// @param targetEntity The targeted entity for modification
    /// @param player       The specific player
    /// @param reference    The reference to the specific location within the Accessories Inventory
    /// @param buffer       The buffer to send a response to if the Accessory can or can not be equipped
    ///
    void allowModifications(LivingEntity targetEntity, Player player, @Nullable SlotReference reference, ActionResponseBuffer buffer);
}
