package io.wispforest.accessories.fabric;

import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesHolder;
import io.wispforest.accessories.client.AccessoriesMenu;
import io.wispforest.accessories.impl.AccessoriesInternals;
import io.wispforest.accessories.networking.AccessoriesNetworkHandler;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;

import java.util.Collection;
import java.util.Optional;
import java.util.function.UnaryOperator;

public class AccessoriesAccessImpl {

    public static Optional<AccessoriesCapability> getCapability(LivingEntity livingEntity){
        return Optional.ofNullable(AccessoriesFabric.CAPABILITY.find(livingEntity, null));
    }

    public static AccessoriesHolder getHolder(LivingEntity livingEntity){
        return livingEntity.getAttachedOrCreate(AccessoriesFabric.HOLDER_ATTACHMENT_TYPE);
    }

    public static void modifyHolder(LivingEntity livingEntity, UnaryOperator<AccessoriesHolder> modifier){
        var holder = getHolder(livingEntity);

        holder = modifier.apply(holder);

        livingEntity.setAttached(AccessoriesFabric.HOLDER_ATTACHMENT_TYPE, holder);
    }

    public static AccessoriesNetworkHandler getNetworkHandler(){
        return AccessoriesNetworkHandlerImpl.INSTANCE;
    }

    public static AccessoriesInternals getInternal(){
        return AccessoriesInternalsImpl.INSTANCE;
    }

}
