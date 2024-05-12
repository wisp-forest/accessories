/*
 * Copyright (c) 2018-2023 C4
 *
 * This file is part of Curios, a mod made for Minecraft.
 *
 * Curios is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Curios is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Curios.  If not, see <https://www.gnu.org/licenses/>.
 */

package top.theillusivec4.curios.api;

import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public class SlotResult {
    private final SlotContext slotContext;
    private final ItemStack stack;

    public SlotResult(SlotContext slotContext, ItemStack stack) {
        this.slotContext = slotContext;
        this.stack = stack;
    }

    public SlotContext slotContext() {
        return slotContext;
    }

    public ItemStack stack() {
        return stack;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (SlotResult) obj;
        return Objects.equals(this.slotContext, that.slotContext) &&
                Objects.equals(this.stack, that.stack);
    }

    @Override
    public int hashCode() {
        return Objects.hash(slotContext, stack);
    }

    @Override
    public String toString() {
        return "SlotResult[" +
                "slotContext=" + slotContext + ", " +
                "stack=" + stack + ']';
    }

}
