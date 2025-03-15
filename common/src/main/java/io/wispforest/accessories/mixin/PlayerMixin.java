package io.wispforest.accessories.mixin;

import io.wispforest.accessories.pond.DroppedStacksExtension;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.Collection;
import java.util.List;

@Mixin(Player.class)
public abstract class PlayerMixin implements DroppedStacksExtension {

    @Unique
    private Collection<ItemStack> toBeDroppedStacks = List.of();

    @Override
    public void addToBeDroppedStacks(Collection<ItemStack> list) {
        this.toBeDroppedStacks = list;
    }

    @Override
    public Collection<ItemStack> toBeDroppedStacks() {
        return this.toBeDroppedStacks;
    }
}
