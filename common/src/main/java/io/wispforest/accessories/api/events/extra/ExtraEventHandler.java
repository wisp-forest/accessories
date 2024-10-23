package io.wispforest.accessories.api.events.extra;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoryRegistry;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Choice Events implemented for use on {@link io.wispforest.accessories.api.Accessory} when needed
 */
public class ExtraEventHandler {

    public static int lootingAdjustments(LivingEntity entity, LootContext context, int currentLevel){
        var damageSource = context.getOptionalParameter(LootContextParams.DAMAGE_SOURCE);

        if(damageSource != null && damageSource.getEntity() instanceof LivingEntity targetEntity){
            var capability = AccessoriesCapability.get(entity);

            if(capability != null){
                for (var entryRef : capability.getAllEquipped()) {
                    var reference = entryRef.reference();
                    var stack = entryRef.stack();

                    var accessory = AccessoryRegistry.getAccessoryOrDefault(stack);

                    currentLevel += io.wispforest.accessories.api.events.extra.LootingAdjustment.EVENT.invoker().getLootingAdjustment(stack, reference, targetEntity, damageSource, currentLevel);

                    currentLevel += io.wispforest.accessories.api.events.extra.v2.LootingAdjustment.EVENT.invoker().getLootingAdjustment(stack, reference, targetEntity, context, damageSource, currentLevel);

                    //--

                    if(accessory instanceof io.wispforest.accessories.api.events.extra.v2.LootingAdjustment lootingAdjustment){
                        currentLevel += lootingAdjustment.getLootingAdjustment(stack, reference, targetEntity, context, damageSource, currentLevel);
                    }
                }
            }
        }

        return currentLevel;
    }

    public static int fortuneAdjustment(LootContext context, int currentLevel){
        if(context.getOptionalParameter(LootContextParams.THIS_ENTITY) instanceof LivingEntity livingEntity) {
            var capability = AccessoriesCapability.get(livingEntity);

            if (capability != null) {
                for (var entryRef : capability.getAllEquipped()) {
                    var reference = entryRef.reference();
                    var stack = entryRef.stack();

                    var accessory = AccessoryRegistry.getAccessoryOrDefault(stack);

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

                var accessory = AccessoryRegistry.getAccessoryOrDefault(stack);

                if(accessory instanceof PiglinNeutralInducer inducer){
                    state = inducer.makePiglinsNeutral(stack, reference);

                    if(state != TriState.DEFAULT) return state;
                }

                state = PiglinNeutralInducer.EVENT.invoker().makePiglinsNeutral(stack, reference);

                if(state != TriState.DEFAULT) return state;

                if (stack.is(ItemTags.PIGLIN_SAFE_ARMOR)) return TriState.TRUE;
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

                var accessory = AccessoryRegistry.getAccessoryOrDefault(stack);

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

    //--

    private static final LoadingCache<Integer, Map<Integer, TriState>> gazeDisguiseCache = CacheBuilder.newBuilder()
            .concurrencyLevel(1)
            .expireAfterAccess(Duration.ofSeconds(1))
            //.maximumSize(1000)
            .weakKeys()
            .build(CacheLoader.from(() -> new HashMap<>()));

    public static TriState isGazedBlocked(boolean vanillaPredicate, LivingEntity lookingEntity, LivingEntity targetEntity){
        var cache = gazeDisguiseCache.getIfPresent(targetEntity.getId());

        if(cache != null && cache.containsKey(lookingEntity.getId())) return cache.get(lookingEntity.getId());

        var state = TriState.DEFAULT;
        var capability = AccessoriesCapability.get(targetEntity);

        if(capability != null) {
            for (var entryRef : capability.getAllEquipped()) {
                var reference = entryRef.reference();
                var stack = entryRef.stack();

                var accessory = AccessoryRegistry.getAccessoryOrDefault(stack);

                if(accessory instanceof IsGazeDisguised masked){
                    state = masked.isWearDisguise(lookingEntity, vanillaPredicate, stack, reference);

                    if(state != TriState.DEFAULT) return state;
                }

                state = IsGazeDisguised.EVENT.invoker().isWearDisguise(lookingEntity, vanillaPredicate, stack, reference);

                if(state != TriState.DEFAULT) return state;

                if (vanillaPredicate && stack.is(ItemTags.GAZE_DISGUISE_EQUIPMENT)) return TriState.TRUE;
            }
        }

        gazeDisguiseCache.getUnchecked(targetEntity.getId())
                .put(lookingEntity.getId(), state);

        return state;
    }
}