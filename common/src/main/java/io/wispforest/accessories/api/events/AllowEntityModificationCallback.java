package io.wispforest.accessories.api.events;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.menu.AccessoriesBasedSlot;
import io.wispforest.accessories.api.slot.SlotReference;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Event callback used to allow or denied the ability to adjust a given entities accessories or prevent such Accessory
 * screen being open by the given player. Such is fired within {@link AccessoriesBasedSlot#mayPickup}
 * and within {@link Accessories#openAccessoriesMenu(Player, LivingEntity, ItemStack)}
 */
public interface AllowEntityModificationCallback {

    Event<AllowEntityModificationCallback> EVENT = EventFactory.createArrayBacked(AllowEntityModificationCallback.class,
            (invokers) -> (targetEntity, player, reference) -> {
                TriState returnResult = TriState.DEFAULT;

                for (var invoker : invokers) {
                    returnResult = invoker.allowModifications(targetEntity, player, reference);

                    if(!returnResult.equals(TriState.DEFAULT)) break;
                }

                return returnResult;
            }
    );

    /**
     * @param targetEntity The targeted entity for modification
     * @param player       The specific player
     * @param reference    The reference to the specific location within the Accessories Inventory
     * @return If the given player has the ability to modify the given entity
     */
    TriState allowModifications(LivingEntity targetEntity, Player player, @Nullable SlotReference reference);
}
