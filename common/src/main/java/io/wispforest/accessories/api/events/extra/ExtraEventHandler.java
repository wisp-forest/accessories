package io.wispforest.accessories.api.events.extra;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.AccessoriesCapability;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Choice Events implemented for use on {@link io.wispforest.accessories.api.Accessory} when needed
 */
public class ExtraEventHandler {

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

                    currentLevel += LootingAdjustment.EVENT.invoker().getLootingAdjustment(stack, reference, targetEntity, damageSource, currentLevel);
                }
            }
        }

        return currentLevel;
    }

    public static int fortuneAdjustment(LootContext context, int currentLevel){
        if(context.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof LivingEntity livingEntity) {
            var capability = AccessoriesCapability.get(livingEntity);

            if (capability != null) {
                for (var entryRef : capability.getAllEquipped()) {
                    var reference = entryRef.reference();
                    var stack = entryRef.stack();

                    var accessory = AccessoriesAPI.getAccessory(stack);

                    if (accessory instanceof FortuneAdjustment fortuneAdjustment) {
                        currentLevel += fortuneAdjustment.getFortuneAdjustment(stack, reference, context, currentLevel);
                    }

                    currentLevel += FortuneAdjustment.EVENT.invoker().getFortuneAdjustment(stack, reference, context, currentLevel);
                }
            }
        }

        return currentLevel;
    }

    public static TriState isPiglinsNeutral(LivingEntity entity){
        var state = TriState.DEFAULT;

        var capability = AccessoriesCapability.get(entity);

        if(capability != null){
            for (var entryRef : capability.getAllEquipped()) {
                var reference = entryRef.reference();
                var stack = entryRef.stack();

                var accessory = AccessoriesAPI.getAccessory(stack);

                if(accessory instanceof PiglinNeutralInducer inducer){
                    state = inducer.makePiglinsNeutral(stack, reference);

                    if(state != TriState.DEFAULT) return state;
                }

                state = PiglinNeutralInducer.EVENT.invoker().makePiglinsNeutral(stack, reference);

                if(state != TriState.DEFAULT) return state;
            }
        }

        return state;
    }

    public static TriState allowWalkingOnSnow(LivingEntity entity){
        var state = TriState.DEFAULT;

        var capability = AccessoriesCapability.get(entity);

        if(capability != null){
            for (var entryRef : capability.getAllEquipped()) {
                var reference = entryRef.reference();
                var stack = entryRef.stack();

                var accessory = AccessoriesAPI.getAccessory(stack);

                if(accessory instanceof AllowWalkingOnSnow event){
                    state = event.allowWalkingOnSnow(stack, reference);

                    if(state != TriState.DEFAULT) return state;
                }

                state = AllowWalkingOnSnow.EVENT.invoker().allowWalkingOnSnow(stack, reference);

                if(state != TriState.DEFAULT) return state;
            }
        }

        return state;
    }

    private static final LoadingCache<Integer, Map<Integer, TriState>> endermanAngyCacheResults = CacheBuilder.newBuilder()
            .concurrencyLevel(1)
            .expireAfterAccess(Duration.ofSeconds(1))
            //.maximumSize(1000)
            .weakKeys()
            .build(CacheLoader.from(() -> new HashMap<>()));

    public static TriState isEndermanMask(LivingEntity entity, EnderMan enderMan){
        var cache = endermanAngyCacheResults.getIfPresent(entity.getId());

        if(cache != null && cache.containsKey(enderMan.getId())) return cache.get(enderMan.getId());

        var state = TriState.DEFAULT;
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

                state = EndermanMasked.EVENT.invoker().isEndermanMasked(enderMan, stack, reference);

                if(state != TriState.DEFAULT) return state;
            }
        }

        endermanAngyCacheResults.getUnchecked(entity.getId())
                .put(enderMan.getId(), state);

        return state;
    }
}