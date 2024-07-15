package io.wispforest.accessories.menu;

import io.wispforest.accessories.menu.variants.AccessoriesExperimentalMenu;
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
    DEFAULT(() -> AccessoriesMenuTypes.BASE_MENU),
    EXPERIMENTAL_1(() -> AccessoriesMenuTypes.EXPERIMENTAL_MENU);

    public final Supplier<MenuType<? extends AccessoriesMenuBase>> supplier;

    AccessoriesMenuVariant(Supplier<MenuType<? extends AccessoriesMenuBase>> supplier) {
        this.supplier = supplier;
    }

    public static AccessoriesMenuVariant getVariant(MenuType<? extends AccessoriesMenuBase> menuType) {
        for (var value : AccessoriesMenuVariant.values()) {
            if(value.supplier.get().equals(menuType)) return value;
        }

        throw new IllegalArgumentException("Unknown MenuType passed to get Accessories Menu Variant! [Type: " + BuiltInRegistries.MENU.getKey(menuType) + "]");
    }

    public static AbstractContainerMenu openMenu(int i, Inventory inv, AccessoriesMenuVariant variant, @Nullable LivingEntity target, @Nullable ItemStack carriedStack) {
        var menu = switch (variant) {
            case AccessoriesMenuVariant.EXPERIMENTAL_1 -> new AccessoriesExperimentalMenu(i, inv, target);
            case DEFAULT -> new AccessoriesMenu(i, inv, target);
            default -> throw new IllegalArgumentException("Unknown AccessoriesMenuVariant passed to construct Menu! [Variant: " + variant.name() + "]");
        };

        if(carriedStack != null) menu.setCarried(carriedStack);

        return menu;
    }
}
