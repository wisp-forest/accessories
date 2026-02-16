package io.wispforest.accessories.api.events;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.action.ActionResponseBuffer;
import io.wispforest.accessories.api.action.ValidationState;
import io.wispforest.accessories.api.menu.AccessoriesBasedSlot;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.impl.core.UnknownResponse;
import io.wispforest.accessories.impl.event.WrappedEvent;
import io.wispforest.accessories.menu.AccessoriesMenuVariant;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Event callback used to allow or denied the ability to adjust a given entities accessories or prevent the Accessory
 * screen being open by the given player.
 * <p>
 * Fired in {@link AccessoriesBasedSlot#mayPickup}
 * and in {@link Accessories#openAccessoriesMenu(Player, AccessoriesMenuVariant, LivingEntity,ItemStack)}
 */
@Deprecated
public interface AllowEntityModificationCallback {

    Event<AllowEntityModificationCallback> EVENT = new WrappedEvent<>(
        io.wispforest.accessories.api.events.v2.AllowEntityModificationCallback.EVENT,
        callback -> {
            return (targetEntity, player, ref, buffer) -> {
                var result = callback.allowModifications(targetEntity, player, ref);

                buffer.respondWith(new UnknownResponse(ValidationState.of(result)));
            };
        }, event -> {
            return (targetEntity, player, reference) -> {
                var buffer = new ActionResponseBuffer(true);

                event.invoker().allowModifications(targetEntity, player, reference, buffer);

                return buffer.canPerformAction().toTriState();
            };
        });

    /**
     * @param targetEntity The targeted entity for modification
     * @param player       The specific player
     * @param reference    The reference to the specific location within the Accessories Inventory
     * @return If the given player has the ability to modify the given entity
     */
    TriState allowModifications(LivingEntity targetEntity, Player player, @Nullable SlotReference reference);
}
