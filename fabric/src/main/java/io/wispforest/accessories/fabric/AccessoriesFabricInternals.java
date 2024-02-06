package io.wispforest.accessories.fabric;

import com.google.gson.JsonObject;
import io.wispforest.accessories.impl.AccessoriesInternals;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.function.BiFunction;

public class AccessoriesFabricInternals implements AccessoriesInternals {

    public static final AccessoriesFabricInternals INSTANCE = new AccessoriesFabricInternals();

    @Override
    public Collection<ServerPlayer> getTracking(Entity entity) {
        return PlayerLookup.tracking(entity);
    }

    @Override
    public void giveItemToPlayer(ServerPlayer player, ItemStack stack) {
        try(var transaction = Transaction.openOuter()) {
            PlayerInventoryStorage.of(player).offerOrDrop(ItemVariant.of(stack), stack.getCount(), transaction);
            transaction.commit();
        }
    }

    @Override
    public boolean isValidOnConditions(JsonObject object) {
        return ResourceConditions.objectMatchesConditions(object);
    }

    @Override
    public <T extends AbstractContainerMenu> MenuType<T> registerMenuType(ResourceLocation location, BiFunction<Integer, Inventory, T> func) {
        return Registry.register(BuiltInRegistries.MENU, location, new MenuType<>(func::apply, FeatureFlags.VANILLA_SET));
    }
}
