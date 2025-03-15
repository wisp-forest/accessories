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

import com.google.common.collect.Multimap;
import io.wispforest.accessories.api.caching.ItemStackBasedPredicate;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.ladysnake.cca.api.v3.component.ComponentV3;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public interface TrinketComponent extends ComponentV3 {

    LivingEntity getEntity();

    /**
     * @return A map of names to slot groups available to the entity
     */
    Map<String, SlotGroup> getGroups();

    /**
     * @return A map of slot group names, to slot names, to trinket inventories
     * for the entity. Inventories will respect EAM slot count modifications for
     * the entity.
     */
    Map<String, Map<String, TrinketInventory>> getInventory();

    void update();

    void addTemporaryModifiers(Multimap<String, AttributeModifier> modifiers);

    void addPersistentModifiers(Multimap<String, AttributeModifier> modifiers);

    void removeModifiers(Multimap<String, AttributeModifier> modifiers);

    void clearModifiers();

    Multimap<String, AttributeModifier> getModifiers();

    /**
     * @return Whether the predicate matches any slots available to the entity
     */
    boolean isEquipped(Predicate<ItemStack> predicate);

    /**
     * @return Whether the item is in any slots available to the entity
     */
    default boolean isEquipped(Item item) {
        return isEquipped(ItemStackBasedPredicate.ofItem(item));
    }

    /**
     * @return All slots that match the provided predicate
     */
    List<Tuple<SlotReference, ItemStack>> getEquipped(Predicate<ItemStack> predicate);

    /**
     * @return All slots that contain the provided item
     */
    default List<Tuple<SlotReference, ItemStack>> getEquipped(Item item) {
        return getEquipped(ItemStackBasedPredicate.ofItem(item));
    }

    /**
     * @return All non-empty slots
     */
    default List<Tuple<SlotReference, ItemStack>> getAllEquipped() {
        return getEquipped(stack -> !stack.isEmpty());
    }

    /**
     * Iterates over every slot available to the entity
     */
    void forEach(BiConsumer<SlotReference, ItemStack> consumer);

    Set<TrinketInventory> getTrackingUpdates();

    void clearCachedModifiers();
}
