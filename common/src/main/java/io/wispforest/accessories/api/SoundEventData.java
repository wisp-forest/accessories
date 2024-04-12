package io.wispforest.accessories.api;

import net.minecraft.sounds.SoundEvent;

public record SoundEventData(SoundEvent event, float volume, float pitch) { }
