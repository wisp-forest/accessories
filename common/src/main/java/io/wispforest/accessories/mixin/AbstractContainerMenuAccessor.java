package io.wispforest.accessories.mixin;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerMenu.class)
public interface AbstractContainerMenuAccessor {
    @Accessor("menuType")
    @Mutable void accessories$setMenuType(MenuType<?> menuType);

    @Accessor("containerId")
    @Mutable void accessories$setContainerId(int containerId);
}
