package top.theillusivec4.curios.mixin.core;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import javax.annotation.Nullable;

@Mixin(AbstractContainerMenu.class)
public interface AbstractContainerMenuAccessor {
    @Mutable
    @Accessor
    void setMenuType(MenuType<?> menuType);
}
