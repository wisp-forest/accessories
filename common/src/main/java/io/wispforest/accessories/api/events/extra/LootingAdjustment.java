package io.wispforest.accessories.api.events.extra;

import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.impl.event.WrappedEvent;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.Optional;

/**
 * @deprecated Use {@link io.wispforest.accessories.api.events.extra.v2.LootingAdjustment} instead!
 */
@Deprecated(forRemoval = true)
public interface LootingAdjustment {

    @Deprecated(forRemoval = true)
    Event<LootingAdjustment> EVENT = new WrappedEvent<>(
            io.wispforest.accessories.api.events.extra.v2.LootingAdjustment.EVENT,
            (adjustment) -> (stack, reference, target, context, damageSource, currentLevel) -> adjustment.getLootingAdjustment(stack, reference, target, damageSource, currentLevel),
            lootingAdjustmentEvent -> {
                return (stack, reference, target, damageSource, currentLevel) -> {
                    var contextBuilder = new LootContext.Builder(
                            new LootParams.Builder((ServerLevel) reference.entity().level())
                                    .withParameter(LootContextParams.THIS_ENTITY, reference.entity())
                                    .withParameter(LootContextParams.ORIGIN, reference.entity().position())
                                    .withParameter(LootContextParams.DAMAGE_SOURCE, damageSource)
                                    .withParameter(LootContextParams.ATTACKING_ENTITY, target)
                                    .create(LootContextParamSets.ENTITY)
                    );

                    return lootingAdjustmentEvent.invoker().getLootingAdjustment(stack, reference, target, contextBuilder.create(Optional.empty()), damageSource, currentLevel);
                };
            }
    );

    int getLootingAdjustment(ItemStack stack, SlotReference reference, LivingEntity target, DamageSource damageSource, int currentLevel);
}
