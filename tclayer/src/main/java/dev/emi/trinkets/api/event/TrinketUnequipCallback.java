/*
 * MIT License
 *
 * Copyright (c) 2019 Emily Rose Ploszaj
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package dev.emi.trinkets.api.event;

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.Trinket;
import dev.emi.trinkets.compat.WrappingTrinketsUtils;
import io.wispforest.accessories.api.events.AccessoryChangeCallback;
import io.wispforest.accessories.api.events.SlotStateChange;
import io.wispforest.accessories.impl.event.WrappedEvent;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public interface TrinketUnequipCallback {
    Event<TrinketUnequipCallback> EVENT = new WrappedEvent<>(AccessoryChangeCallback.EVENT, callback -> {
        return (prevStack, currentStack, reference, stateChange) -> {
            var slotReference = WrappingTrinketsUtils.createTrinketsReference(reference);

            if(slotReference.isEmpty()) return;

            callback.onUnequip(prevStack, slotReference.get(), reference.entity());
        };
    }, accessoryChangeCallbackEvent -> (stack, slot, entity) -> {
        var ref = WrappingTrinketsUtils.createAccessoriesReference(slot).get();

        accessoryChangeCallbackEvent.invoker().onChange(stack, ref.getStack(), ref, SlotStateChange.REPLACEMENT);
    });

    /**
     * Called when an entity un-equips a trinket, after the {@link Trinket#onUnequip} method of the Trinket
     *
     * @param stack The stack being unequipped
     * @param slot The slot the stack was unequipped from
     * @param entity The entity that unequipped the stack
     */
    void onUnequip(ItemStack stack, SlotReference slot, LivingEntity entity);
}
