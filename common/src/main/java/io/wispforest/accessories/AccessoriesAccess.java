package io.wispforest.accessories;

import dev.architectury.injectables.annotations.ExpectPlatform;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.AccessoriesHolder;
import io.wispforest.accessories.impl.AccessoriesInternals;
import io.wispforest.accessories.networking.AccessoriesNetworkHandler;
import net.minecraft.world.entity.LivingEntity;

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
    public static AccessoriesInternals getInternal(){
        throw new AssertionError();
    }
}
