package io.wispforest.accessories.api.events.extra;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.AccessoriesCapability;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Choice Events implemented for use on {@link io.wispforest.accessories.api.Accessory} when needed
 */
public class ImplementedEvents {

    public static int lootingAdjustments(LivingEntity entity, DamageSource damageSource, int currentLevel){
        if(damageSource != null && damageSource.getEntity() instanceof LivingEntity targetEntity){
            var capability = AccessoriesCapability.get(entity);

            if(capability != null){
                for (var entryRef : capability.getAllEquipped()) {
                    var reference = entryRef.reference();
                    var stack = entryRef.stack();

                    var accessory = AccessoriesAPI.getAccessory(stack);

                    if(accessory instanceof LootingAdjustment lootingAdjustment){
                        currentLevel += lootingAdjustment.getLootingAdjustment(stack, reference, targetEntity, damageSource, currentLevel);
                    }

                    currentLevel += LOOTING_ADJUSTMENT_EVENT.invoker().getLootingAdjustment(stack, reference, targetEntity, damageSource, currentLevel);
                }
            }
        }

        return currentLevel;
    }

    /**
     * Event used to adjust the looting level of a given {@link LivingEntity} death within
     * the {@link LivingEntity#dropAllDeathLoot} call based on the Accessory passed in
     */
    public static final Event<LootingAdjustment> LOOTING_ADJUSTMENT_EVENT = EventFactory.createArrayBacked(LootingAdjustment.class, invokers -> (stack, reference, target, damageSource, currentLevel) -> {
        for (var invoker : invokers) {
            currentLevel += invoker.getLootingAdjustment(stack, reference, target, damageSource, currentLevel);
        }

        return currentLevel;
    });

    //--

    public static int fortuneAdjustment(LootContext context, int currentLevel){
        var entity = context.getParamOrNull(LootContextParams.THIS_ENTITY);

        if(!(entity instanceof LivingEntity livingEntity)) return currentLevel;

        var capability = AccessoriesCapability.get(livingEntity);

        if(capability != null){
            for (var entryRef : capability.getAllEquipped()) {
                var reference = entryRef.reference();
                var stack = entryRef.stack();

                var accessory = AccessoriesAPI.getAccessory(stack);

                if(accessory instanceof FortuneAdjustment fortuneAdjustment){
                    currentLevel += fortuneAdjustment.getFortuneAdjustment(stack, reference, context, currentLevel);
                }

                currentLevel += FORTUNE_ADJUSTMENT_EVENT.invoker().getFortuneAdjustment(stack, reference, context, currentLevel);
            }
        }

        return currentLevel;
    }

    /**
     * Event used to adjust the fortune level of a given Bonus Count adjustment for any valid {@link ApplyBonusCount#run} call
     */
    public static final Event<FortuneAdjustment> FORTUNE_ADJUSTMENT_EVENT = EventFactory.createArrayBacked(FortuneAdjustment.class, invokers -> (stack, reference, context, currentLevel) -> {
        for (var invoker : invokers) {
            currentLevel += invoker.getFortuneAdjustment(stack, reference, context, currentLevel);
        }

        return currentLevel;
    });

    //--

    public static TriState isPiglinsNeutral(LivingEntity entity){
        var state = TriState.DEFAULT;

        var capability = AccessoriesCapability.get(entity);

        if(capability != null){
            for (var entryRef : capability.getAllEquipped()) {
                var reference = entryRef.reference();
                var stack = entryRef.stack();

                var accessory = AccessoriesAPI.getAccessory(stack);

                if(accessory instanceof PiglinNeutralInducer inducer){
                    state = inducer.makesPiglinsNeutral(stack, reference);

                    if(state != TriState.DEFAULT) return state;
                }

                state = PIGLIN_NEUTRAL_INDUCER_EVENT.invoker().makesPiglinsNeutral(stack, reference);

                if(state != TriState.DEFAULT) return state;
            }
        }

        return state;
    }

