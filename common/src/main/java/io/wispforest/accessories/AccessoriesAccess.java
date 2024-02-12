package io.wispforest.accessories;

import dev.architectury.injectables.annotations.ExpectPlatform;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesHolder;
import io.wispforest.accessories.impl.AccessoriesInternals;
import io.wispforest.accessories.networking.AccessoriesNetworkHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * Util Class implemented though Architectury Plugin allowing for various access to platform specific way
 * of getting class instances
 */
public class AccessoriesAccess {

    /**
     * @return {@link AccessoriesCapability} attached to a given {@link LivingEntity} based on the Platforms method for getting such
     */
    @ExpectPlatform
    public static Optional<AccessoriesCapability> getCapability(LivingEntity livingEntity){
        throw new AssertionError();
    }

    /**
     * @return {@link AccessoriesHolder} attached to a given {@link LivingEntity} based on the Platforms method for getting such
     */
    @ExpectPlatform
    public static AccessoriesHolder getHolder(LivingEntity livingEntity){
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void modifyHolder(LivingEntity livingEntity, UnaryOperator<AccessoriesHolder> modifier){
        throw new AssertionError();
    }

    /**
     * @return {@link AccessoriesNetworkHandler} based on the Platforms method for getting such
     */
    @ExpectPlatform
    public static AccessoriesNetworkHandler getNetworkHandler(){
        throw new AssertionError();
    }

    /**
     * @return An Internal API for accessing various aspects of platform specific code
     */
    @ExpectPlatform
    public static AccessoriesInternals getInternal(){
        throw new AssertionError();
    }
}
