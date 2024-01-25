package io.wispforest.accessories.impl;

import com.google.gson.JsonObject;
import dev.architectury.injectables.annotations.ExpectPlatform;
import io.wispforest.accessories.client.AccessoriesMenu;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;

import java.util.Collection;
import java.util.Optional;
import java.util.function.BiFunction;

public interface AccessoriesInternals {

    Collection<ServerPlayer> getTracking(Entity entity);

    void giveItemToPlayer(ServerPlayer player, ItemStack stack);

    boolean isValidOnConditions(JsonObject object);

    default Optional<IEventBus> getBus() {
        return Optional.empty();
    }

    <T extends AbstractContainerMenu> MenuType<T> registerMenuType(ResourceLocation location, BiFunction<Integer, Inventory, T> func);
}
