package io.wispforest.accessories.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor("SLOT_HIGHLIGHT_FRONT_SPRITE")
    static ResourceLocation accessories$SLOT_HIGHLIGHT_FRONT_SPRITE() { throw new UnsupportedOperationException(); }

    @Accessor("SLOT_HIGHLIGHT_BACK_SPRITE")
    static ResourceLocation accessories$SLOT_HIGHLIGHT_BACK_SPRITE() { throw new UnsupportedOperationException(); }

    @Accessor("leftPos") int accessories$leftPos();

    @Accessor("topPos") int accessories$topPos();
}
