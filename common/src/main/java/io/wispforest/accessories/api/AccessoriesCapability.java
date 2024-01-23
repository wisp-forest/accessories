package io.wispforest.accessories.api;

import com.google.common.collect.Multimap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

public interface AccessoriesCapability extends InstanceCodecable {

    LivingEntity getEntity();

    void clear();

    Map<String, AccessoriesContainer> getContainers();

    //--

    void addTransientSlotModifiers(Multimap<String, AttributeModifier> modifiers);

    void addPersistentSlotModifiers(Multimap<String, AttributeModifier> modifiers);

    void removeSlotModifiers(Multimap<String, AttributeModifier> modifiers);

    Multimap<String, AttributeModifier> getSlotModifiers();

    void clearSlotModifiers();

    void clearCachedSlotModifiers();

    //--
    boolean equipAccessory(ItemStack stack);

    //--

    boolean isEquipped(Predicate<ItemStack> predicate);

    default boolean isEquipped(Item item){
        return isEquipped(stack -> stack.getItem() == item);
    }

    List<SlotEntryReference> getEquipped(Predicate<ItemStack> predicate);

    default List<SlotEntryReference> getEquipped(Item item){
        return getEquipped(stack -> stack.getItem() == item);
    }

    List<SlotEntryReference> getAllEquipped();

    //--

    void foreach(Consumer<SlotEntryReference> consumer);
}
