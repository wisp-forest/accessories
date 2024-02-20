package io.wispforest.accessories.api.events.extra;

import com.mojang.blaze3d.platform.Window;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoryNest;
import io.wispforest.accessories.api.SlotReference;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.HashMap;
import java.util.Map;

/**
 * Choice Events implemented for use on {@link io.wispforest.accessories.api.Accessory} when needed
 */
public class ImplementedEvents {

    public static int lootingAdjustments(LivingEntity entity, DamageSource damageSource, int currentLevel){
        if(damageSource != null && damageSource.getEntity() instanceof LivingEntity targetEntity){
            var capability = AccessoriesCapability.get(entity);

            if(capability.isPresent()){
                for (var containerEntry : capability.get().getContainers().entrySet()) {
                    for (var accessoryEntry : containerEntry.getValue().getAccessories()) {
                        var reference = new SlotReference(containerEntry.getKey(), entity, accessoryEntry.getFirst());
                        var stack = accessoryEntry.getSecond();

                        var accessory = AccessoriesAPI.getAccessory(stack);

                        if(accessory.isPresent() && accessory.get() instanceof LootingAdjustment lootingAdjustment){
                            currentLevel += lootingAdjustment.getLootingAdjustment(stack, reference, targetEntity, damageSource, currentLevel);
                        }

                        for (var entry : AccessoryNest.tryAndGet(stack).entrySet()) {
                            if((entry.getValue() instanceof LootingAdjustment lootingAdjustment)) {
                                currentLevel += lootingAdjustment.getLootingAdjustment(entry.getKey(), reference, targetEntity, damageSource, currentLevel);
                            }

                            currentLevel += LOOTING_ADJUSTMENT_EVENT.invoker().getLootingAdjustment(stack, reference, targetEntity, damageSource, currentLevel);
                        }

                        currentLevel += LOOTING_ADJUSTMENT_EVENT.invoker().getLootingAdjustment(stack, reference, targetEntity, damageSource, currentLevel);
                    }
                }
            }
        }

        return currentLevel;
    }

    public static final Event<LootingAdjustment> LOOTING_ADJUSTMENT_EVENT = EventFactory.createArrayBacked(LootingAdjustment.class, invokers -> (stack, reference, target, damageSource, currentLevel) -> {
        for (var invoker : invokers) {
            currentLevel += invoker.getLootingAdjustment(stack, reference, target, damageSource, currentLevel);
        }

        return currentLevel;
    });

    public interface LootingAdjustment {
        int getLootingAdjustment(ItemStack stack, SlotReference reference, LivingEntity target, DamageSource damageSource, int currentLevel);
    }

    //--

    public static int fortuneAdjustment(LootContext context, int currentLevel){
        var entity = context.getParamOrNull(LootContextParams.THIS_ENTITY);

        if(!(entity instanceof LivingEntity livingEntity)) return currentLevel;

        var capability = AccessoriesCapability.get(livingEntity);

        if(capability.isPresent()){
            for (var containerEntry : capability.get().getContainers().entrySet()) {
                for (var accessoryEntry : containerEntry.getValue().getAccessories()) {
                    var reference = new SlotReference(containerEntry.getKey(), livingEntity, accessoryEntry.getFirst());
                    var stack = accessoryEntry.getSecond();

                    var accessory = AccessoriesAPI.getAccessory(stack);

                    if(accessory.isPresent() && accessory.get() instanceof FortuneAdjustment fortuneAdjustment){
                        currentLevel += fortuneAdjustment.getFortuneAdjustment(stack, reference, context, currentLevel);
                    }

                    for (var entry : AccessoryNest.tryAndGet(stack).entrySet()) {
                        var innerStack = entry.getKey();

                        if((entry.getValue() instanceof FortuneAdjustment fortuneAdjustment)) {
                            currentLevel += fortuneAdjustment.getFortuneAdjustment(innerStack, reference, context, currentLevel);
                        }

                        currentLevel += FORTUNE_ADJUSTMENT_EVENT.invoker().getFortuneAdjustment(innerStack, reference, context, currentLevel);
                    }

                    currentLevel += FORTUNE_ADJUSTMENT_EVENT.invoker().getFortuneAdjustment(stack, reference, context, currentLevel);
                }
            }
        }

        return currentLevel;
    }

