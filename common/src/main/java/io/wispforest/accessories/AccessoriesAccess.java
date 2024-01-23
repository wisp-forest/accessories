package io.wispforest.accessories;

import dev.architectury.injectables.annotations.ExpectPlatform;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.AccessoriesHolder;
import io.wispforest.accessories.impl.AccessoriesHolderImpl;
import io.wispforest.accessories.networking.AccessoriesNetworkHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Collection;

public class AccessoriesAccess {

    @ExpectPlatform
    public static AccessoriesAPI getAPI() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static AccessoriesHolder getHolder(LivingEntity livingEntity){
        throw new AssertionError();
    }

    @ExpectPlatform
    public static AccessoriesNetworkHandler getHandler(){
        throw new AssertionError();
    }

    @ExpectPlatform
    public static Collection<ServerPlayer> getTracking(Entity entity){
        throw new AssertionError();
    }
}
