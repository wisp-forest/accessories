package io.wispforest.accessories.pond;

import io.wispforest.accessories.api.slot.SlotReference;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.effects.EnchantmentLocationBasedEffect;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

public interface AccessoriesLivingEntityExtension {
    void onEquipItem(SlotReference slotReference, ItemStack oldItem, ItemStack newItem);

    void pushEnchantmentContext(ItemStack stack, SlotReference reference);

    @Nullable
    SlotReference popEnchantmentContext(ItemStack stack);

    Map<Enchantment, Set<EnchantmentLocationBasedEffect>> activeLocationDependentEnchantmentsFromSlotReference(SlotReference slotReference);
}