    public static final Event<FortuneAdjustment> FORTUNE_ADJUSTMENT_EVENT = EventFactory.createArrayBacked(FortuneAdjustment.class, invokers -> (stack, reference, context, currentLevel) -> {
        for (var invoker : invokers) {
            currentLevel += invoker.getFortuneAdjustment(stack, reference, context, currentLevel);
        }

        return currentLevel;
    });

    public interface FortuneAdjustment {
        int getFortuneAdjustment(ItemStack stack, SlotReference reference, LootContext context, int currentLevel);
    }

    //--

    public static TriState isPiglinsNeutral(LivingEntity entity){
        var state = TriState.DEFAULT;

        var capability = AccessoriesCapability.get(entity);

        if(capability.isPresent()){
            for (var containerEntry : capability.get().getContainers().entrySet()) {
                for (var accessoryEntry : containerEntry.getValue().getAccessories()) {
                    var reference = new SlotReference(containerEntry.getKey(), entity, accessoryEntry.getFirst());
                    var stack = accessoryEntry.getSecond();

                    var accessory = AccessoriesAPI.getAccessory(stack);

                    if(accessory.isPresent() && accessory.get() instanceof PiglinNeutralInducer inducer){
                        state = inducer.makesPiglinsNeutral(stack, reference);

                        if(state != TriState.DEFAULT) return state;
                    }

                    state = PIGLIN_NEUTRAL_INDUCER_EVENT.invoker().makesPiglinsNeutral(stack, reference);

                    if(state != TriState.DEFAULT) return state;

                    for (var entry : AccessoryNest.tryAndGet(stack).entrySet()) {
                        var innerStack = entry.getKey();

                        if(entry.getValue() instanceof PiglinNeutralInducer inducer) {
                            state = inducer.makesPiglinsNeutral(innerStack, reference);

                            if(state != TriState.DEFAULT) return state;
                        }

                        state = PIGLIN_NEUTRAL_INDUCER_EVENT.invoker().makesPiglinsNeutral(innerStack, reference);
                    }
                }
            }
        }

        return state;
    }

    public static final Event<PiglinNeutralInducer> PIGLIN_NEUTRAL_INDUCER_EVENT = EventFactory.createArrayBacked(PiglinNeutralInducer.class, invokers -> (stack, reference) -> {
        for (var invoker : invokers) {
            var state = invoker.makesPiglinsNeutral(stack, reference);

            if(state != TriState.DEFAULT) return state;
        }

        return TriState.DEFAULT;
    });

    public interface PiglinNeutralInducer {
        TriState makesPiglinsNeutral(ItemStack stack, SlotReference reference);
    }

    //--

    public static TriState allowWalkingOnSnow(LivingEntity entity){
        var state = TriState.DEFAULT;

        var capability = AccessoriesCapability.get(entity);

        if(capability.isPresent()){
            for (var containerEntry : capability.get().getContainers().entrySet()) {
                for (var accessoryEntry : containerEntry.getValue().getAccessories()) {
                    var reference = new SlotReference(containerEntry.getKey(), entity, accessoryEntry.getFirst());
                    var stack = accessoryEntry.getSecond();

                    var accessory = AccessoriesAPI.getAccessory(stack);

                    if(accessory.isPresent() && accessory.get() instanceof AllowWalingOnSnow event){
                        state = event.allowWalkingOnSnow(stack, reference);

                        if(state != TriState.DEFAULT) return state;
                    }

                    state = ALLOW_WALING_ON_SNOW_EVENT.invoker().allowWalkingOnSnow(stack, reference);

                    if(state != TriState.DEFAULT) return state;

                    for (var entry : AccessoryNest.tryAndGet(stack).entrySet()) {
                        var innerStack = entry.getKey();

                        if(entry.getValue() instanceof AllowWalingOnSnow event) {
                            state = event.allowWalkingOnSnow(innerStack, reference);

                            if (state != TriState.DEFAULT) return state;
                        }

                        state = ALLOW_WALING_ON_SNOW_EVENT.invoker().allowWalkingOnSnow(innerStack, reference);
                    }
                }
            }
        }

        return state;
    }

