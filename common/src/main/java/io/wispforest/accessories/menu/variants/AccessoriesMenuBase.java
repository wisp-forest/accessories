package io.wispforest.accessories.menu.variants;

import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.menu.AccessoriesMenuVariant;
import io.wispforest.accessories.networking.server.ScreenOpen;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.Nullable;

public abstract class AccessoriesMenuBase extends AbstractContainerMenu {

    protected final Player owner;

    @Nullable
    protected final LivingEntity targetEntity;

    protected AccessoriesMenuBase(MenuType<? extends AccessoriesMenuBase> menuType, int containerId, Inventory inventory, @Nullable LivingEntity targetEntity) {
        super(menuType, containerId);

        this.owner = inventory.player;
        this.targetEntity = targetEntity;
    }

    public final AccessoriesMenuVariant menuVariant() {
        return AccessoriesMenuVariant.getVariant((MenuType<? extends AccessoriesMenuBase>) this.getType());
    }

    @Nullable
    public final LivingEntity targetEntity() {
        return this.targetEntity;
    }

    public final Player owner() {
        return this.owner;
    }

    public final void reopenMenu() {
        AccessoriesInternals.getNetworkHandler().sendToServer(ScreenOpen.of(this.targetEntity(), this.menuVariant()));
    }
}
