package io.wispforest.accessories.api.components;

import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.serialization.CodecUtils;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.ApiStatus;

import java.util.*;
import java.util.stream.Collectors;

@ApiStatus.Experimental
public final class AccessoryMobEffectsComponent {
    public static final AccessoryMobEffectsComponent EMPTY = new AccessoryMobEffectsComponent(new ArrayList<>(), new HashMap<>());

    private static final Endec<List<MobEffectInstance>> MOB_EFFECT_INSTANCES = CodecUtils.toEndecWithRegistries(MobEffectInstance.CODEC, MobEffectInstance.STREAM_CODEC).listOf();
    private static final Endec<Map<Integer, List<MobEffectInstance>>> MAP_ENDEC = StructEndecBuilder.of(
            Endec.INT.fieldOf("delay", Map.Entry::getKey),
            MOB_EFFECT_INSTANCES.fieldOf("effect_instances", Map.Entry::getValue),
            Map::entry
    ).listOf()
            .xmap(entries -> {
                return entries.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (instances1, instances2) -> {
                    var list = new ArrayList<>(instances1);

                    list.addAll(instances2);

                    return list;
                }, LinkedHashMap::new));
            }, kvMap -> List.copyOf(kvMap.entrySet()));

    public static Endec<AccessoryMobEffectsComponent> ENDEC = StructEndecBuilder.of(
            MOB_EFFECT_INSTANCES.fieldOf("constant_effects", AccessoryMobEffectsComponent::constantMobEffects),
            MAP_ENDEC.fieldOf("delayed_effects", AccessoryMobEffectsComponent::delayedMobEffects),
            AccessoryMobEffectsComponent::new
    );

    private final List<MobEffectInstance> constantMobEffects;
    private final Map<Integer, List<MobEffectInstance>> delayedMobEffects;

    private final Map<Integer, Long> delayToTimer = new HashMap<>();

    public AccessoryMobEffectsComponent(List<MobEffectInstance> constantMobEffects, Map<Integer, List<MobEffectInstance>> mobEffects) {
        this.constantMobEffects = constantMobEffects;
        this.delayedMobEffects = mobEffects;
    }

    public List<MobEffectInstance> constantMobEffects() {
        return Collections.unmodifiableList(this.constantMobEffects);
    }

    public Map<Integer, List<MobEffectInstance>> delayedMobEffects() {
        return Collections.unmodifiableMap(this.delayedMobEffects);
    }

    public AccessoryMobEffectsComponent addEffect(MobEffectInstance instance) {
        var effects = new ArrayList<>(this.constantMobEffects);

        effects.add(instance);

        return new AccessoryMobEffectsComponent(effects, this.delayedMobEffects);
    }

    public AccessoryMobEffectsComponent addEffect(MobEffectInstance instance, int applyDelay) {
        var map = new HashMap<>(this.delayedMobEffects);

        map.computeIfAbsent(applyDelay, integer -> new ArrayList<>())
                .add(instance);

        return new AccessoryMobEffectsComponent(this.constantMobEffects, map);
    }

    public void handleApplyingConstantEffects(LivingEntity livingEntity) {
        for (MobEffectInstance constantMobEffect : this.constantMobEffects) {
            livingEntity.addEffect(constantMobEffect);
        }
    }

    public void handleReapplyingEffects(LivingEntity livingEntity, long time) {
        for (var i : delayedMobEffects.keySet()) {
            var lastApply = delayToTimer.getOrDefault(i, null);

            if ((lastApply == null) || time - lastApply > i) {
                for (var mobEffectInstance : delayedMobEffects.get(i)) {
                    livingEntity.addEffect(mobEffectInstance);
                }
            }

            delayToTimer.put(i, time);
        }
    }

    public void handleRemovingEffects(LivingEntity livingEntity) {
        for (List<MobEffectInstance> value : delayedMobEffects.values()) {
            for (MobEffectInstance mobEffectInstance : value) {
                livingEntity.removeEffect(mobEffectInstance.getEffect());
            }
        }

        for (MobEffectInstance mobEffectInstance : this.constantMobEffects) {
            livingEntity.removeEffect(mobEffectInstance.getEffect());
        }

        delayToTimer.clear();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (AccessoryMobEffectsComponent) obj;
        return Objects.equals(this.delayedMobEffects, that.delayedMobEffects);
    }

    @Override
    public int hashCode() {
        return Objects.hash(delayedMobEffects);
    }

    @Override
    public String toString() {
        return "AccessoryMobEffectsComponent[" +
                "mobEffects=" + delayedMobEffects + ']';
    }
}