    public static final Event<AllowWalingOnSnow> ALLOW_WALING_ON_SNOW_EVENT = EventFactory.createArrayBacked(AllowWalingOnSnow.class, invokers -> (stack, reference) -> {
        for (var invoker : invokers) {
            var state = invoker.allowWalkingOnSnow(stack, reference);

            if(state != TriState.DEFAULT) return state;
        }

        return TriState.DEFAULT;
    });

    public interface AllowWalingOnSnow {
        TriState allowWalkingOnSnow(ItemStack stack, SlotReference reference);
    }

    //--

    //TODO: Replace with cache?
    private static final Map<Integer, Map<Integer, TriState>> endermanAngyCacheResults = new HashMap<>();

    public static TriState isEndermanMask(LivingEntity entity, EnderMan enderMan){
        var state = TriState.DEFAULT;

        if(endermanAngyCacheResults.containsKey(entity.getId())){
            var enderManResultCache = endermanAngyCacheResults.get(entity.getId());

            if(enderManResultCache.containsKey(enderMan.getId())){
                return enderManResultCache.get(enderMan.getId());
            }
        }

        var capability = AccessoriesCapability.get(entity);

        if(capability.isPresent()){
            for (var containerEntry : capability.get().getContainers().entrySet()) {
                for (var accessoryEntry : containerEntry.getValue().getAccessories()) {
                    var reference = new SlotReference(containerEntry.getKey(), entity, accessoryEntry.getFirst());
                    var stack = accessoryEntry.getSecond();

                    var accessory = AccessoriesAPI.getAccessory(stack);

                    if(accessory.isPresent() && accessory.get() instanceof EndermanMasked masked){
                        state = masked.isEndermanMasked(enderMan, stack, reference);

                        if(state != TriState.DEFAULT) return state;
                    }

                    state = ENDERMAN_MASKED_EVENT.invoker().isEndermanMasked(enderMan, stack, reference);

                    if(state != TriState.DEFAULT) return state;

                    for (var entry : AccessoryNest.tryAndGet(stack).entrySet()) {
                        var innerStack = entry.getKey();

                        if(entry.getValue() instanceof EndermanMasked masked) {
                            state = masked.isEndermanMasked(enderMan, innerStack, reference);

                            if (state != TriState.DEFAULT) return state;
                        }

                        state = ENDERMAN_MASKED_EVENT.invoker().isEndermanMasked(enderMan, innerStack, reference);
                    }
                }
            }
        }

        endermanAngyCacheResults.computeIfAbsent(entity.getId(), integer -> new HashMap<>())
                .put(enderMan.getId(), state);

        return state;
    }

    public static void clearEndermanAngryCache(){
        for (var map : endermanAngyCacheResults.values()) map.clear();
    }

    public static final Event<EndermanMasked> ENDERMAN_MASKED_EVENT = EventFactory.createArrayBacked(EndermanMasked.class, invokers -> (enderMan, stack, reference) -> {
        for (var invoker : invokers) {
            var state = invoker.isEndermanMasked(enderMan, stack, reference);

            if(state != TriState.DEFAULT) return state;
        }

        return TriState.DEFAULT;
    });

    public interface EndermanMasked {
        TriState isEndermanMasked(EnderMan enderMan, ItemStack stack, SlotReference reference);
    }

    public static final Event<WindowResizeCallback> WINDOW_RESIZE_CALLBACK_EVENT = EventFactory.createArrayBacked(WindowResizeCallback.class, callbacks -> (client, window) -> {
        for (var callback : callbacks) {
            callback.onResized(client, window);
        }
    });

    public interface WindowResizeCallback {

        /**
         * Called after the client's window has been resized
         *
         * @param client The currently active client
         * @param window The window which was resized
         */
        void onResized(Minecraft client, Window window);

    }
}