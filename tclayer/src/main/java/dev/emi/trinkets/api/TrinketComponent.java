package dev.emi.trinkets.api;

import com.google.common.collect.Multimap;
import dev.onyxstudios.cca.api.v3.component.ComponentV3;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

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
        return isEquipped(stack -> stack.getItem() == item);
    }

    /**
     * @return All slots that match the provided predicate
     */
    List<Tuple<SlotReference, ItemStack>> getEquipped(Predicate<ItemStack> predicate);

    /**
     * @return All slots that contain the provided item
     */
    default List<Tuple<SlotReference, ItemStack>> getEquipped(Item item) {
        return getEquipped(stack -> stack.getItem() == item);
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
