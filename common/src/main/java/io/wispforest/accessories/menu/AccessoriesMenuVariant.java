package io.wispforest.accessories.menu;

import io.wispforest.accessories.compat.config.ScreenType;
import io.wispforest.accessories.menu.variants.AccessoriesMenu;
import io.wispforest.accessories.menu.variants.AccessoriesMenuBase;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public enum AccessoriesMenuVariant {
    PRIMARY_V2(() -> AccessoriesMenuTypes.PRIAMRY_MENU);

    public final Supplier<MenuType<? extends AccessoriesMenuBase>> supplier;

    AccessoriesMenuVariant(Supplier<MenuType<? extends AccessoriesMenuBase>> supplier) {
        this.supplier = supplier;
    }

    @Nullable
    public static AccessoriesMenuVariant getVariant(ScreenType screenType) {
        return switch (screenType) {
            case PRIMARY_V2 -> PRIMARY_V2;
            default -> null;
        };
    }

    public static AccessoriesMenuVariant getVariant(MenuType<? extends AccessoriesMenuBase> menuType) {
        for (var value : AccessoriesMenuVariant.values()) {
            if(value.supplier.get().equals(menuType)) return value;
        }

        throw new IllegalArgumentException("Unknown MenuType passed to get Accessories Menu Variant! [Type: " + BuiltInRegistries.MENU.getKey(menuType) + "]");
    }

    public static AbstractContainerMenu openMenu(int i, Inventory inv, AccessoriesMenuVariant variant, @Nullable LivingEntity target, @Nullable ItemStack carriedStack) {
        return switch (variant) {
            case AccessoriesMenuVariant.PRIMARY_V2 -> new AccessoriesMenu(i, inv, target, carriedStack);
            default -> throw new IllegalArgumentException("Unknown AccessoriesMenuVariant passed to construct Menu! [Variant: " + variant.name() + "]");
        };
    }
}