    /**
     * Event used to test if the given {@link LivingEntity} will have piglins be
     * neutral or not based on the passed Accessory
     */
    public static final Event<PiglinNeutralInducer> PIGLIN_NEUTRAL_INDUCER_EVENT = EventFactory.createArrayBacked(PiglinNeutralInducer.class, invokers -> (stack, reference) -> {
        for (var invoker : invokers) {
            var state = invoker.makesPiglinsNeutral(stack, reference);

            if(state != TriState.DEFAULT) return state;
        }

        return TriState.DEFAULT;
    });

    //--

    public static TriState allowWalkingOnSnow(LivingEntity entity){
        var state = TriState.DEFAULT;

        var capability = AccessoriesCapability.get(entity);

        if(capability != null){
            for (var entryRef : capability.getAllEquipped()) {
                var reference = entryRef.reference();
                var stack = entryRef.stack();

                var accessory = AccessoriesAPI.getAccessory(stack);

                if(accessory instanceof AllowWalingOnSnow event){
                    state = event.allowWalkingOnSnow(stack, reference);

                    if(state != TriState.DEFAULT) return state;
                }

                state = ALLOW_WALING_ON_SNOW_EVENT.invoker().allowWalkingOnSnow(stack, reference);

                if(state != TriState.DEFAULT) return state;
            }
        }

        return state;
    }

    /**
     * Event used to test if the given {@link LivingEntity} will have the ability
     * to walk on snow or not based on the passed Accessory
     */
    public static final Event<AllowWalingOnSnow> ALLOW_WALING_ON_SNOW_EVENT = EventFactory.createArrayBacked(AllowWalingOnSnow.class, invokers -> (stack, reference) -> {
        for (var invoker : invokers) {
            var state = invoker.allowWalkingOnSnow(stack, reference);

            if(state != TriState.DEFAULT) return state;
        }

        return TriState.DEFAULT;
    });

    //--

    private static final LoadingCache<Integer, Map<Integer, TriState>> endermanAngyCacheResults = CacheBuilder.newBuilder()
            .concurrencyLevel(1)
            .expireAfterAccess(Duration.ofSeconds(1))
            //.maximumSize(1000)
            .weakKeys()
            .build(CacheLoader.from(() -> new HashMap<>()));

    public static TriState isEndermanMask(LivingEntity entity, EnderMan enderMan){
        var state = TriState.DEFAULT;

        var cache = endermanAngyCacheResults.getIfPresent(entity.getId());

        if(cache != null && cache.containsKey(enderMan.getId())){
            return cache.get(enderMan.getId());
        }

        var capability = AccessoriesCapability.get(entity);

        if(capability != null) {
            for (var entryRef : capability.getAllEquipped()) {
                var reference = entryRef.reference();
                var stack = entryRef.stack();

                var accessory = AccessoriesAPI.getAccessory(stack);

                if(accessory instanceof EndermanMasked masked){
                    state = masked.isEndermanMasked(enderMan, stack, reference);

                    if(state != TriState.DEFAULT) return state;
                }

                state = ENDERMAN_MASKED_EVENT.invoker().isEndermanMasked(enderMan, stack, reference);

                if(state != TriState.DEFAULT) return state;
            }
        }

        endermanAngyCacheResults.getUnchecked(entity.getId())
                .put(enderMan.getId(), state);

        return state;
    }

//    public static void clearEndermanAngryCache(){
//        for (var map : endermanAngyCacheResults.values()) map.clear();
//    }

    /**
     * Event used to test if the given {@link LivingEntity} will anger
     * any enderman or not based on the passed Accessory
     */
    public static final Event<EndermanMasked> ENDERMAN_MASKED_EVENT = EventFactory.createArrayBacked(EndermanMasked.class, invokers -> (enderMan, stack, reference) -> {
        for (var invoker : invokers) {
            var state = invoker.isEndermanMasked(enderMan, stack, reference);

            if(state != TriState.DEFAULT) return state;
        }

        return TriState.DEFAULT;
    });

}