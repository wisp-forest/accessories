package io.wispforest.accessories.api.events;

import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.impl.AccessoriesEventHandler;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Event callback used to change if the given entity death will use default accessory behvaior to drop
 * the equipped accessories with such called at {@link AccessoriesEventHandler#onDeath(LivingEntity, DamageSource)}
 */
public interface OnDeathCallback {

    Event<OnDeathCallback> EVENT = EventFactory.createArrayBacked(OnDeathCallback.class,
            (invokers) -> (currentState, entity, capability, damageSource) -> {
                for (var invoker : invokers) {
                    var returnState = invoker.shouldDrop(currentState, entity, capability, damageSource);

                    if(returnState != TriState.DEFAULT) currentState = returnState;
                }

                return currentState;
            }
    );

    /**
     * Event used to check if the given default logic for dropping Accessories should be run or not
     *
     * @param currentState Wether or not someone is attempting to handle the dropping already
     * @param entity The given target entity
     * @param capability The given capability bound to the entity
     * @param damageSource
     * @return If default dropping behavior should occur
     */
    TriState shouldDrop(TriState currentState, LivingEntity entity, AccessoriesCapability capability, DamageSource damageSource);
}
