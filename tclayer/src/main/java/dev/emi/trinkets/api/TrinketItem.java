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
package dev.emi.trinkets.api;

import dev.emi.trinkets.TrinketSlot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * A convenient base class for trinket items that automatically registers itself and
 */
public class TrinketItem extends Item implements Trinket {

    public TrinketItem(Properties settings) {
        super(settings);
        TrinketsApi.registerTrinket(this, this);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
//        ItemStack stack = user.getItemInHand(hand);
//        if (equipItem(user, stack)) {
//            return InteractionResultHolder.sidedSuccess(stack, world.isClientSide());
//        }
        return super.use(world, user, hand);
    }

    public static boolean equipItem(Player user, ItemStack stack) {
        return equipItem((LivingEntity) user, stack);
    }

    public static boolean equipItem(LivingEntity user, ItemStack stack) {
        var optional = TrinketsApi.getTrinketComponent(user);
        if (optional.isPresent()) {
            TrinketComponent comp = optional.get();
            for (var group : comp.getInventory().values()) {
                for (TrinketInventory inv : group.values()) {
                    for (int i = 0; i < inv.getContainerSize(); i++) {
                        if (inv.getItem(i).isEmpty()) {
                            SlotReference ref = new SlotReference(inv, i);
                            if (TrinketSlot.canInsert(stack, ref, user)) {
                                ItemStack newStack = stack.copy();
                                inv.setItem(i, newStack);
                                Trinket trinket = TrinketsApi.getTrinket(stack.getItem());
                                var soundEvent = trinket.getEquipSound(stack, ref, user);
                                if (!stack.isEmpty() && soundEvent != null) {
                                    user.gameEvent(GameEvent.EQUIP);
                                    user.playSound(soundEvent.value(), 1.0F, 1.0F);
                                }
                                stack.setCount(0);
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
}