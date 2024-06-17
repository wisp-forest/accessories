package io.wispforest.accessories.api;

import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;

/**
 * Simple record object used within {@link Accessory#getEquipSound(ItemStack, SlotReference)} to
 * have a custom equip sound play
 */
public record SoundEventData(Holder<SoundEvent> event, float volume, float pitch) { }
