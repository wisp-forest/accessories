package io.wispforest.accessories.neoforge;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import io.wispforest.accessories.impl.AccessoriesInternals;
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
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

public class AccessoriesForgeInternals implements AccessoriesInternals {

    public static final AccessoriesForgeInternals INSTANCE = new AccessoriesForgeInternals();

    @Override
    public Collection<ServerPlayer> getTracking(Entity entity) {
        return List.of();
    }

    @Override
    public void giveItemToPlayer(ServerPlayer player, ItemStack stack) {
        ItemHandlerHelper.giveItemToPlayer(player, stack);
    }

    @Override
    public boolean isValidOnConditions(JsonObject object) {
        return ICondition.conditionsMatched(JsonOps.INSTANCE, object);
    }

    @Override
    public <T extends AbstractContainerMenu> MenuType<T> registerMenuType(ResourceLocation location, BiFunction<Integer, Inventory, T> func) {
        return Registry.register(BuiltInRegistries.MENU, location, new MenuType<>(func::apply, FeatureFlags.VANILLA_SET));
    }
}
