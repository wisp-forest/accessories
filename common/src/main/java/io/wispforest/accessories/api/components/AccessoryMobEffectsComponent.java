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
            MAP_ENDEC.fieldOf("delayed_instances", AccessoryMobEffectsComponent::mobEffects),
            AccessoryMobEffectsComponent::new
    );

    private final Map<Integer, List<MobEffectInstance>> mobEffects;

    private final Map<Integer, Long> delayToTimer = new HashMap<>();

    public AccessoryMobEffectsComponent(Map<Integer, List<MobEffectInstance>> mobEffects) {
        this.mobEffects = mobEffects;
    }

    public Map<Integer, List<MobEffectInstance>> mobEffects() {
        return mobEffects;
    }

    public void handleReapplyingEffects(LivingEntity livingEntity, long time) {
        for (var i : mobEffects.keySet()) {
            var lastApply = delayToTimer.getOrDefault(i, null);

            if ((lastApply == null) || time - lastApply > i) {
                for (var mobEffectInstance : mobEffects.get(i)) {
                    livingEntity.addEffect(mobEffectInstance);
                }
            }

            delayToTimer.put(i, time);
        }
    }

    public void handleRemovingEffects(LivingEntity livingEntity) {
        for (List<MobEffectInstance> value : mobEffects.values()) {
            for (MobEffectInstance mobEffectInstance : value) {
                livingEntity.removeEffect(mobEffectInstance.getEffect());
            }
        }

        delayToTimer.clear();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (AccessoryMobEffectsComponent) obj;
        return Objects.equals(this.mobEffects, that.mobEffects);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mobEffects);
    }

    @Override
    public String toString() {
        return "AccessoryMobEffectsComponent[" +
                "mobEffects=" + mobEffects + ']';
    }
}
