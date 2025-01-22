package io.wispforest.accessories.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.inventory.HorseInventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HorseInventoryMenu.class)
public interface HorseInventoryMenuAccessor {

    @Accessor("SADDLE_SLOT_SPRITE")
    static ResourceLocation accessories$SADDLE_SLOT_SPRITE() {
        throw new AssertionError();
    }

    @Accessor("LLAMA_ARMOR_SLOT_SPRITE")
    static ResourceLocation accessories$LLAMA_ARMOR_SLOT_SPRITE() {
        throw new AssertionError();
    }

    @Accessor("horse")
    AbstractHorse accessories$horse();
}
